package com.safeshield.app.vpn

import android.content.Intent
import android.net.VpnService
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ServiceTestRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises the real VpnService lifecycle (PRD Phase 16: "VPN tests: Start,
 * Stop"). Requires VPN consent to already be granted to this app — a test
 * run (unlike a real user tapping through the system dialog) has no way to
 * click through that UI, so this pre-authorizes via adb instead:
 *
 * ```
 * adb shell appops set com.safeshield.app ACTIVATE_VPN allow
 * ```
 *
 * Without that, `VpnService.prepare()` still returns a non-null consent
 * intent and this test skips itself (via `assumeTrue`) rather than
 * failing — an environment that hasn't run the command above isn't a
 * broken build, just an unprepared one. Could not be run in this sandbox
 * (no Android SDK/emulator).
 */
@RunWith(AndroidJUnit4::class)
class SafeShieldVpnServiceTest {

    @get:Rule
    val serviceRule = ServiceTestRule()

    @Test
    fun startingTheService_connectsTheVpn_andStoppingDisconnectsIt() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assumeTrue(
            "VPN consent not pre-granted — run: adb shell appops set ${context.packageName} ACTIVATE_VPN allow",
            VpnService.prepare(context) == null
        )

        serviceRule.startService(Intent(context, SafeShieldVpnService::class.java))

        val afterStart = runBlocking {
            withTimeoutOrNull(5_000) {
                VpnConnectionState.state.first { it == VpnState.CONNECTED || it == VpnState.ERROR }
            }
        }
        assertEquals(VpnState.CONNECTED, afterStart)

        SafeShieldVpnService.stop(context)

        val afterStop = runBlocking {
            withTimeoutOrNull(5_000) {
                VpnConnectionState.state.first { it == VpnState.DISCONNECTED }
            }
        }
        assertEquals(VpnState.DISCONNECTED, afterStop)
    }
}
