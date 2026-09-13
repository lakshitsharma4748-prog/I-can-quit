package com.safeshield.app.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** A domain the user has explicitly chosen to always allow, overriding any blocklist match (PRD Phase 16 allowlist tests). */
@Entity(
    tableName = "allowlist",
    indices = [Index(value = ["domain"], unique = true)]
)
data class AllowlistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val domain: String,
    val createdAt: Long
)
