package com.safeshield.app.vpn

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Mirrors the Home screen's VPN status line — see PRD Phase 1/14 UI spec. */
enum class VpnState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

/**
 * Process-wide, observable VPN connection state. [SafeShieldVpnService] is
 * the only writer; the UI (from Phase 5 onward) and any other component
 * that needs to know "is protection actually running right now" reads
 * [state] rather than trying to query the service directly.
 *
 * A plain object is deliberate here: there is exactly one VPN connection
 * per device, Android itself enforces that (starting SafeShield's VPN tears
 * down any other app's), so a singleton matches the real constraint rather
 * than pretending multiple instances could exist.
 */
object VpnConnectionState {
    private val _state = MutableStateFlow(VpnState.DISCONNECTED)
    val state: StateFlow<VpnState> = _state.asStateFlow()

    internal fun update(newState: VpnState) {
        _state.value = newState
    }
}
