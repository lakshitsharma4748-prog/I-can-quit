package com.safeshield.app.security

import android.content.Context
import android.util.Base64

/**
 * Administrator PIN storage and verification (PRD Phase 4).
 *
 * The PIN itself is never stored — only a random salt and a PBKDF2 hash of
 * (PIN, salt), inside [SecureStorage]'s Keystore-encrypted preferences.
 * Rate limiting is delegated to [RateLimiter]; this class's job is just to
 * persist that state between calls and expose a small, honest result type
 * rather than a plain boolean (a caller needs to tell "wrong PIN" apart
 * from "you're locked out" apart from "no PIN has been set yet").
 */
class PinManager(context: Context) {

    private val prefs = SecureStorage.encryptedPreferences(context)
    private val rateLimiter = RateLimiter()

    sealed class VerifyResult {
        object Success : VerifyResult()
        object IncorrectPin : VerifyResult()
        data class LockedOut(val retryAfterMillis: Long) : VerifyResult()
        object NotConfigured : VerifyResult()
    }

    fun isPinSet(): Boolean = prefs.contains(KEY_HASH)

    /** Sets or overwrites the PIN, clearing any prior lockout/attempt state. */
    fun setPin(pin: String) {
        require(pin.length in MIN_PIN_LENGTH..MAX_PIN_LENGTH) {
            "PIN must be $MIN_PIN_LENGTH-$MAX_PIN_LENGTH digits"
        }
        require(pin.all { it.isDigit() }) { "PIN must be numeric" }

        val salt = PinHasher.generateSalt()
        val hash = PinHasher.hash(pin.toCharArray(), salt)
        prefs.edit()
            .putString(KEY_SALT, salt.toBase64())
            .putString(KEY_HASH, hash.toBase64())
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .putLong(KEY_LOCKED_UNTIL, 0)
            .apply()
    }

    fun verifyPin(pin: String, now: Long = System.currentTimeMillis()): VerifyResult {
        if (!isPinSet()) return VerifyResult.NotConfigured

        val snapshot = currentSnapshot()
        rateLimiter.remainingLockoutMillis(snapshot, now)?.let { remaining ->
            return VerifyResult.LockedOut(remaining)
        }

        val salt = prefs.getString(KEY_SALT, null)?.fromBase64() ?: return VerifyResult.NotConfigured
        val expectedHash = prefs.getString(KEY_HASH, null)?.fromBase64() ?: return VerifyResult.NotConfigured

        return if (PinHasher.matches(pin.toCharArray(), salt, expectedHash)) {
            persist(rateLimiter.onSuccess())
            VerifyResult.Success
        } else {
            persist(rateLimiter.onFailure(snapshot, now))
            VerifyResult.IncorrectPin
        }
    }

    /** Requires [currentPin] to verify successfully before applying [newPin]. Returns the same [VerifyResult] verifyPin would. */
    fun changePin(currentPin: String, newPin: String): VerifyResult {
        val result = verifyPin(currentPin)
        if (result is VerifyResult.Success) {
            setPin(newPin)
        }
        return result
    }

    private fun currentSnapshot() = RateLimiter.Snapshot(
        failedAttempts = prefs.getInt(KEY_FAILED_ATTEMPTS, 0),
        lockedOutUntil = prefs.getLong(KEY_LOCKED_UNTIL, 0)
    )

    private fun persist(snapshot: RateLimiter.Snapshot) {
        prefs.edit()
            .putInt(KEY_FAILED_ATTEMPTS, snapshot.failedAttempts)
            .putLong(KEY_LOCKED_UNTIL, snapshot.lockedOutUntil)
            .apply()
    }

    private fun ByteArray.toBase64(): String = Base64.encodeToString(this, Base64.NO_WRAP)
    private fun String.fromBase64(): ByteArray = Base64.decode(this, Base64.NO_WRAP)

    companion object {
        const val MIN_PIN_LENGTH = 4
        const val MAX_PIN_LENGTH = 8

        private const val KEY_SALT = "pin_salt"
        private const val KEY_HASH = "pin_hash"
        private const val KEY_FAILED_ATTEMPTS = "pin_failed_attempts"
        private const val KEY_LOCKED_UNTIL = "pin_locked_until"
    }
}
