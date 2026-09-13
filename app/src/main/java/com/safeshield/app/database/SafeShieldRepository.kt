package com.safeshield.app.database

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * The single entry point the rest of the app uses to read/write persisted
 * domains, allowlist entries, and settings — screens and the VPN service
 * talk to this, not to the DAOs directly.
 *
 * This does not push updates into [com.safeshield.app.vpn.DomainFilter]
 * itself. That wiring (observe [activeBlockedDomains] and
 * [allowedDomains], call `DomainFilter.updateBlockedDomains`/
 * `updateAllowedDomains` whenever they change) belongs to whatever owns the
 * VPN's lifecycle, landing in Phase 5 alongside the rest of Standard
 * Protection's real activation logic.
 */
class SafeShieldRepository(
    private val domainDao: DomainDao,
    private val allowlistDao: AllowlistDao,
    private val settingsDao: SettingsDao,
    private val clock: () -> Long = System::currentTimeMillis
) {

    /** All enabled domain names, categories collapsed — this is what should ever reach [com.safeshield.app.vpn.DomainFilter.updateBlockedDomains]. */
    val activeBlockedDomains: Flow<Set<String>> =
        domainDao.observeEnabled().map { entities -> entities.map { it.domain }.toSet() }

    val allowedDomains: Flow<Set<String>> =
        allowlistDao.observeAll().map { entities -> entities.map { it.domain }.toSet() }

    val allDomains: Flow<List<DomainEntity>> = domainDao.observeAll()
    val allowlistEntries: Flow<List<AllowlistEntity>> = allowlistDao.observeAll()

    /** Always emits a value — a default, protection-off row if nothing has been saved yet. */
    val settings: Flow<SettingsEntity> =
        settingsDao.observe().map { it ?: SettingsEntity() }

    suspend fun currentSettings(): SettingsEntity = settingsDao.get() ?: SettingsEntity()

    suspend fun setProtectionEnabled(enabled: Boolean) {
        settingsDao.upsert(currentSettings().copy(protectionEnabled = enabled))
    }

    suspend fun setBlockAdultContent(enabled: Boolean) {
        settingsDao.upsert(currentSettings().copy(blockAdultContent = enabled))
    }

    suspend fun recordBlocklistUpdate(atEpochMillis: Long = clock()) {
        settingsDao.upsert(currentSettings().copy(lastBlocklistUpdate = atEpochMillis))
    }

    suspend fun addToAllowlist(domain: String) {
        allowlistDao.add(AllowlistEntity(domain = domain.trim().lowercase(), createdAt = clock()))
    }

    suspend fun removeFromAllowlist(domain: String) {
        allowlistDao.remove(domain.trim().lowercase())
    }

    suspend fun setDomainEnabled(domain: String, enabled: Boolean) {
        domainDao.setEnabled(domain, enabled)
    }

    /**
     * Atomically replaces every domain in [category] (e.g. re-seeding the
     * test blocklist, or applying a Phase 12 sync) without ever leaving the
     * table transiently empty for readers of [activeBlockedDomains].
     */
    suspend fun replaceCategoryDomains(category: String, domains: Set<String>) {
        val now = clock()
        domainDao.replaceCategory(
            category,
            domains.map { DomainEntity(domain = it.trim().lowercase(), category = category, updatedAt = now) }
        )
    }

    /** Seeds the Phase 2 test blocklist into Room on first run, so a fresh install has something to filter. */
    suspend fun ensureDefaultTestBlocklist() {
        if (domainDao.count() == 0) {
            replaceCategoryDomains(
                DomainEntity.CATEGORY_TEST,
                setOf("blocked-example.test", "adult-example.test")
            )
        }
    }
}
