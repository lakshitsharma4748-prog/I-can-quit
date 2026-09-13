package com.safeshield.app.device

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Exposes the current [ManagementState] to the UI. Unlike VPN state (which
 * the running service updates reactively), Device Owner/Admin status can
 * only be *asked for* via `DevicePolicyManager` — there's no push
 * notification for "you're Device Owner now" — so [refresh] must be called
 * at points where the state could plausibly have changed (Home appearing,
 * returning from the provisioning system UI).
 */
class DeviceManagementViewModel(application: Application) : AndroidViewModel(application) {

    private val controller = DeviceManagementController(application)

    private val _state = MutableStateFlow(controller.currentState())
    val state: StateFlow<ManagementState> = _state.asStateFlow()

    private val _policyOutcomes = MutableStateFlow<List<DeviceManagementController.PolicyOutcome>>(emptyList())
    /** Empty unless [state] is [ManagementState.DEVICE_OWNER] — see [DeviceManagementController.applyStrongProtectionPolicies]. */
    val policyOutcomes: StateFlow<List<DeviceManagementController.PolicyOutcome>> = _policyOutcomes.asStateFlow()

    init {
        applyPoliciesIfOwner()
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
        _state.value = controller.currentState()
        applyPoliciesIfOwner()
    }

    private fun applyPoliciesIfOwner() {
        _policyOutcomes.value = if (_state.value == ManagementState.DEVICE_OWNER) {
            controller.applyStrongProtectionPolicies()
        } else {
            emptyList()
        }
    }

    fun deviceOwnerProvisioningIntent(): Intent = controller.deviceOwnerProvisioningIntent()

    fun legacyDeviceAdminIntent(explanation: String): Intent = controller.legacyDeviceAdminIntent(explanation)

    fun describeSupportedPolicies(): String = controller.describeSupportedPolicies(state.value)
}
