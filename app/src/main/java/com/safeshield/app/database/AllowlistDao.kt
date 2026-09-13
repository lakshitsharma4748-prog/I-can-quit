package com.safeshield.app.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AllowlistDao {

    @Query("SELECT * FROM allowlist ORDER BY domain ASC")
    fun observeAll(): Flow<List<AllowlistEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun add(entry: AllowlistEntity)

    @Query("DELETE FROM allowlist WHERE domain = :domain")
    suspend fun remove(domain: String)

    @Delete
    suspend fun remove(entry: AllowlistEntity)
}
