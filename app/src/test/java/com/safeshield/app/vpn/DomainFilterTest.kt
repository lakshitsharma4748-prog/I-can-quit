package com.safeshield.app.vpn

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Covers the exact cases the PRD's Phase 16 test plan calls out. */
class DomainFilterTest {

    private val filter = DomainFilter(setOf("blocked.example", "adult-example.test"))

    @Test
    fun `exact match is blocked`() {
        assertTrue(filter.isBlocked("blocked.example"))
    }

    @Test
    fun `subdomain of a blocked domain is blocked`() {
        assertTrue(filter.isBlocked("sub.blocked.example"))
        assertTrue(filter.isBlocked("deeply.nested.sub.blocked.example"))
    }

    @Test
    fun `unrelated domain is allowed`() {
        assertFalse(filter.isBlocked("safe.example"))
    }

    @Test
    fun `domain that merely shares a suffix string is not blocked`() {
        // "notblocked.example" ends in "blocked.example" as a *string* but is
        // not a subdomain of it — matching must be label-aware, not a bare
        // suffix check.
        assertFalse(filter.isBlocked("notblocked.example"))
    }

    @Test
    fun `matching is case-insensitive and ignores a trailing root dot`() {
        assertTrue(filter.isBlocked("BLOCKED.EXAMPLE"))
        assertTrue(filter.isBlocked("blocked.example."))
    }

    @Test
    fun `default blocklist contains the PRD test domains`() {
        val default = DomainFilter()
        assertTrue(default.isBlocked("blocked-example.test"))
        assertTrue(default.isBlocked("adult-example.test"))
        assertFalse(default.isBlocked("safe.example"))
    }

    @Test
    fun `updateBlockedDomains replaces the list wholesale`() {
        val filter = DomainFilter(setOf("old.example"))
        filter.updateBlockedDomains(setOf("new.example"))
        assertFalse(filter.isBlocked("old.example"))
        assertTrue(filter.isBlocked("new.example"))
    }
}
