package com.safeshield.app.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface DomainDao {

    /** Backed by the unique index on `domain`; used by the VPN's startup/refresh path, not per-packet. */
    @Query("SELECT * FROM domains WHERE enabled = 1")
    fun observeEnabled(): Flow<List<DomainEntity>>

    @Query("SELECT * FROM domains ORDER BY domain ASC")
    fun observeAll(): Flow<List<DomainEntity>>

    @Query("SELECT COUNT(*) FROM domains")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(domain: DomainEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(domains: List<DomainEntity>)

    @Query("UPDATE domains SET enabled = :enabled WHERE domain = :domain")
    suspend fun setEnabled(domain: String, enabled: Boolean)

    @Query("DELETE FROM domains WHERE domain = :domain")
    suspend fun delete(domain: String)

    @Query("DELETE FROM domains WHERE category = :category")
    suspend fun deleteByCategory(category: String)

    /**
     * Replaces every entry in [category] with [domains] in one transaction,
     * so a blocklist sync (Phase 12) never leaves the table half-updated if
     * it fails partway through, and readers never observe a moment with an
     * empty blocklist mid-update.
     */
    @Transaction
    suspend fun replaceCategory(category: String, domains: List<DomainEntity>) {
        deleteByCategory(category)
        upsertAll(domains)
    }
}
