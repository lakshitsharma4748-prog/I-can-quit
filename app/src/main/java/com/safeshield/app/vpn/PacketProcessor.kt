package com.safeshield.app.vpn

import com.safeshield.app.vpn.dns.DnsMessage
import java.net.InetAddress

/**
 * Parses raw IP/UDP packets read from the TUN interface, decides
 * BLOCK/ALLOW for DNS queries via [DomainFilter], and builds reply packets.
 * This class performs no I/O itself (no socket, no TUN access) so it is
 * fully unit-testable on the JVM — [SafeShieldVpnService] is the only thing
 * that touches file descriptors or sockets.
 *
 * Scope: UDP port 53 DNS over IPv4 or IPv6 (Phase 9 added IPv6 alongside
 * Phase 2's original IPv4-only MVP). The VPN's routes only ever send DNS
 * traffic (destined for one of the virtual DNS addresses) through the TUN,
 * so anything else reaching here is unexpected and is dropped rather than
 * guessed at.
 *
 * Explicitly NOT covered, and not achievable without a fundamentally
 * different (full-tunnel) VPN architecture — see BYPASS_TESTING.md:
 * DNS-over-TLS/Private DNS (port 853) and DNS-over-HTTPS both go directly
 * to their configured resolver's real IP over an encrypted connection that
 * never touches this VPN's narrow DNS-only routes, so they bypass this
 * filter entirely today.
 */
class PacketProcessor(private val domainFilter: DomainFilter) {

    sealed class Decision {
        /** Write [packet] straight back into the TUN — a synthesized NXDOMAIN reply. */
        data class Block(val packet: ByteArray) : Decision()

        /**
         * Forward [dnsQuery] upstream (the caller decides to where — always
         * the configured real resolver, never [ParsedIpHeader.destinationAddress],
         * which is only ever one of this VPN's own virtual DNS addresses).
         * Whatever comes back from upstream should be passed to [wrapReply]
         * to get the packet to write back into the TUN.
         */
        data class Forward(
            val dnsQuery: ByteArray,
            val wrapReply: (ByteArray) -> ByteArray
        ) : Decision()

        /** Not a DNS query this class understands — caller should drop the packet. */
        object Drop : Decision()
    }

    fun process(packet: ByteArray, length: Int): Decision {
        val ip = parseIpHeader(packet, length) ?: return Decision.Drop
        if (ip.protocol != PROTOCOL_UDP) return Decision.Drop

        val udp = parseUdpHeader(packet, ip.headerLength, length) ?: return Decision.Drop
        if (udp.destinationPort != DNS_PORT) return Decision.Drop

        val payloadStart = ip.headerLength + UDP_HEADER_LENGTH
        if (payloadStart > length) return Decision.Drop
        val dnsQuery = packet.copyOfRange(payloadStart, length)

        val parsed = DnsMessage.parseQuestionName(dnsQuery) ?: return Decision.Drop

        return if (domainFilter.isBlocked(parsed.questionName)) {
            val response = DnsMessage.buildNxDomainResponse(parsed)
            Decision.Block(buildReplyPacket(ip, udp, response))
        } else {
            Decision.Forward(dnsQuery = dnsQuery) { response -> buildReplyPacket(ip, udp, response) }
        }
    }

    // ---- IP (v4 or v6) ------------------------------------------------------

    internal data class ParsedIpHeader(
        val headerLength: Int,
        val protocol: Int,
        val isIpv6: Boolean,
        val sourceAddress: InetAddress,
        val destinationAddress: InetAddress
    )

    internal fun parseIpHeader(packet: ByteArray, length: Int): ParsedIpHeader? {
        if (length < 1) return null
        return when ((packet[0].toInt() and 0xFF) shr 4) {
            4 -> parseIpv4Header(packet, length)
            6 -> parseIpv6Header(packet, length)
            else -> null
        }
    }

    private fun parseIpv4Header(packet: ByteArray, length: Int): ParsedIpHeader? {
        if (length < 20) return null
        val headerLength = (packet[0].toInt() and 0x0F) * 4
        if (headerLength < 20 || headerLength > length) return null

        val protocol = packet[9].toInt() and 0xFF
        val source = InetAddress.getByAddress(packet.copyOfRange(12, 16))
        val destination = InetAddress.getByAddress(packet.copyOfRange(16, 20))

        return ParsedIpHeader(headerLength, protocol, isIpv6 = false, source, destination)
    }

    /**
     * IPv6's fixed header is always exactly 40 bytes. Extension headers
     * (routing, fragment, etc.) are uncommon for a plain outgoing UDP DNS
     * query and are not walked here — a packet using one is dropped rather
     * than mis-parsed as if "next header" were the final protocol.
     */
    private fun parseIpv6Header(packet: ByteArray, length: Int): ParsedIpHeader? {
        if (length < 40) return null
        val nextHeader = packet[6].toInt() and 0xFF
        val source = InetAddress.getByAddress(packet.copyOfRange(8, 24))
        val destination = InetAddress.getByAddress(packet.copyOfRange(24, 40))
        return ParsedIpHeader(headerLength = 40, protocol = nextHeader, isIpv6 = true, source, destination)
    }

    // ---- UDP ----------------------------------------------------------------

    internal data class UdpHeader(val sourcePort: Int, val destinationPort: Int)

    internal fun parseUdpHeader(packet: ByteArray, ipHeaderLength: Int, length: Int): UdpHeader? {
        if (ipHeaderLength + UDP_HEADER_LENGTH > length) return null
        val sourcePort = readUInt16(packet, ipHeaderLength)
        val destinationPort = readUInt16(packet, ipHeaderLength + 2)
        return UdpHeader(sourcePort, destinationPort)
    }

    // ---- Reply packet construction -----------------------------------------

    /** Builds a full IP/UDP reply packet carrying [payload], dispatching to the IPv4 or IPv6 builder based on the original packet's version. */
    private fun buildReplyPacket(ip: ParsedIpHeader, udp: UdpHeader, payload: ByteArray): ByteArray =
        if (ip.isIpv6) buildIpv6ReplyPacket(ip, udp, payload) else buildIpv4ReplyPacket(ip, udp, payload)

    private fun buildIpv4ReplyPacket(ip: ParsedIpHeader, udp: UdpHeader, payload: ByteArray): ByteArray {
        val udpLength = UDP_HEADER_LENGTH + payload.size
        val totalLength = 20 + udpLength

        val ipHeader = ByteArray(20)
        ipHeader[0] = 0x45 // version 4, IHL 5 (20 bytes, no options)
        ipHeader[1] = 0x00 // DSCP/ECN
        writeUInt16(ipHeader, 2, totalLength)
        writeUInt16(ipHeader, 4, 0) // identification
        writeUInt16(ipHeader, 6, 0x4000) // flags: don't fragment
        ipHeader[8] = 64 // TTL
        ipHeader[9] = PROTOCOL_UDP.toByte()
        writeUInt16(ipHeader, 10, 0) // checksum placeholder
        System.arraycopy(ip.destinationAddress.address, 0, ipHeader, 12, 4) // reply source = original destination
        System.arraycopy(ip.sourceAddress.address, 0, ipHeader, 16, 4) // reply destination = original source
        val ipChecksum = Checksums.compute(ipHeader)
        writeUInt16(ipHeader, 10, ipChecksum)

        val udpHeader = buildUdpHeader(ip, udp, payload, udpLength, pseudoHeaderSize = 12) { pseudo ->
            System.arraycopy(ip.destinationAddress.address, 0, pseudo, 0, 4)
            System.arraycopy(ip.sourceAddress.address, 0, pseudo, 4, 4)
            pseudo[8] = 0
            pseudo[9] = PROTOCOL_UDP.toByte()
            writeUInt16(pseudo, 10, udpLength)
        }

        return ipHeader + udpHeader + payload
    }

    private fun buildIpv6ReplyPacket(ip: ParsedIpHeader, udp: UdpHeader, payload: ByteArray): ByteArray {
        val udpLength = UDP_HEADER_LENGTH + payload.size

        val ipHeader = ByteArray(40)
        ipHeader[0] = 0x60 // version 6; traffic class/flow label left at 0
        writeUInt16(ipHeader, 4, udpLength) // payload length (excludes this 40-byte header)
        ipHeader[6] = PROTOCOL_UDP.toByte() // next header
        ipHeader[7] = 64 // hop limit
        System.arraycopy(ip.destinationAddress.address, 0, ipHeader, 8, 16) // reply source = original destination
        System.arraycopy(ip.sourceAddress.address, 0, ipHeader, 24, 16) // reply destination = original source
        // IPv6 has no header checksum field at all (RFC 8200) — unlike IPv4, there's nothing to compute here.

        // UDP checksum is mandatory over IPv6 (RFC 8200 §8.1), unlike IPv4
        // where 0 means "no checksum" — buildUdpHeader's zero->0xFFFF
        // substitution (for the vanishingly unlikely computed-zero case)
        // keeps the field non-zero either way, so the same helper is safe
        // to reuse for both.
        val udpHeader = buildUdpHeader(ip, udp, payload, udpLength, pseudoHeaderSize = 40) { pseudo ->
            System.arraycopy(ip.destinationAddress.address, 0, pseudo, 0, 16)
            System.arraycopy(ip.sourceAddress.address, 0, pseudo, 16, 16)
            writeUInt32(pseudo, 32, udpLength)
            pseudo[39] = PROTOCOL_UDP.toByte()
        }

        return ipHeader + udpHeader + payload
    }

    /** Shared UDP header + checksum construction; [fillPseudoHeader] writes the IP-version-specific pseudo-header fields into an appropriately sized, zeroed buffer. */
    private fun buildUdpHeader(
        ip: ParsedIpHeader,
        udp: UdpHeader,
        payload: ByteArray,
        udpLength: Int,
        pseudoHeaderSize: Int,
        fillPseudoHeader: (ByteArray) -> Unit
    ): ByteArray {
        val udpHeader = ByteArray(UDP_HEADER_LENGTH)
        writeUInt16(udpHeader, 0, udp.destinationPort) // reply source port = original destination port (53)
        writeUInt16(udpHeader, 2, udp.sourcePort) // reply destination port = original source port
        writeUInt16(udpHeader, 4, udpLength)
        writeUInt16(udpHeader, 6, 0) // checksum placeholder

        val pseudoHeader = ByteArray(pseudoHeaderSize)
        fillPseudoHeader(pseudoHeader)
        val checksumRaw = Checksums.compute(pseudoHeader, udpHeader, payload)
        // Per RFC 768, a computed checksum of zero is transmitted as all-ones;
        // an all-zero field would instead mean "no checksum" (IPv4 only —
        // moot for IPv6, where the field must be non-zero regardless).
        val checksum = if (checksumRaw == 0) 0xFFFF else checksumRaw
        writeUInt16(udpHeader, 6, checksum)

        return udpHeader
    }

    private fun readUInt16(data: ByteArray, offset: Int): Int =
        ((data[offset].toInt() and 0xFF) shl 8) or (data[offset + 1].toInt() and 0xFF)

    private fun writeUInt16(data: ByteArray, offset: Int, value: Int) {
        data[offset] = (value shr 8).toByte()
        data[offset + 1] = value.toByte()
    }

    private fun writeUInt32(data: ByteArray, offset: Int, value: Int) {
        data[offset] = (value shr 24).toByte()
        data[offset + 1] = (value shr 16).toByte()
        data[offset + 2] = (value shr 8).toByte()
        data[offset + 3] = value.toByte()
    }

    companion object {
        const val PROTOCOL_UDP = 17
        const val UDP_HEADER_LENGTH = 8
        const val DNS_PORT = 53
    }
}
