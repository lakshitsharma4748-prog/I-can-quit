package com.safeshield.app.vpn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetAddress

class PacketProcessorTest {

    private val filter = DomainFilter(setOf("blocked-example.test"))
    private val processor = PacketProcessor(filter)

    /** Builds a raw IPv4/UDP packet carrying a DNS A-record query for [domain], from a fake client to the fake VPN DNS address. */
    private fun buildDnsQueryPacket(domain: String, sourcePort: Int = 54321): ByteArray {
        val question = mutableListOf<Byte>()
        for (label in domain.split(".")) {
            question.add(label.length.toByte())
            question.addAll(label.toByteArray(Charsets.US_ASCII).toList())
        }
        question.add(0)
        question.addAll(listOf<Byte>(0x00, 0x01, 0x00, 0x01)) // QTYPE=A, QCLASS=IN

        val dnsHeader = ByteArray(12)
        dnsHeader[1] = 0x42 // transaction id low byte
        dnsHeader[2] = 0x01 // RD
        dnsHeader[5] = 0x01 // QDCOUNT=1
        val dnsMessage = dnsHeader + question.toByteArray()

        val udpLength = 8 + dnsMessage.size
        val udpHeader = ByteArray(8)
        writeUInt16(udpHeader, 0, sourcePort)
        writeUInt16(udpHeader, 2, 53)
        writeUInt16(udpHeader, 4, udpLength)
        // Checksum intentionally left 0 (disabled) — valid for IPv4 UDP and irrelevant to inbound parsing, which doesn't validate it.

        val totalLength = 20 + udpLength
        val ipHeader = ByteArray(20)
        ipHeader[0] = 0x45
        writeUInt16(ipHeader, 2, totalLength)
        ipHeader[8] = 64
        ipHeader[9] = 17 // UDP
        System.arraycopy(InetAddress.getByName("10.0.0.2").address, 0, ipHeader, 12, 4)
        System.arraycopy(InetAddress.getByName("192.0.2.1").address, 0, ipHeader, 16, 4)

        return ipHeader + udpHeader + dnsMessage
    }

    private fun writeUInt16(data: ByteArray, offset: Int, value: Int) {
        data[offset] = (value shr 8).toByte()
        data[offset + 1] = value.toByte()
    }

    @Test
    fun `blocked domain yields a Block decision with a well-formed reply packet`() {
        val packet = buildDnsQueryPacket("blocked-example.test")
        val decision = processor.process(packet, packet.size)

        assertTrue(decision is PacketProcessor.Decision.Block)
        val reply = (decision as PacketProcessor.Decision.Block).packet

        // IPv4 header checksum self-verifies to zero.
        assertEquals(0, Checksums.compute(reply.copyOfRange(0, 20)))
        // Source/destination were swapped: reply comes "from" 192.0.2.1.
        assertEquals("192.0.2.1", InetAddress.getByAddress(reply.copyOfRange(12, 16)).hostAddress)
        assertEquals("10.0.0.2", InetAddress.getByAddress(reply.copyOfRange(16, 20)).hostAddress)
    }

    @Test
    fun `allowed domain yields a Forward decision`() {
        val packet = buildDnsQueryPacket("safe.example")
        val decision = processor.process(packet, packet.size)
        assertTrue(decision is PacketProcessor.Decision.Forward)
    }

    @Test
    fun `non-UDP packet is dropped`() {
        val packet = buildDnsQueryPacket("safe.example")
        packet[9] = 6 // TCP instead of UDP
        val decision = processor.process(packet, packet.size)
        assertEquals(PacketProcessor.Decision.Drop, decision)
    }

    @Test
    fun `traffic to a non-DNS port is dropped`() {
        val packet = buildDnsQueryPacket("safe.example")
        writeUInt16(packet, 22, 8080) // overwrite destination port (offset 20 + 2)
        val decision = processor.process(packet, packet.size)
        assertEquals(PacketProcessor.Decision.Drop, decision)
    }
}
