package com.safeshield.app.device

import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Passive half of "detect management-state changes" (PRD Phase 8).
 * `DevicePolicyManager.ACTION_DEVICE_OWNER_CHANGED` fires whenever Device
 * Owner is set *or* removed for any app, by any means Android recognizes
 * (including someone running `adb shell dpm remove-active-admin`, or an
 * MDM console revoking it remotely) — not just from actions this app took
 * itself. Re-checking here means the UI's [DeviceManagementState] can't go
 * stale just because nobody happened to open Settings > Device Management
 * afterward.
 */
class DeviceOwnerChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != DevicePolicyManager.ACTION_DEVICE_OWNER_CHANGED) return

        val controller = DeviceManagementController(context.applicationContext)
        val newState = controller.currentState()
        Log.i(TAG, "Device Owner state changed externally; now $newState")
        DeviceManagementState.update(newState)

        if (newState == ManagementState.DEVICE_OWNER) {
            // Re-assert Phase 7's policies rather than assuming whatever
            // granted Device Owner also configured them the way SafeShield
            // needs.
            controller.applyStrongProtectionPolicies()
        }
    }

    companion object {
        private const val TAG = "SafeShield/DeviceOwnerChange"
    }
}
