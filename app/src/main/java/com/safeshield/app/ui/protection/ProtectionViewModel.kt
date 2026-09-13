package com.safeshield.app.ui.protection

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.safeshield.app.database.AppDatabase
import com.safeshield.app.database.SafeShieldRepository
import com.safeshield.app.database.SettingsEntity
import com.safeshield.app.security.PinManager
import com.safeshield.app.vpn.SafeShieldVpnService
import com.safeshield.app.vpn.VpnConnectionState
import com.safeshield.app.vpn.VpnState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Backs the Home screen's protection controls (PRD Phase 5: "Standard
 * Protection"). Owns the one repository/PinManager instance the UI layer
 * needs for this — screens read [settings] and [vpnState] rather than
 * touching Room or [VpnConnectionState] directly.
 */
class ProtectionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SafeShieldRepository
    private val pinManager = PinManager(application)

    init {
        val database = AppDatabase.getInstance(application)
        repository = SafeShieldRepository(database.domainDao(), database.allowlistDao(), database.settingsDao())
    }

    val settings: StateFlow<SettingsEntity> = repository.settings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        SettingsEntity()
    )

    /** [VpnConnectionState] is process-wide (there is only ever one VPN connection); exposed here so the UI has one place to read protection-related state from. */
    val vpnState: StateFlow<VpnState> = VpnConnectionState.state

    /**
     * Starts VPN filtering and marks protection enabled. Assumes VPN
     * consent (`VpnService.prepare`) has already been granted — that has to
     * happen at the Activity layer (it needs an ActivityResultLauncher),
     * before this is called.
     */
    fun activateProtection() {
        viewModelScope.launch {
            repository.setProtectionEnabled(true)
            SafeShieldVpnService.start(getApplication())
        }
    }

    /** True if disabling protection should be gated behind [verifyPinToDisable] first. */
    fun isPinRequiredToDisable(): Boolean = pinManager.isPinSet()

    suspend fun verifyPinToDisable(pin: String): Boolean = withContext(Dispatchers.IO) {
        pinManager.verifyPin(pin) is PinManager.VerifyResult.Success
    }

    fun deactivateProtection() {
        viewModelScope.launch {
            SafeShieldVpnService.stop(getApplication())
            repository.setProtectionEnabled(false)
        }
    }
}
