package com.safeshield.app.vpn.dns

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DnsMessageTest {

    /** Builds a minimal, well-formed DNS query for [name] with the given transaction id, requesting an A record. */
    private fun buildQuery(name: String, transactionId: Int = 0x1234): ByteArray {
        val header = ByteArray(12)
        header[0] = (transactionId shr 8).toByte()
        header[1] = transactionId.toByte()
        header[2] = 0x01 // RD=1
        header[5] = 0x01 // QDCOUNT = 1

        val question = mutableListOf<Byte>()
        for (label in name.split(".")) {
            question.add(label.length.toByte())
            question.addAll(label.toByteArray(Charsets.US_ASCII).toList())
        }
        question.add(0) // root label
        question.addAll(listOf(0x00, 0x01).map { it.toByte() }) // QTYPE = A
        question.addAll(listOf(0x00, 0x01).map { it.toByte() }) // QCLASS = IN

        return header + question.toByteArray()
    }

    @Test
    fun `parses a simple query`() {
        val query = buildQuery("blocked-example.test", transactionId = 0xABCD)
        val parsed = DnsMessage.parseQuestionName(query)

        assertEquals(0xABCD, parsed?.transactionId)
        assertEquals("blocked-example.test", parsed?.questionName)
    }

    @Test
    fun `parses a multi-label query correctly`() {
        val parsed = DnsMessage.parseQuestionName(buildQuery("sub.blocked.example"))
        assertEquals("sub.blocked.example", parsed?.questionName)
    }

    @Test
    fun `too-short message is rejected`() {
        assertNull(DnsMessage.parseQuestionName(ByteArray(4)))
    }

    @Test
    fun `message with zero questions is rejected`() {
        val header = ByteArray(12) // QDCOUNT defaults to 0
        assertNull(DnsMessage.parseQuestionName(header))
    }

    @Test
    fun `compressed name pointer in a query is rejected rather than mis-parsed`() {
        val header = ByteArray(12)
        header[5] = 0x01 // QDCOUNT = 1
        val withPointer = header + byteArrayOf(0xC0.toByte(), 0x0C) // a compression pointer, not a label
        assertNull(DnsMessage.parseQuestionName(withPointer))
    }

    @Test
    fun `NXDOMAIN response echoes the transaction id and question, and sets QR plus RCODE 3`() {
        val query = buildQuery("adult-example.test", transactionId = 0x5566)
        val parsed = DnsMessage.parseQuestionName(query)!!
        val response = DnsMessage.buildNxDomainResponse(parsed)

        val responseTransactionId = ((response[0].toInt() and 0xFF) shl 8) or (response[1].toInt() and 0xFF)
        assertEquals(0x5566, responseTransactionId)

        val qrBit = (response[2].toInt() and 0x80) != 0
        assertEquals(true, qrBit)

        val rcode = response[3].toInt() and 0x0F
        assertEquals(3, rcode) // NXDOMAIN

        val answerCount = ((response[6].toInt() and 0xFF) shl 8) or (response[7].toInt() and 0xFF)
        assertEquals(0, answerCount)

        // The question section is echoed back byte-for-byte after the 12-byte header.
        val echoedQuestion = response.copyOfRange(12, response.size)
        org.junit.Assert.assertArrayEquals(parsed.rawQuestion, echoedQuestion)
    }
}
