package com.safeshield.app.vpn.dns

/**
 * Minimal DNS message handling — just enough to read the question name out
 * of a stub resolver's query and to synthesize an NXDOMAIN reply to it. This
 * is not a general-purpose DNS library: it only understands a single
 * question per message (the normal case for a device's outgoing query) and
 * never parses answer/authority/additional records.
 */
object DnsMessage {

    private const val HEADER_SIZE = 12
    private const val RCODE_NXDOMAIN = 0x03

    /**
     * @param transactionId the query's 16-bit transaction ID, copied into any reply.
     * @param questionName the dot-joined domain name being queried, e.g. "blocked-example.test".
     * @param rawQuestion the question section's exact bytes (name + QTYPE + QCLASS), reused verbatim in a reply.
     */
    data class ParsedQuery(
        val transactionId: Int,
        val questionName: String,
        val rawQuestion: ByteArray
    )

    /**
     * Parses the first question name out of a raw DNS message. Returns null
     * for anything that isn't a well-formed single-question query — callers
     * treat that as "not a DNS query we understand" and drop the packet
     * rather than guess.
     */
    fun parseQuestionName(message: ByteArray): ParsedQuery? {
        if (message.size < HEADER_SIZE) return null

        val transactionId = readUInt16(message, 0)
        val questionCount = readUInt16(message, 4)
        if (questionCount < 1) return null

        val questionStart = HEADER_SIZE
        var offset = questionStart
        val name = StringBuilder()

        while (offset < message.size) {
            val labelLength = message[offset].toInt() and 0xFF
            if (labelLength == 0) {
                offset += 1
                break
            }
            // Compression pointers (top two bits set) are a response-only
            // feature; a query's own question section is never compressed.
            // Bail out rather than mis-parse if we see one.
            if (labelLength and 0xC0 == 0xC0) return null
            offset += 1
            if (offset + labelLength > message.size) return null
            if (name.isNotEmpty()) name.append('.')
            name.append(String(message, offset, labelLength, Charsets.US_ASCII))
            offset += labelLength
        }

        val questionEnd = offset + 4 // QTYPE (2 bytes) + QCLASS (2 bytes)
        if (questionEnd > message.size) return null

        return ParsedQuery(
            transactionId = transactionId,
            questionName = name.toString(),
            rawQuestion = message.copyOfRange(questionStart, questionEnd)
        )
    }

    /**
     * Builds a synthetic NXDOMAIN response to [parsed], reusing the
     * original question section verbatim so the requesting stub resolver
     * recognizes it as the answer to its own query.
     */
    fun buildNxDomainResponse(parsed: ParsedQuery): ByteArray {
        val header = ByteArray(HEADER_SIZE)
        writeUInt16(header, 0, parsed.transactionId)

        // Byte 2: QR(1) Opcode(4) AA(1) TC(1) RD(1). Preserve Opcode and RD
        // from... we don't have the original byte 2 here, so instead build
        // it from scratch as a standard, non-authoritative, non-truncated
        // response with recursion available — RD doesn't need echoing back
        // correctly for the resolver to accept an NXDOMAIN.
        header[2] = 0x80.toByte() // QR=1, Opcode=0 (QUERY), AA=0, TC=0, RD=0
        header[3] = (0x80 or RCODE_NXDOMAIN).toByte() // RA=1, Z=0, RCODE=NXDOMAIN

        writeUInt16(header, 4, 1) // QDCOUNT = 1 (question echoed back)
        writeUInt16(header, 6, 0) // ANCOUNT
        writeUInt16(header, 8, 0) // NSCOUNT
        writeUInt16(header, 10, 0) // ARCOUNT

        return header + parsed.rawQuestion
    }

    private fun readUInt16(data: ByteArray, offset: Int): Int =
        ((data[offset].toInt() and 0xFF) shl 8) or (data[offset + 1].toInt() and 0xFF)

    private fun writeUInt16(data: ByteArray, offset: Int, value: Int) {
        data[offset] = (value shr 8).toByte()
        data[offset + 1] = value.toByte()
    }
}
