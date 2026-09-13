package com.safeshield.app.vpn

import org.junit.Assert.assertEquals
import org.junit.Test

class ChecksumsTest {

    @Test
    fun `known checksum example from RFC 1071 style verification`() {
        // A buffer that already contains its own correct checksum, verified
        // by re-summing everything including the checksum field: the result
        // must be zero. This is the standard self-check for Internet
        // checksums and doesn't depend on hand-deriving an expected value.
        val data = byteArrayOf(0x45, 0x00, 0x00, 0x28, 0x1c, 0x46, 0x40, 0x00, 0x40, 0x06)
        val checksum = Checksums.compute(data)
        val withChecksum = data + byteArrayOf((checksum shr 8).toByte(), checksum.toByte())
        assertEquals(0, Checksums.compute(withChecksum))
    }

    @Test
    fun `checksum is order-independent across chunk boundaries for even-length chunks`() {
        val a = byteArrayOf(1, 2, 3, 4)
        val b = byteArrayOf(5, 6)
        val combined = a + b
        assertEquals(Checksums.compute(combined), Checksums.compute(a, b))
    }

    @Test
    fun `all-zero buffer checksums to all-ones`() {
        assertEquals(0xFFFF, Checksums.compute(ByteArray(8)))
    }
}
