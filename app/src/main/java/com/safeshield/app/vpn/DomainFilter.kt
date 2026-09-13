package com.safeshield.app.vpn

/**
 * Decides whether a domain name should be blocked.
 *
 * This is the Phase 2 MVP: a small, in-memory blocklist with the two test
 * domains from the PRD. It has no dependency on Android, Room, or any I/O,
 * so it can be unit-tested directly on the JVM. Phase 3 replaces the
 * in-memory set with one backed by the Room `domains`/`allowlist` tables —
 * the call sites in [PacketProcessor] should not need to change, only how
 * this class is constructed/updated.
 */
class DomainFilter(initialBlockedDomains: Set<String> = DEFAULT_TEST_BLOCKLIST) {

    @Volatile
    private var blockedDomains: Set<String> = normalizeAll(initialBlockedDomains)

    /**
     * True if [domain] (or any of its parent domains) is on the blocklist,
     * so `sub.blocked-example.test` matches a blocked `blocked-example.test`
     * just as much as an exact hit does.
     */
    fun isBlocked(domain: String): Boolean {
        val normalized = normalize(domain) ?: return false
        if (normalized in blockedDomains) return true

        var dotIndex = normalized.indexOf('.')
        while (dotIndex != -1) {
            val parent = normalized.substring(dotIndex + 1)
            if (parent.isEmpty()) break
            if (parent in blockedDomains) return true
            dotIndex = normalized.indexOf('.', dotIndex + 1)
        }
        return false
    }

    /** Replaces the blocklist wholesale, e.g. after a blocklist sync (Phase 12). */
    fun updateBlockedDomains(domains: Set<String>) {
        blockedDomains = normalizeAll(domains)
    }

    fun currentBlockedDomains(): Set<String> = blockedDomains

    companion object {
        val DEFAULT_TEST_BLOCKLIST = setOf("blocked-example.test", "adult-example.test")

        /** Lowercases and strips an optional trailing root-zone dot; returns null for a blank/invalid entry. */
        internal fun normalize(domain: String): String? {
            val trimmed = domain.trim().lowercase().removeSuffix(".")
            return trimmed.ifEmpty { null }
        }

        private fun normalizeAll(domains: Set<String>): Set<String> =
            domains.mapNotNull { normalize(it) }.toSet()
    }
}
