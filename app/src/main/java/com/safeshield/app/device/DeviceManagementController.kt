package com.safeshield.app.device

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent

/**
 * The only place in the app that talks to [DevicePolicyManager] directly.
 * Everything here uses official, documented Android APIs — there is no
 * path in this class that acquires Device Owner silently or without the
 * user explicitly completing Android's own system provisioning UI.
 *
 * Platform reality this class does not paper over (PRD §5): calling
 * [deviceOwnerProvisioningIntent] does not guarantee Device Owner status.
 * `ACTION_PROVISION_MANAGED_DEVICE` only succeeds on a device in a
 * provisionable state — in practice, one with no user accounts added yet,
 * which usually means right after a factory reset, during initial setup,
 * or via `adb shell dpm set-device-owner` with no accounts present. On an
 * already-set-up device (the common case for someone installing SafeShield
 * later), Android itself will refuse and show its own explanation — this
 * class does not attempt to detect that in advance or work around it, and
 * neither should it. See DEVICE_OWNER_PROVISIONING.md.
 */
class DeviceManagementController(private val context: Context) {

    private val devicePolicyManager: DevicePolicyManager =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

    private val adminComponent = ComponentName(context, SafeShieldDeviceAdminReceiver::class.java)

    fun currentState(): ManagementState = when {
        devicePolicyManager.isDeviceOwnerApp(context.packageName) -> ManagementState.DEVICE_OWNER
        devicePolicyManager.isProfileOwnerApp(context.packageName) -> ManagementState.MANAGED_PROFILE
        devicePolicyManager.isAdminActive(adminComponent) -> ManagementState.DEVICE_ADMIN
        else -> ManagementState.NOT_MANAGED
    }

    /** The official Android Device Owner provisioning flow. See the class doc for why this can fail on a normal, already-set-up device. */
    fun deviceOwnerProvisioningIntent(): Intent =
        Intent(DevicePolicyManager.ACTION_PROVISION_MANAGED_DEVICE)
            .putExtra(DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME, adminComponent)

    /**
     * The legacy Device Admin activation flow — works on any already
     * set-up device, unlike Device Owner provisioning, but is honestly a
     * much weaker tier: it cannot block uninstalling SafeShield or enforce
     * user restrictions, only the narrow legacy policy set declared in
     * res/xml/device_admin.xml (currently none). Exposed because the PRD
     * explicitly asks the UI to distinguish and support this tier, not
     * because it materially strengthens protection on its own.
     */
    fun legacyDeviceAdminIntent(explanation: String): Intent =
        Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
            .putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, explanation)

    /** Only removes legacy Device Admin — a Device Owner cannot remove itself this way, matching "no silent state changes" in either direction. */
    fun removeLegacyDeviceAdmin() {
        if (devicePolicyManager.isAdminActive(adminComponent) && !devicePolicyManager.isDeviceOwnerApp(context.packageName)) {
            devicePolicyManager.removeActiveAdmin(adminComponent)
        }
    }

    /** Short, accurate description of what's actually enforceable at [state] — shown in Settings > Device Management. */
    fun describeSupportedPolicies(state: ManagementState): String = when (state) {
        ManagementState.NOT_MANAGED ->
            "No device management is active. Protection relies solely on the VPN and can be turned off or uninstalled like any other app."
        ManagementState.DEVICE_ADMIN ->
            "Legacy Device Admin is active. This does not currently enforce any additional policy, and cannot block uninstalling SafeShield — it exists as a recognized state, not a stronger protection tier."
        ManagementState.DEVICE_OWNER ->
            "Device Owner is active. Where supported by this device and Android version, SafeShield can block uninstallation and enforce protection-related restrictions (see Phase 7)."
        ManagementState.MANAGED_PROFILE ->
            "SafeShield is the profile owner of a managed profile. Enforcement applies within that profile only, not the whole device."
    }
}
