package com.safeshield.app.vpn

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationDebouncerTest {

    @Test
    fun `first notification (no prior timestamp) is always allowed`() {
        assertTrue(NotificationDebouncer.shouldNotify(null, now = 1_000, minIntervalMillis = 5_000))
    }

    @Test
    fun `a notification within the interval is suppressed`() {
        assertFalse(NotificationDebouncer.shouldNotify(lastNotifiedAtEpochMillis = 1_000, now = 3_000, minIntervalMillis = 5_000))
    }

    @Test
    fun `a notification exactly at the interval boundary is allowed`() {
        assertTrue(NotificationDebouncer.shouldNotify(lastNotifiedAtEpochMillis = 1_000, now = 6_000, minIntervalMillis = 5_000))
    }

    @Test
    fun `a notification well past the interval is allowed`() {
        assertTrue(NotificationDebouncer.shouldNotify(lastNotifiedAtEpochMillis = 1_000, now = 60_000, minIntervalMillis = 5_000))
    }
}
