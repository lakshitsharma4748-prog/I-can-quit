package com.safeshield.app.vpn

/**
 * The "Internet checksum" (RFC 1071) used by both the IPv4 header and the
 * UDP pseudo-header/payload. Pulled out on its own because both
 * [PacketProcessor]'s reply-packet builder and its unit tests need it, and
 * because getting one's-complement checksum math right in two places
 * invites the two copies to disagree.
 */
internal object Checksums {

    /**
     * Sums 16-bit big-endian words across one or more byte ranges as if they
     * were a single contiguous buffer, folds the carry, and returns the
     * one's complement. Accepting multiple chunks lets callers checksum an
     * IPv4/UDP pseudo-header plus the real header plus the payload without
     * concatenating them into one array first.
     *
     * Correct only when every chunk but the last has even length, which
     * holds for all of this file's callers (pseudo-header and UDP/IPv4
     * headers are fixed, even-length structures; only the final payload
     * chunk may be odd, and a trailing odd byte is zero-padded as RFC 1071
     * requires).
     */
    fun compute(vararg chunks: ByteArray): Int {
        var sum = 0
        for (chunk in chunks) {
            var i = 0
            while (i < chunk.size) {
                val hi = chunk[i].toInt() and 0xFF
                val lo = if (i + 1 < chunk.size) chunk[i + 1].toInt() and 0xFF else 0
                sum += (hi shl 8) or lo
                i += 2
            }
        }
        while (sum shr 16 != 0) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }
        return sum.inv() and 0xFFFF
    }
}
