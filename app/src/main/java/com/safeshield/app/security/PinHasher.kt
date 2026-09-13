package com.safeshield.app.security

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * The actual PIN hashing primitives, kept free of any Android/SharedPreferences
 * dependency so they can be unit-tested directly on the JVM (PBKDF2 via
 * `javax.crypto` is standard JDK, not an Android API). [PinManager] is the
 * Android-facing class that persists what this produces.
 */
internal object PinHasher {
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    const val SALT_LENGTH_BYTES = 16
    const val ITERATIONS = 120_000
    const val KEY_LENGTH_BITS = 256

    fun generateSalt(): ByteArray = ByteArray(SALT_LENGTH_BYTES).also { SecureRandom().nextBytes(it) }

    fun hash(pin: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin, salt, ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance(ALGORITHM)
        return try {
            factory.generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    /** Constant-time comparison — a PIN hash check must not leak timing information about how much of it matched. */
    fun matches(pin: CharArray, salt: ByteArray, expectedHash: ByteArray): Boolean =
        MessageDigest.isEqual(hash(pin, salt), expectedHash)
}
