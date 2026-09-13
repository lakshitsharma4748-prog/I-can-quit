# Architecture

This documents the actual current structure of the codebase — updated as
of Phase 18, reflecting everything built across Phases 0–17, not the
original Phase 0 skeleton (see README.md's phase table for what landed
when).

## Android app (`app/`)

```
app/src/main/java/com/safeshield/app/
├── MainActivity.kt              — hosts the Compose nav graph, nothing else
├── SafeShieldApplication.kt     — schedules the Phase 12 blocklist sync at process start
│
├── ui/
│   ├── navigation/
│   │   ├── Routes.kt            — every destination as a string constant
│   │   └── SafeShieldNavHost.kt — the entire nav graph + cross-cutting
│   │                              wiring (VPN consent flow, Device Owner
│   │                              provisioning sequencing, blocked-event
│   │                              collection)
│   ├── onboarding/               — first-run: Welcome, Explain
│   ├── home/                     — HomeScreen (all four status rows, live)
│   ├── protection/                — ProtectionModeScreen,
│   │                                StrongProtectionConsentScreen,
│   │                                ProtectionViewModel (starts/stops the VPN)
│   ├── admin/                     — AdminSecurityScreen + ViewModel (PIN set/change)
│   ├── settings/                  — SettingsScreen (hub) + DeviceManagementScreen,
│   │                                BlocklistScreen (+ViewModel), AboutPrivacyScreen
│   ├── blocked/                   — BlockedScreen (Phase 15, foreground-only case)
│   ├── components/                — StatusRow/StatusCard, SafeShieldTopBar,
│   │                                PinConfirmDialog, ComingSoonNotice — shared
│   │                                across the screens above
│   ├── theme/                     — Material 3 theme (Color/Type/Theme.kt)
│   └── common/                    — reserved, currently empty; no shared
│                                     non-UI utility has been needed yet
│
├── vpn/
│   ├── SafeShieldVpnService.kt   — the VpnService: TUN lifecycle, packet
│   │                                loop, foreground notification, network-
│   │                                change resilience, blocked-notification
│   ├── PacketProcessor.kt        — IPv4/IPv6 + UDP parsing, reply-packet
│   │                                construction (pure, unit-tested)
│   ├── dns/DnsMessage.kt          — DNS question parsing + NXDOMAIN builder (pure)
│   ├── Checksums.kt               — RFC 1071 Internet checksum (pure)
│   ├── DomainFilter.kt            — in-memory block/allow decision (pure)
│   ├── DnsResolver.kt             — forwards allowed queries upstream
│   ├── VpnConnectionState.kt      — process-wide observable VpnState
│   ├── BlockedEventBus.kt         — process-wide "a block just happened" signal
│   ├── NotificationDebouncer.kt   — throttles the blocked-website notification (pure)
│   └── BootCompletedReceiver.kt   — restarts the VPN after reboot if it was on
│
├── database/
│   ├── AppDatabase.kt             — Room database (domains, allowlist, settings)
│   ├── DomainEntity.kt / DomainDao.kt
│   ├── AllowlistEntity.kt / AllowlistDao.kt
│   ├── SettingsEntity.kt / SettingsDao.kt
│   └── SafeShieldRepository.kt    — the one thing everything else talks to;
│                                     never call a DAO directly outside this file
│
├── security/
│   ├── PinHasher.kt               — PBKDF2 + constant-time comparison (pure)
│   ├── RateLimiter.kt             — lockout backoff math (pure)
│   ├── SecureStorage.kt           — Keystore-backed EncryptedSharedPreferences
│   └── PinManager.kt              — ties the above together; set/verify/change PIN
│
├── device/
│   ├── ManagementState.kt         — the four states (+ display labels)
│   ├── SafeShieldDeviceAdminReceiver.kt — required admin component
│   ├── DeviceManagementController.kt    — the only class that calls
│   │                                       DevicePolicyManager directly
│   ├── DeviceManagementState.kt   — process-wide observable ManagementState
│   ├── DeviceManagementViewModel.kt
│   └── DeviceOwnerChangeReceiver.kt — passive detection via Android's own broadcast
│
└── updater/
    ├── BlocklistDtos.kt            — kotlinx.serialization DTOs matching the backend
    ├── BlocklistValidator.kt       — parse + validate (pure, fully unit-tested)
    ├── BlocklistFreshness.kt       — NOT_LOADED/UP_TO_DATE/STALE classification (pure)
    ├── BlocklistUpdater.kt         — the CoroutineWorker (HTTPS fetch, validate, apply)
    └── BlocklistSyncScheduler.kt   — periodic + manual WorkManager scheduling
```

### Design rules this codebase actually follows (not aspirational)

- **Pure logic is separated from Android glue, deliberately, everywhere.**
  Every file above marked "(pure)" has zero Android SDK imports and a full
  JVM unit test suite — this wasn't incidental, it's why e.g. `DomainFilter`,
  `PacketProcessor`, `PinHasher`, `RateLimiter`, `BlocklistValidator`, and
  `NotificationDebouncer` are separate classes from the services/managers
  that call them. See TESTING.md for why this matters in an environment
  where the Android build itself can't run.
- **One repository, no DAOs called from outside it.** `SafeShieldRepository`
  is the only thing `SafeShieldVpnService`, `BlocklistUpdater`, and every
  ViewModel talk to for persisted data.
- **Two process-wide state holders** (`VpnConnectionState`,
  `DeviceManagementState`) exist because there's exactly one VPN connection
  and one device-management status per device — a singleton object matches
  that real constraint rather than pretending multiple instances could
  exist.
- **Nothing acquires elevated privilege silently.** VPN consent and Device
  Owner provisioning both route through Android's own system UI, launched
  from `SafeShieldNavHost`, never from a background path.

## Backend (`backend/`)

See [backend/README.md](backend/README.md) for the full breakdown — in
short: `app.ts` (Express app, testable without a bound port) +
`server.ts` (the actual listener), `routes/` → `controllers/` →
`services/`, one bundled JSON data file, no database (see
`backend/src/database/README.md` for why).

## Everything else at the repo root

| File | What it's for |
|---|---|
| `README.md` | Project overview, phase status, build/run instructions |
| `ARCHITECTURE.md` | This file |
| `TESTING.md` | Every test mapped against the PRD's Phase 16 checklist |
| `PRIVACY.md` | The formal privacy policy, matched against the code |
| `BYPASS_TESTING.md` | Network/bypass scenario analysis + manual test checklist |
| `DEVICE_OWNER_PROVISIONING.md` | What Device Owner provisioning actually requires |
| `RELEASE.md` | How to build a signed release APK/AAB |
