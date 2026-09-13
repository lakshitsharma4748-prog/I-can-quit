package com.safeshield.app.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Schema export is off for now (no migrations exist yet at v1) — turn it on
 * and start committing schema JSON files under app/schemas the moment a
 * v2 migration is needed.
 */
@Database(
    entities = [DomainEntity::class, AllowlistEntity::class, SettingsEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun domainDao(): DomainDao
    abstract fun allowlistDao(): AllowlistDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "safeshield.db"
                ).build().also { instance = it }
            }
    }
}
