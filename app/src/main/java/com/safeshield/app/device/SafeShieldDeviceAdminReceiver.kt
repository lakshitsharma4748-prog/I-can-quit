package com.safeshield.app.device

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * The device admin component Device Owner provisioning (and legacy Device
 * Admin activation) registers against. Deliberately does almost nothing —
 * SafeShield's actual policy logic lives in [DeviceManagementController],
 * called from the UI/services that need it, not from receiver callbacks.
 */
class SafeShieldDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        Log.i(TAG, "Device admin enabled")
    }

    override fun onDisabled(context: Context, intent: Intent) {
        Log.i(TAG, "Device admin disabled")
    }

    companion object {
        private const val TAG = "SafeShield/DeviceAdmin"
    }
}
