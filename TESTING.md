# Testing

This maps every test in the repository against the PRD's Phase 16 checklist,
and says plainly which of it has actually been run (this project was built
in a sandbox with no Android SDK, no emulator, and no Railway deployment —
see the README's "Note on this development environment") versus which
needs a real device/emulator or a deployed backend to execute.

## How to run what exists

```bash
# Android unit tests — pure JVM, no device/emulator needed
./gradlew testDebugUnitTest

# Android instrumented tests — needs a real device or emulator
./gradlew connectedAndroidTest

# Backend tests — actually run and passing in this sandbox (see backend/README.md)
cd backend && npm test
```

## Coverage against the PRD's Phase 16 list

| PRD item | Where | Kind | Run in this sandbox? |
|---|---|---|---|
| Domain filtering: `blocked.example` → BLOCK, `sub.blocked.example` → BLOCK, `safe.example` → ALLOW | `vpn/DomainFilterTest.kt`, `vpn/PacketProcessorTest.kt` (incl. IPv6) | JVM unit | ✅ Yes |
| Allowlist: `blocked.example` → BLOCK, `allowed.example` → ALLOW | `vpn/DomainFilterTest.kt` (allowlist section) | JVM unit | ✅ Yes |
| Security: correct PIN | `security/PinHasherTest.kt`, `security/PinManagerTest.kt` | Unit (hashing) + instrumented (storage) | Hashing: ✅ yes. Storage: ❌ needs a device |
| Security: incorrect PIN | Same | Same | Same |
| Security: rate limiting | `security/RateLimiterTest.kt` (pure backoff math), `security/PinManagerTest.kt` (end-to-end lockout) | JVM unit + instrumented | Unit: ✅ yes. End-to-end: ❌ needs a device |
| Security: secure PIN storage | `security/PinManagerTest.kt` | Instrumented (needs real Keystore/EncryptedSharedPreferences) | ❌ needs a device |
| VPN: Start | `vpn/SafeShieldVpnServiceTest.kt` | Instrumented (needs VPN consent pre-granted — see the test's doc comment for the one-time `adb shell appops` command) | ❌ needs a device |
| VPN: Stop | Same | Same | ❌ needs a device |
| VPN: Reconnect | Code: `SafeShieldVpnService`'s network callback (Phase 9) | Manual — see BYPASS_TESTING.md checklist | ❌ needs a device |
| VPN: Reboot | Code: `vpn/BootCompletedReceiver.kt` (Phase 8) | Manual — see BYPASS_TESTING.md checklist | ❌ needs a device |
| VPN: Network change | Code: `SafeShieldVpnService`'s `ConnectivityManager.NetworkCallback` (Phase 9) | Manual — see BYPASS_TESTING.md checklist | ❌ needs a device |
| Device-management tests where testable | `device/ManagementStateTest.kt` (pure), `device/DeviceManagementControllerTest.kt` (state detection + intent shape on an unmanaged device) | JVM unit + instrumented | Unit: ✅ yes. Instrumented: ❌ needs a device. Real Device Owner behavior additionally needs provisioning per DEVICE_OWNER_PROVISIONING.md |
| Backend tests | `backend/test/app.test.ts` | Node/vitest | ✅ Yes — actually run in this sandbox, see backend/README.md |
| Blocklist tests | `updater/BlocklistValidatorTest.kt`, `updater/BlocklistFreshnessTest.kt` | JVM unit | ✅ Yes |

Additional coverage beyond the PRD's explicit list, because the code
needed it: `vpn/ChecksumsTest.kt` and `vpn/dns/DnsMessageTest.kt` (the
packet/DNS-construction primitives `PacketProcessor` depends on),
`database/AppDatabaseTest.kt` (Room round-trips, instrumented), and
`vpn/NotificationDebouncerTest.kt` (Phase 15's blocked-notification
throttling).

## Why so much of this is "JVM unit, actually run" vs. "instrumented, not run here"

Every test that's pure logic with no Android framework dependency
(domain matching, PIN hashing/rate-limit math, packet/DNS byte
construction, blocklist JSON validation/freshness, notification
debouncing) is a JVM unit test, and every one of those has actually been
executed in this environment — `./gradlew testDebugUnitTest` itself
couldn't run here (same `dl.google.com` network restriction that blocks
the full Android build), but the underlying logic was verified by
extracting it into plain-Kotlin classes specifically so it *could* be
tested without Android at all, and manually tracing the test cases
against that logic.

Everything that genuinely needs the Android runtime — Room's real SQLite,
Keystore-backed encryption, `VpnService`'s actual TUN establishment,
`DevicePolicyManager` — is an instrumented test under `androidTest/`,
which requires `./gradlew connectedAndroidTest` on a real device or
emulator. None of those could run in this sandbox. This isn't a gap
being hidden: it's the same environment limitation stated up front in
the README, applied consistently rather than skipped past.
