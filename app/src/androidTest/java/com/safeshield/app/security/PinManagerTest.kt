package com.safeshield.app.security

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented — [PinManager] persists through Keystore-backed
 * EncryptedSharedPreferences, which requires the real Android runtime.
 * Could not be run in this sandbox (no Android SDK/emulator); run via
 * `./gradlew connectedAndroidTest` on a real device or emulator.
 */
@RunWith(AndroidJUnit4::class)
class PinManagerTest {

    private lateinit var pinManager: PinManager

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        // Clear any state left by a previous run so every test starts from
        // "no PIN configured" deterministically. Duplicating the file name
        // here (rather than reaching into SecureStorage) keeps this test
        // honest about only depending on PinManager's public contract.
        context.getSharedPreferences("safeshield_secure_prefs", Context.MODE_PRIVATE)
            .edit().clear().commit()
        pinManager = PinManager(context)
    }

    @Test
    fun noPinConfiguredInitially() {
        assertEquals(false, pinManager.isPinSet())
        assertEquals(PinManager.VerifyResult.NotConfigured, pinManager.verifyPin("1234"))
    }

    @Test
    fun setPin_thenVerifyPin_withCorrectPin_succeeds() {
        pinManager.setPin("1234")
        assertTrue(pinManager.isPinSet())
        assertEquals(PinManager.VerifyResult.Success, pinManager.verifyPin("1234"))
    }

    @Test
    fun verifyPin_withWrongPin_fails() {
        pinManager.setPin("1234")
        assertEquals(PinManager.VerifyResult.IncorrectPin, pinManager.verifyPin("0000"))
    }

    @Test
    fun repeatedFailures_triggerLockout() {
        pinManager.setPin("1234")
        repeat(5) { pinManager.verifyPin("0000") }
        assertTrue(pinManager.verifyPin("0000") is PinManager.VerifyResult.LockedOut)
    }

    @Test
    fun changePin_requiresCorrectCurrentPin() {
        pinManager.setPin("1234")
        assertEquals(PinManager.VerifyResult.IncorrectPin, pinManager.changePin("0000", "5678"))
        assertEquals(PinManager.VerifyResult.Success, pinManager.changePin("1234", "5678"))
        assertEquals(PinManager.VerifyResult.Success, pinManager.verifyPin("5678"))
    }

    @Test
    fun setPin_rejectsNonNumericOrWrongLength() {
        assertThrowsIllegalArgument { pinManager.setPin("abcd") }
        assertThrowsIllegalArgument { pinManager.setPin("12") }
        assertThrowsIllegalArgument { pinManager.setPin("123456789") }
    }

    private fun assertThrowsIllegalArgument(block: () -> Unit) {
        var threw = false
        try {
            block()
        } catch (e: IllegalArgumentException) {
            threw = true
        }
        assertTrue("expected IllegalArgumentException", threw)
    }
}
