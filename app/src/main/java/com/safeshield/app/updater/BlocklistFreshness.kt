package com.safeshield.app.updater

import java.util.concurrent.TimeUnit

/** Drives the Home screen's Blocklist status row (PRD Phase 1/14 UI spec). */
enum class BlocklistFreshness {
    NOT_LOADED,
    UP_TO_DATE,
    STALE
}

/**
 * Pure classification logic, kept separate from the UI so it's directly
 * unit-testable: never synced → [BlocklistFreshness.NOT_LOADED]; synced
 * within [staleAfterMillis] → [BlocklistFreshness.UP_TO_DATE]; otherwise
 * [BlocklistFreshness.STALE] (the periodic sync — every 12h, see
 * [BlocklistSyncScheduler] — has clearly missed more than a couple of
 * cycles, e.g. the device has been offline).
 */
fun classifyBlocklistFreshness(
    lastUpdatedEpochMillis: Long?,
    now: Long = System.currentTimeMillis(),
    staleAfterMillis: Long = TimeUnit.HOURS.toMillis(36)
): BlocklistFreshness = when {
    lastUpdatedEpochMillis == null -> BlocklistFreshness.NOT_LOADED
    now - lastUpdatedEpochMillis <= staleAfterMillis -> BlocklistFreshness.UP_TO_DATE
    else -> BlocklistFreshness.STALE
}
