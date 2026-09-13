package com.safeshield.app.security

import kotlin.math.min
import kotlin.math.pow

/**
 * Pure attempt-counting/lockout math for the administrator PIN, with no
 * storage of its own — [PinManager] persists the [Snapshot] it produces
 * between calls. Kept separate from storage so the backoff schedule itself
 * is unit-testable without touching SharedPreferences/Keystore.
 */
internal class RateLimiter(
    private val attemptsBeforeLockout: Int = 5,
    private val baseLockoutMillis: Long = 30_000,
    private val maxLockoutMillis: Long = 5 * 60_000
) {
    data class Snapshot(val failedAttempts: Int, val lockedOutUntil: Long)

    /** Null lockout remaining means "not currently locked out". */
    fun remainingLockoutMillis(snapshot: Snapshot, now: Long): Long? {
        val remaining = snapshot.lockedOutUntil - now
        return if (remaining > 0) remaining else null
    }

    /** Call after a correct PIN entry. */
    fun onSuccess(): Snapshot = Snapshot(failedAttempts = 0, lockedOutUntil = 0)

    /**
     * Call after an incorrect PIN entry. Lockout duration doubles each time
     * the attempt count passes [attemptsBeforeLockout], capped at
     * [maxLockoutMillis], so repeated guessing gets slower rather than
     * merely blocked-then-immediately-retryable.
     */
    fun onFailure(previous: Snapshot, now: Long): Snapshot {
        val attempts = previous.failedAttempts + 1
        if (attempts < attemptsBeforeLockout) {
            return Snapshot(failedAttempts = attempts, lockedOutUntil = 0)
        }
        val overBy = attempts - attemptsBeforeLockout
        val duration = min(
            baseLockoutMillis * 2.0.pow(overBy).toLong().coerceAtLeast(baseLockoutMillis),
            maxLockoutMillis
        )
        return Snapshot(failedAttempts = attempts, lockedOutUntil = now + duration)
    }
}
