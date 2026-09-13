# SafeShield

SafeShield is a **consent-based** Android application that helps users block
adult and explicit websites across their device. It offers two protection
modes:

1. **Standard Protection** — VPN-based DNS/domain filtering, protected by an
   administrator PIN.
2. **Strong Protection** — legitimate Android managed-device / Device Owner
   provisioning (where supported by the device and Android version),
   combined with VPN filtering and supported device-management policies.

SafeShield never secretly obtains device control, never bypasses Android
security, and never monitors users without their explicit consent. It only
uses official Android APIs (`VpnService`, `DevicePolicyManager`, Android's
own managed-device provisioning flow) and never uses exploits, hidden
`AccessibilityService` tricks, or undocumented privilege escalation.

> **Platform limitation, stated up front:** a normal Android app cannot
> guarantee that a user can never uninstall, disable, or bypass it on every
> device. Strong Protection raises the bar using legitimate managed-device /
> Device Owner provisioning, but this remains subject to Android, device
> manufacturer, and OS-version limitations. SafeShield does not claim
> "100% unbypassable" protection anywhere in the product.

## Project status

This project is being built incrementally, phase by phase, per the product
requirements document.

| Phase | Description | Status |
|---|---|---|
| 0 | Repository setup | ✅ Done |
| 1 | Basic UI (onboarding, home, settings, admin, about) | ✅ Done |
| 2 | VPN MVP (local test blocklist) | ✅ Done |
| 3 | Room database (domains, allowlist, settings) | ✅ Done |
| 4 | Administrator PIN | ✅ Done |
| 5 | Standard Protection | ✅ Done |
| 6 | Strong Protection consent & Device Owner flow | ✅ Done |
| 7 | Strong Protection policies | ✅ Done |
| 8 | Anti-tampering | ✅ Done |
| 9 | Bypass/network-change testing (IPv6, network resilience) | ✅ Done — see [BYPASS_TESTING.md](BYPASS_TESTING.md) |
| 10–11 | Railway backend + configuration | ✅ Done — see [backend/README.md](backend/README.md) |
| 12 | Blocklist updates (WorkManager) | ✅ Done |
| 13 | Privacy documentation | ✅ Done — see [PRIVACY.md](PRIVACY.md) |
| 14 | Full Material 3 UI polish | Planned |
| 15 | Blocked-website experience | Planned |
| 16 | Testing | Planned |
| 17–19 | Build verification, release prep, final acceptance | Planned |

See [DEVICE_OWNER_PROVISIONING.md](DEVICE_OWNER_PROVISIONING.md) for what
Strong Protection's Device Owner step actually requires on a real device,
and [BYPASS_TESTING.md](BYPASS_TESTING.md) for the VPN's known filtering
gaps (Private DNS/DoH/DoT) and a manual test checklist.

## Architecture

The Android app (module `app/`) is organized by responsibility so that UI,
filtering, storage, security, and device-management code stay decoupled:

```
app/src/main/java/com/safeshield/app/
├── ui/
│   ├── onboarding/   — first-run explanation & mode selection
│   ├── home/         — protection/VPN/device-management status
│   ├── protection/   — standard/strong protection setup flows
│   ├── settings/     — blocklist, allowlist, security settings
│   ├── admin/        — administrator PIN screens
│   └── blocked/      — "website blocked" experience
├── vpn/              — SafeShieldVpnService, DNS resolver, domain filter
├── database/         — Room: AppDatabase, DomainDao, SettingsDao
├── security/         — PinManager, SecureStorage (Android Keystore)
├── device/           — DevicePolicyManager wrapper, Device Owner detection
├── updater/          — WorkManager-based blocklist synchronization
└── common/           — shared utilities
```

A `backend/` directory (Node.js/TypeScript/Express, deployed to Railway)
serves blocklist/version data over HTTPS — see
[backend/README.md](backend/README.md) for its endpoints, local dev, and
Railway deployment steps. The VPN itself works entirely offline without
it; the backend only feeds the Phase 12 blocklist-sync job.

## Requirements

- Android Studio (Koala or newer) or a Gradle 8.7 + JDK 17 toolchain.
- Android SDK Platform 34, Build-Tools matching AGP 8.5.2.
- A physical device or emulator running Android 8.0 (API 26) or later.
  Device Owner provisioning (Phase 6+) is easiest to test on an emulator or
  a device that has never been signed into a Google account, or via
  `adb shell dpm set-device-owner`.

## Building

```bash
./gradlew build
./gradlew testDebugUnitTest
```

> **Note on this development environment:** this repository was scaffolded
> inside a sandboxed CI-like environment whose network policy blocks
> `dl.google.com` (Google's Maven repository, `google()` in Gradle). That
> host serves the Android Gradle Plugin, Android SDK platforms/build-tools,
> and all AndroidX/Compose artifacts, so `./gradlew build` **could not be
> executed to completion in that environment** — it fails at dependency
> resolution, not due to a code or configuration defect. The project
> structure, Gradle files, and Kotlin/Compose sources were written by hand
> against known-good, mutually compatible versions (AGP 8.5.2, Gradle 8.7,
> Kotlin 2.0.20, Compose BOM 2024.09.03) and reviewed for correctness. On a
> machine or CI runner with normal access to `dl.google.com` /
> `maven.google.com`, `./gradlew build` is expected to succeed. Please run
> the build there (or in Android Studio) and report back if anything needs
> adjustment.

## Privacy

By default, SafeShield does not store browsing history, upload visited
URLs, collect search history, read private messages, record screen
contents, or otherwise monitor the user. Only the minimum information
needed for blocklist/version management is sent to the backend. See
[PRIVACY.md](PRIVACY.md) for the full policy (also linked from the app's
Settings → About & Privacy screen), matched point-by-point against what
the code actually does.

## License

Not yet decided.
