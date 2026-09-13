package com.safeshield.app.database

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises the real Room schema (in-memory, so nothing touches disk).
 *
 * Instrumented (requires a device/emulator) rather than a JVM unit test —
 * Room 2.6's SQLite bindings need the real Android SQLite implementation,
 * which isn't available to plain `testDebugUnitTest`. This could not be run
 * in the sandbox this project was scaffolded in (no Android SDK/emulator);
 * run it via `./gradlew connectedAndroidTest` on a real device or emulator.
 */
@RunWith(AndroidJUnit4::class)
class AppDatabaseTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: SafeShieldRepository

    @Before
    fun createDatabase() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = SafeShieldRepository(database.domainDao(), database.allowlistDao(), database.settingsDao())
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun ensureDefaultTestBlocklist_seedsOnlyWhenEmpty() = runBlocking {
        repository.ensureDefaultTestBlocklist()
        val first = repository.activeBlockedDomains.first()
        assertEquals(setOf("blocked-example.test", "adult-example.test"), first)

        repository.setDomainEnabled("blocked-example.test", enabled = false)
        repository.ensureDefaultTestBlocklist() // should be a no-op now that the table isn't empty
        val second = repository.activeBlockedDomains.first()
        assertEquals(setOf("adult-example.test"), second) // the disabled domain stays disabled
    }

    @Test
    fun replaceCategoryDomains_isAtomicAndDoesNotAffectOtherCategories() = runBlocking {
        repository.replaceCategoryDomains(DomainEntity.CATEGORY_TEST, setOf("a.test", "b.test"))
        repository.replaceCategoryDomains(DomainEntity.CATEGORY_ADULT, setOf("c.example"))

        repository.replaceCategoryDomains(DomainEntity.CATEGORY_TEST, setOf("d.test"))

        val all = repository.activeBlockedDomains.first()
        assertEquals(setOf("d.test", "c.example"), all)
    }

    @Test
    fun allowlist_addAndRemove() = runBlocking {
        repository.addToAllowlist("Allowed.Example")
        assertEquals(setOf("allowed.example"), repository.allowedDomains.first())

        repository.removeFromAllowlist("allowed.example")
        assertTrue(repository.allowedDomains.first().isEmpty())
    }

    @Test
    fun settings_defaultsToProtectionOff_andPersistsChanges() = runBlocking {
        val defaults = repository.currentSettings()
        assertEquals(false, defaults.protectionEnabled)
        assertEquals(true, defaults.blockAdultContent)
        assertNull(defaults.lastBlocklistUpdate)

        repository.setProtectionEnabled(true)
        assertEquals(true, repository.currentSettings().protectionEnabled)
    }
}
