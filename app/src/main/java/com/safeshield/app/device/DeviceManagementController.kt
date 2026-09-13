package com.safeshield.app.device

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.UserManager
import android.util.Log

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
            "Device Owner is active. The specific policies below are applied where this device and Android version support them."
        ManagementState.MANAGED_PROFILE ->
            "SafeShield is the profile owner of a managed profile. Enforcement applies within that profile only, not the whole device."
    }

    /** One policy this class tried to apply, and whether it actually took — never assumed. */
    data class PolicyOutcome(val label: String, val applied: Boolean, val failureReason: String? = null)

    /**
     * Applies the Device Owner policies Strong Protection relies on
     * (PRD Phase 7). Every call is independent and wrapped so one
     * unsupported policy (a manufacturer restriction, an older Android
     * version lacking a given `UserManager` constant's real effect, etc.)
     * never prevents the others from being attempted — this always returns
     * a result for every policy it tried, applied or not, rather than
     * throwing partway through.
     *
     * No-op (returns every outcome as not-applied, reason "not Device
     * Owner") unless [currentState] is actually [ManagementState.DEVICE_OWNER]
     * — this method itself never elevates or assumes management state.
     */
    fun applyStrongProtectionPolicies(): List<PolicyOutcome> {
        val policies = listOf(
            "Block uninstalling SafeShield" to { devicePolicyManager.setUninstallBlocked(adminComponent, context.packageName, true) },
            "Prevent reconfiguring VPN settings" to { devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_CONFIG_VPN) },
            "Block booting into Safe Mode (disables third-party apps, including this one)" to
                { devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_SAFE_BOOT) },
            "Block factory reset from Settings" to { devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_FACTORY_RESET) }
        )

        if (currentState() != ManagementState.DEVICE_OWNER) {
            return policies.map { (label, _) -> PolicyOutcome(label, applied = false, failureReason = "Not Device Owner") }
        }

        return policies.map { (label, apply) ->
            try {
                apply()
                PolicyOutcome(label, applied = true)
            } catch (e: SecurityException) {
                Log.w(TAG, "Policy not applied ($label): ${e.javaClass.simpleName}")
                PolicyOutcome(label, applied = false, failureReason = e.message ?: e.javaClass.simpleName)
            } catch (e: IllegalStateException) {
                Log.w(TAG, "Policy not applied ($label): ${e.javaClass.simpleName}")
                PolicyOutcome(label, applied = false, failureReason = e.message ?: e.javaClass.simpleName)
            }
        }
    }

    /**
     * Reverts everything [applyStrongProtectionPolicies] can revert, for
     * if the user later chooses to step back down from Strong Protection.
     * Not currently reachable from the UI (no such downgrade flow exists
     * yet) — provided so that capability isn't missing once one does.
     */
    fun revertStrongProtectionPolicies() {
        if (currentState() != ManagementState.DEVICE_OWNER) return
        runCatching { devicePolicyManager.setUninstallBlocked(adminComponent, context.packageName, false) }
        runCatching { devicePolicyManager.clearUserRestriction(adminComponent, UserManager.DISALLOW_CONFIG_VPN) }
        runCatching { devicePolicyManager.clearUserRestriction(adminComponent, UserManager.DISALLOW_SAFE_BOOT) }
        runCatching { devicePolicyManager.clearUserRestriction(adminComponent, UserManager.DISALLOW_FACTORY_RESET) }
    }

    companion object {
        private const val TAG = "SafeShield/DeviceMgmt"
    }
}
