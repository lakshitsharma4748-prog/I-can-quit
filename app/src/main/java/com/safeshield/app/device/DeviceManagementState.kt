package com.safeshield.app.device

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process-wide, observable Device Owner/Admin state — the device-management
 * counterpart to [com.safeshield.app.vpn.VpnConnectionState].
 *
 * Unlike VPN state, nothing pushes updates here as a side effect of normal
 * operation; both [DeviceManagementViewModel] (on-demand refresh) and
 * [DeviceOwnerChangeReceiver] (passive detection of an external change —
 * PRD Phase 8: "detect management-state changes") write to this, so every
 * reader sees one consistent value regardless of which of them noticed
 * last.
 */
object DeviceManagementState {
    private val _state = MutableStateFlow(ManagementState.NOT_MANAGED)
    val state: StateFlow<ManagementState> = _state.asStateFlow()

    internal fun update(newState: ManagementState) {
        _state.value = newState
    }
}
