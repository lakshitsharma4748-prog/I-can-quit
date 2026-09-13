package com.safeshield.app.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RateLimiterTest {

    private val limiter = RateLimiter(attemptsBeforeLockout = 3, baseLockoutMillis = 1_000, maxLockoutMillis = 8_000)

    @Test
    fun `no lockout below the failure threshold`() {
        var snapshot = RateLimiter.Snapshot(0, 0)
        repeat(2) { snapshot = limiter.onFailure(snapshot, now = 0) }
        assertEquals(2, snapshot.failedAttempts)
        assertNull(limiter.remainingLockoutMillis(snapshot, now = 0))
    }

    @Test
    fun `lockout begins once the threshold is reached and doubles thereafter`() {
        var snapshot = RateLimiter.Snapshot(0, 0)
        repeat(3) { snapshot = limiter.onFailure(snapshot, now = 0) } // 3rd failure = threshold
        assertEquals(1_000L, snapshot.lockedOutUntil)

        snapshot = limiter.onFailure(snapshot, now = 0) // 4th failure
        assertEquals(2_000L, snapshot.lockedOutUntil)

        snapshot = limiter.onFailure(snapshot, now = 0) // 5th failure
        assertEquals(4_000L, snapshot.lockedOutUntil)
    }

    @Test
    fun `lockout duration is capped`() {
        var snapshot = RateLimiter.Snapshot(0, 0)
        repeat(10) { snapshot = limiter.onFailure(snapshot, now = 0) }
        assertTrue(snapshot.lockedOutUntil - 0 <= 8_000L)
    }

    @Test
    fun `remainingLockoutMillis is null once the lockout has elapsed`() {
        val snapshot = RateLimiter.Snapshot(failedAttempts = 3, lockedOutUntil = 1_000)
        assertNull(limiter.remainingLockoutMillis(snapshot, now = 1_001))
        assertEquals(500L, limiter.remainingLockoutMillis(snapshot, now = 500))
    }

    @Test
    fun `onSuccess clears attempts and lockout`() {
        val snapshot = limiter.onSuccess()
        assertEquals(0, snapshot.failedAttempts)
        assertEquals(0L, snapshot.lockedOutUntil)
    }
}
