package com.safeshield.app.device

import android.app.admin.DevicePolicyManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Covers what's testable without an actual Device Owner-provisioned
 * device (PRD Phase 16: "Device-management tests where testable"):
 * accurate state detection on an unmanaged test device, and that the
 * provisioning/admin intents are shaped correctly. Real Device Owner
 * behavior (policy application, state transitions) needs a device
 * provisioned per DEVICE_OWNER_PROVISIONING.md — not possible in this
 * sandbox (no Android SDK/emulator) or in an ordinary instrumented test
 * run either.
 */
@RunWith(AndroidJUnit4::class)
class DeviceManagementControllerTest {

    private lateinit var controller: DeviceManagementController

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        controller = DeviceManagementController(context)
    }

    @Test
    fun currentState_isNotManagedOnAnOrdinaryTestDevice() {
        // A CI/test device is not expected to be Device Owner-provisioned;
        // if this ever fails, it means the test environment itself is
        // configured in a way this suite doesn't account for.
        assertEquals(ManagementState.NOT_MANAGED, controller.currentState())
    }

    @Test
    fun deviceOwnerProvisioningIntent_hasTheOfficialActionAndAdminComponent() {
        val intent = controller.deviceOwnerProvisioningIntent()
        assertEquals(DevicePolicyManager.ACTION_PROVISION_MANAGED_DEVICE, intent.action)
        assertTrue(intent.hasExtra(DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME))
    }

    @Test
    fun legacyDeviceAdminIntent_hasTheOfficialActionAndExplanation() {
        val intent = controller.legacyDeviceAdminIntent("test explanation")
        assertEquals(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN, intent.action)
        assertEquals("test explanation", intent.getStringExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION))
        assertTrue(intent.hasExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN))
    }

    @Test
    fun applyStrongProtectionPolicies_isANoOpWhenNotDeviceOwner() {
        val outcomes = controller.applyStrongProtectionPolicies()
        assertTrue(outcomes.isNotEmpty())
        assertTrue(outcomes.all { !it.applied })
    }

    @Test
    fun describeSupportedPolicies_isNonEmptyForEveryState() {
        for (state in ManagementState.entries) {
            assertFalse(controller.describeSupportedPolicies(state).isBlank())
        }
    }
}
