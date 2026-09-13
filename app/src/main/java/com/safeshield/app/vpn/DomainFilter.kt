package com.safeshield.app.vpn

/**
 * Decides whether a domain name should be blocked.
 *
 * This is pure in-memory logic with no dependency on Android, Room, or any
 * I/O, so it's fully unit-testable on the JVM. [com.safeshield.app.database.SafeShieldRepository]
 * (Phase 3) is what keeps the sets here in sync with the Room-backed
 * `domains`/`allowlist` tables — this class doesn't know Room exists.
 *
 * An allowed domain always wins over a blocked one, matching the PRD's
 * Phase 16 allowlist test: `blocked.example` → BLOCK, but if the user adds
 * `blocked.example` to their allowlist, it → ALLOW.
 */
class DomainFilter(
    initialBlockedDomains: Set<String> = DEFAULT_TEST_BLOCKLIST,
    initialAllowedDomains: Set<String> = emptySet()
) {

    @Volatile
    private var blockedDomains: Set<String> = normalizeAll(initialBlockedDomains)

    @Volatile
    private var allowedDomains: Set<String> = normalizeAll(initialAllowedDomains)

    /**
     * True if [domain] (or any of its parent domains) is on the blocklist
     * and neither it nor a parent domain is explicitly allowed — so
     * `sub.blocked-example.test` matches a blocked `blocked-example.test`
     * just as much as an exact hit does, but an allowlist entry for either
     * one overrides that.
     */
    fun isBlocked(domain: String): Boolean {
        val normalized = normalize(domain) ?: return false
        if (matches(normalized, allowedDomains)) return false
        return matches(normalized, blockedDomains)
    }

    /** Replaces the blocklist wholesale, e.g. after a blocklist sync (Phase 12) or a Room update (Phase 3). */
    fun updateBlockedDomains(domains: Set<String>) {
        blockedDomains = normalizeAll(domains)
    }

    /** Replaces the allowlist wholesale, e.g. after the user edits it in Settings. */
    fun updateAllowedDomains(domains: Set<String>) {
        allowedDomains = normalizeAll(domains)
    }

    fun currentBlockedDomains(): Set<String> = blockedDomains
    fun currentAllowedDomains(): Set<String> = allowedDomains

    /** True if [normalizedDomain] or any of its parent domains is in [set]. Both must already be normalized. */
    private fun matches(normalizedDomain: String, set: Set<String>): Boolean {
        if (normalizedDomain in set) return true
        var dotIndex = normalizedDomain.indexOf('.')
        while (dotIndex != -1) {
            val parent = normalizedDomain.substring(dotIndex + 1)
            if (parent.isEmpty()) break
            if (parent in set) return true
            dotIndex = normalizedDomain.indexOf('.', dotIndex + 1)
        }
        return false
    }

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
