package com.safeshield.app.vpn

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.util.Log
import com.safeshield.app.database.AppDatabase
import com.safeshield.app.database.SafeShieldRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Restarts protection after a device reboot if it was on beforehand — PRD
 * Phase 8/9: without this, a reboot would silently leave the device
 * unprotected until someone happens to reopen SafeShield, which is exactly
 * the kind of "accidental" gap in protection Standard/Strong Protection are
 * supposed to close.
 *
 * Only starts the VPN when [VpnService.prepare] already returns null (i.e.
 * consent was granted in a previous session and Android still honors it) —
 * a broadcast receiver has no UI to ask for consent with, and this class
 * will not attempt to acquire it any other way. If consent is no longer
 * valid, protection stays off until the user reopens the app, same as if
 * they'd denied it in the first place.
 */
class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val appContext = context.applicationContext
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = AppDatabase.getInstance(appContext)
                val repository = SafeShieldRepository(database.domainDao(), database.allowlistDao(), database.settingsDao())
                val settings = repository.currentSettings()

                if (settings.protectionEnabled && VpnService.prepare(appContext) == null) {
                    Log.i(TAG, "Protection was enabled before reboot; restarting VPN")
                    SafeShieldVpnService.start(appContext)
                } else if (settings.protectionEnabled) {
                    Log.w(TAG, "Protection was enabled before reboot, but VPN consent is no longer valid; cannot restart without the user")
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "SafeShield/BootReceiver"
    }
}
