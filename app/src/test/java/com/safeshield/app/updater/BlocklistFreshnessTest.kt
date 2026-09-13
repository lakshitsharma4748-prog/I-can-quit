package com.safeshield.app.updater

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.TimeUnit

class BlocklistFreshnessTest {

    private val now = 1_000_000_000L
    private val staleAfter = TimeUnit.HOURS.toMillis(36)

    @Test
    fun `never synced is NOT_LOADED`() {
        assertEquals(BlocklistFreshness.NOT_LOADED, classifyBlocklistFreshness(null, now, staleAfter))
    }

    @Test
    fun `synced just now is UP_TO_DATE`() {
        assertEquals(BlocklistFreshness.UP_TO_DATE, classifyBlocklistFreshness(now, now, staleAfter))
    }

    @Test
    fun `synced exactly at the staleness boundary is still UP_TO_DATE`() {
        assertEquals(
            BlocklistFreshness.UP_TO_DATE,
            classifyBlocklistFreshness(now - staleAfter, now, staleAfter)
        )
    }

    @Test
    fun `synced just past the staleness boundary is STALE`() {
        assertEquals(
            BlocklistFreshness.STALE,
            classifyBlocklistFreshness(now - staleAfter - 1, now, staleAfter)
        )
    }

    @Test
    fun `synced a week ago is STALE`() {
        val aWeek = TimeUnit.DAYS.toMillis(7)
        assertEquals(BlocklistFreshness.STALE, classifyBlocklistFreshness(now - aWeek, now, staleAfter))
    }
}
