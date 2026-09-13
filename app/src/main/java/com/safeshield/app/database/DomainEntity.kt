package com.safeshield.app.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One blocklist entry. `category` exists so the blocklist can eventually
 * carry more than one kind of content (Phase 12's synced blocklist is
 * expected to ship at least an "adult" category); Phase 2's hardcoded test
 * domains map to category [CATEGORY_TEST].
 */
@Entity(
    tableName = "domains",
    indices = [Index(value = ["domain"], unique = true)]
)
data class DomainEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val domain: String,
    val category: String,
    val enabled: Boolean = true,
    val updatedAt: Long
) {
    companion object {
        const val CATEGORY_TEST = "test"
        const val CATEGORY_ADULT = "adult"
    }
}
