package com.safeshield.app.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Single-row app settings table. [id] is always [SINGLETON_ID] — Room
 * doesn't have a native "singleton table" concept, so callers upsert with
 * that fixed id rather than tracking a real primary key.
 */
@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey
    val id: Int = SINGLETON_ID,
    val protectionEnabled: Boolean = false,
    val blockAdultContent: Boolean = true,
    val lastBlocklistUpdate: Long? = null,
    /** The `version` string from the last blocklist sync that was actually checked — lets BlocklistUpdater skip re-downloading an unchanged list via `GET /api/version` first. */
    val lastAppliedBlocklistVersion: String? = null
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}
