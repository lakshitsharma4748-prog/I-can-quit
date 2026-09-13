package com.safeshield.app.device

/**
 * The device-management tiers the PRD requires SafeShield to distinguish
 * and display accurately (§2, §4). Ordered roughly weakest to strongest —
 * only [DEVICE_OWNER] (or [MANAGED_PROFILE] on a work-profile device)
 * unlocks the uninstall-blocking/user-restriction APIs Phase 7 uses.
 */
enum class ManagementState {
    NOT_MANAGED,
    DEVICE_ADMIN,
    DEVICE_OWNER,
    MANAGED_PROFILE
}

/** The exact status wording the PRD's Home/Settings mockups use for each state. */
fun ManagementState.displayLabel(): String = when (this) {
    ManagementState.NOT_MANAGED -> "NOT CONFIGURED"
    ManagementState.DEVICE_ADMIN -> "DEVICE ADMIN"
    ManagementState.DEVICE_OWNER -> "DEVICE OWNER"
    ManagementState.MANAGED_PROFILE -> "MANAGED PROFILE"
}
