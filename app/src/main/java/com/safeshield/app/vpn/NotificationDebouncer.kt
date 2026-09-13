package com.safeshield.app.vpn

/**
 * A single blocked DNS lookup during normal browsing routinely triggers
 * several more (ads, trackers, sub-resources on the same blocked domain) —
 * without this, that would mean a system notification per query. Pure and
 * stateless by design: the caller owns the "last shown at" timestamp, so
 * this is trivially unit-testable and never touches Android APIs itself.
 */
internal object NotificationDebouncer {
    const val DEFAULT_MIN_INTERVAL_MILLIS = 5_000L

    fun shouldNotify(lastNotifiedAtEpochMillis: Long?, now: Long, minIntervalMillis: Long = DEFAULT_MIN_INTERVAL_MILLIS): Boolean =
        lastNotifiedAtEpochMillis == null || now - lastNotifiedAtEpochMillis >= minIntervalMillis
}
