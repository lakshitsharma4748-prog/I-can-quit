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
 * only be *asked for* via [DevicePolicyManager] — there's no push
 * notification for "you're Device Owner now" — so [refresh] must be called
 * at points where the state could plausibly have changed (Home appearing,
 * returning from the provisioning system UI).
 */
class DeviceManagementViewModel(application: Application) : AndroidViewModel(application) {

    private val controller = DeviceManagementController(application)

    private val _state = MutableStateFlow(controller.currentState())
    val state: StateFlow<ManagementState> = _state.asStateFlow()

    fun refresh() {
        _state.value = controller.currentState()
    }

    fun deviceOwnerProvisioningIntent(): Intent = controller.deviceOwnerProvisioningIntent()

    fun legacyDeviceAdminIntent(explanation: String): Intent = controller.legacyDeviceAdminIntent(explanation)

    fun describeSupportedPolicies(): String = controller.describeSupportedPolicies(state.value)
}
