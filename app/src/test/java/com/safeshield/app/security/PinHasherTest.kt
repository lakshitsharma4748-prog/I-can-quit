package com.safeshield.app.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinHasherTest {

    @Test
    fun `same PIN and salt hash identically`() {
        val salt = PinHasher.generateSalt()
        val hash1 = PinHasher.hash("1234".toCharArray(), salt)
        val hash2 = PinHasher.hash("1234".toCharArray(), salt)
        assertTrue(hash1.contentEquals(hash2))
    }

    @Test
    fun `different salts produce different hashes for the same PIN`() {
        val hash1 = PinHasher.hash("1234".toCharArray(), PinHasher.generateSalt())
        val hash2 = PinHasher.hash("1234".toCharArray(), PinHasher.generateSalt())
        assertNotEquals(hash1.toList(), hash2.toList())
    }

    @Test
    fun `matches returns true only for the correct PIN`() {
        val salt = PinHasher.generateSalt()
        val hash = PinHasher.hash("4321".toCharArray(), salt)

        assertTrue(PinHasher.matches("4321".toCharArray(), salt, hash))
        assertFalse(PinHasher.matches("1111".toCharArray(), salt, hash))
    }

    @Test
    fun `generateSalt does not repeat`() {
        val salts = (1..20).map { PinHasher.generateSalt().toList() }.toSet()
        assertTrue(salts.size == 20) // vanishingly unlikely to collide unless SecureRandom is broken
    }
}
