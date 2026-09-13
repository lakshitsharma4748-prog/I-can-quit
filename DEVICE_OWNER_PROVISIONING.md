# Device Owner provisioning — what it takes and what it doesn't

SafeShield's "Strong Protection" uses Android's official Device Owner
mechanism ([`android/device/DeviceManagementController.kt`](app/src/main/java/com/safeshield/app/device/DeviceManagementController.kt))
to make protection harder to disable or uninstall. This document explains
the real-world constraints on that — the PRD explicitly requires SafeShield
never to overstate what it can guarantee here, and this is where that
matters most.

## The short version

**Device Owner provisioning only works on a device with no accounts set
up yet.** On a phone you're already using day-to-day, tapping "Enable
Strong Protection" will launch Android's own provisioning screen, and
Android itself will refuse with its own explanation — SafeShield can't
change that, and doesn't try to.

## Why

`DevicePolicyManager.ACTION_PROVISION_MANAGED_DEVICE` — the API SafeShield
calls — is Android's official Device Owner provisioning flow, but Google
restricts *when* it can succeed: the device must be in a state comparable
to fresh-out-of-the-box (no Google account signed in, no other accounts
added). This is an Android platform policy, not a SafeShield limitation,
and SafeShield will not attempt to work around it — doing so would mean
either exploiting the platform or acquiring Device Owner status silently,
both of which the PRD explicitly forbids.

## How to actually provision a device as Device Owner (for testing)

Pick one:

1. **Factory reset + QR code / NFC enrollment.** The standard production
   path (used by MDM vendors): factory-reset the device, and during setup,
   use Android's "tap 6 times" + QR code flow (or NFC bump) pointing at a
   provisioning payload that names SafeShield's package
   (`com.safeshield.app`) and admin component
   (`com.safeshield.app.device.SafeShieldDeviceAdminReceiver`). This is the
   realistic path for an actual deployment, but requires building and
   hosting a provisioning QR payload, which is out of scope for this
   repository (no backend infrastructure for it exists, and one isn't
   requested by the PRD).

2. **`adb shell dpm set-device-owner` (recommended for development/testing).**
   On an emulator or a test device with no accounts added (a fresh AVD, or
   a real device wiped and set up with "Skip" at every account prompt):

   ```bash
   adb install app-debug.apk
   adb shell dpm set-device-owner com.safeshield.app/.device.SafeShieldDeviceAdminReceiver
   ```

   This bypasses only the *UI* of provisioning (no QR code needed) — it is
   still Android's own official `dpm` tool performing a real, recognized
   Device Owner assignment; SafeShield does nothing differently afterward
   than it would after UI-based provisioning. If the command fails with
   "not allowed" or similar, the device has an account on it — remove all
   accounts (Settings → Accounts) and retry, or wipe and start over.

3. **A fresh emulator (AVD) with no Google Play/account setup.** The
   easiest repeatable option for local development: create an AVD using a
   non-Play (AOSP) system image, boot it, and use option 2 above
   immediately — a brand-new AVD has no accounts by default.

## What Device Owner actually unlocks in this app

See [`device/DeviceManagementController.describeSupportedPolicies`](app/src/main/java/com/safeshield/app/device/DeviceManagementController.kt)
and Phase 7 for the specific policies applied (`setUninstallBlocked`,
`addUserRestriction`, etc.) — all standard, documented `DevicePolicyManager`
APIs gated behind `isDeviceOwnerApp()`, applied only once Device Owner is
confirmed, never assumed.

## What SafeShield will never do here

- Attempt Device Owner acquisition without you completing Android's own
  consent/provisioning UI (or explicitly running the `adb` command
  yourself).
- Claim Device Owner is active without checking
  `DevicePolicyManager.isDeviceOwnerApp()` first.
- Promise that Strong Protection makes SafeShield impossible to remove.
  Even as Device Owner, a factory reset, `adb shell dpm remove-active-admin`
  (before restrictions block it), or recovery-mode wipe still remove it —
  Android does not offer any app a way to prevent that, nor should it.
