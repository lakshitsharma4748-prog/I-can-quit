package com.safeshield.app.vpn

import com.safeshield.app.vpn.dns.DnsMessage
import java.net.Inet4Address

/**
 * Parses raw IPv4/UDP packets read from the TUN interface, decides
 * BLOCK/ALLOW for DNS queries via [DomainFilter], and builds reply packets.
 * This class performs no I/O itself (no socket, no TUN access) so it is
 * fully unit-testable on the JVM — [SafeShieldVpnService] is the only thing
 * that touches file descriptors or sockets.
 *
 * Scope (Phase 2 MVP): IPv4 + UDP port 53 only. The VPN's routes only ever
 * send DNS traffic (destined for the virtual DNS address) through the TUN,
 * so anything else reaching here is unexpected and is dropped rather than
 * guessed at.
 */
class PacketProcessor(private val domainFilter: DomainFilter) {

    sealed class Decision {
        /** Write [packet] straight back into the TUN — a synthesized NXDOMAIN reply. */
        data class Block(val packet: ByteArray) : Decision()

        /**
         * Forward [dnsQuery] upstream (the caller decides to where — always
         * the configured real resolver, never [Ipv4Header.destinationAddress],
         * which is only ever this VPN's own virtual DNS address). Whatever
         * comes back from upstream should be passed to [wrapReply] to get
         * the packet to write back into the TUN.
         */
        data class Forward(
            val dnsQuery: ByteArray,
            val wrapReply: (ByteArray) -> ByteArray
        ) : Decision()

        /** Not a DNS query this class understands — caller should drop the packet. */
        object Drop : Decision()
    }

    fun process(packet: ByteArray, length: Int): Decision {
        val ip = parseIpv4Header(packet, length) ?: return Decision.Drop
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

    // ---- IPv4 -------------------------------------------------------------

    internal data class Ipv4Header(
        val headerLength: Int,
        val protocol: Int,
        val sourceAddress: Inet4Address,
        val destinationAddress: Inet4Address
    )

    internal fun parseIpv4Header(packet: ByteArray, length: Int): Ipv4Header? {
        if (length < 20) return null
        val versionAndIhl = packet[0].toInt() and 0xFF
        val version = versionAndIhl shr 4
        if (version != 4) return null // IPv6 is out of scope for this MVP (documented limitation)
        val headerLength = (versionAndIhl and 0x0F) * 4
        if (headerLength < 20 || headerLength > length) return null

        val protocol = packet[9].toInt() and 0xFF
        val source = Inet4Address.getByAddress(packet.copyOfRange(12, 16)) as Inet4Address
        val destination = Inet4Address.getByAddress(packet.copyOfRange(16, 20)) as Inet4Address

        return Ipv4Header(headerLength, protocol, source, destination)
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

    /**
     * Builds a full IPv4/UDP packet carrying [payload], addressed from the
     * original destination back to the original source (i.e. a reply),
     * with correctly computed IPv4 and UDP checksums.
     */
    private fun buildReplyPacket(ip: Ipv4Header, udp: UdpHeader, payload: ByteArray): ByteArray {
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

        val udpHeader = ByteArray(UDP_HEADER_LENGTH)
        writeUInt16(udpHeader, 0, udp.destinationPort) // reply source port = original destination port (53)
        writeUInt16(udpHeader, 2, udp.sourcePort) // reply destination port = original source port
        writeUInt16(udpHeader, 4, udpLength)
        writeUInt16(udpHeader, 6, 0) // checksum placeholder

        val pseudoHeader = ByteArray(12)
        System.arraycopy(ip.destinationAddress.address, 0, pseudoHeader, 0, 4)
        System.arraycopy(ip.sourceAddress.address, 0, pseudoHeader, 4, 4)
        pseudoHeader[8] = 0
        pseudoHeader[9] = PROTOCOL_UDP.toByte()
        writeUInt16(pseudoHeader, 10, udpLength)
        val udpChecksumRaw = Checksums.compute(pseudoHeader, udpHeader, payload)
        // Per RFC 768, a computed checksum of zero is transmitted as all-ones;
        // an all-zero field would instead mean "no checksum".
        val udpChecksum = if (udpChecksumRaw == 0) 0xFFFF else udpChecksumRaw
        writeUInt16(udpHeader, 6, udpChecksum)

        return ipHeader + udpHeader + payload
    }

    private fun readUInt16(data: ByteArray, offset: Int): Int =
        ((data[offset].toInt() and 0xFF) shl 8) or (data[offset + 1].toInt() and 0xFF)

    private fun writeUInt16(data: ByteArray, offset: Int, value: Int) {
        data[offset] = (value shr 8).toByte()
        data[offset + 1] = value.toByte()
    }

    companion object {
        const val PROTOCOL_UDP = 17
        const val UDP_HEADER_LENGTH = 8
        const val DNS_PORT = 53
    }
}
