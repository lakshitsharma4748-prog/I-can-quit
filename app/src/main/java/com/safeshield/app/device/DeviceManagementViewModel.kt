package com.safeshield.app.device

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Exposes [DeviceManagementState] (process-wide) to the UI, and is the
 * on-demand half of keeping it current — the other half is
 * [DeviceOwnerChangeReceiver], which reacts to Android's own
 * `ACTION_DEVICE_OWNER_CHANGED` broadcast for changes that happen while
 * nothing in the UI is actively asking.
 */
class DeviceManagementViewModel(application: Application) : AndroidViewModel(application) {

    private val controller = DeviceManagementController(application)

    val state: StateFlow<ManagementState> = DeviceManagementState.state

    private val _policyOutcomes = MutableStateFlow<List<DeviceManagementController.PolicyOutcome>>(emptyList())
    /** Empty unless [state] is [ManagementState.DEVICE_OWNER] — see [DeviceManagementController.applyStrongProtectionPolicies]. */
    val policyOutcomes: StateFlow<List<DeviceManagementController.PolicyOutcome>> = _policyOutcomes.asStateFlow()

    init {
        refresh()
    }

    /**
     * Re-reads management state and, if Device Owner, (re-)applies Phase
     * 7's policies. Re-applying is deliberate and safe to call often: every
     * underlying `DevicePolicyManager` call here is idempotent (asking to
     * block an already-blocked uninstall, or add an already-added
     * restriction, is a no-op), so this doubles as the mechanism that
     * detects "we just became Device Owner" without needing a separate
     * one-time trigger.
     */
    fun refresh() {
        DeviceManagementState.update(controller.currentState())
        _policyOutcomes.value = if (state.value == ManagementState.DEVICE_OWNER) {
            controller.applyStrongProtectionPolicies()
        } else {
            emptyList()
        }
    }

    fun deviceOwnerProvisioningIntent(): Intent = controller.deviceOwnerProvisioningIntent()

    fun legacyDeviceAdminIntent(explanation: String): Intent = controller.legacyDeviceAdminIntent(explanation)

    fun describeSupportedPolicies(): String = controller.describeSupportedPolicies(state.value)
}
