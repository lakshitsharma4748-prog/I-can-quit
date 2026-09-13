# SafeShield Privacy Policy

*Last updated: this document is maintained alongside the app's source and
reflects the current codebase, not a separately-tracked legal document.*

This is the privacy policy referenced from the app (Settings → About &
Privacy) and required for app store listings for an app that requests VPN
and device-management permissions. It describes what the current, MVP
version of SafeShield does — see the root [README.md](README.md) for
overall project status.

## Summary

SafeShield does not know what websites you visit. It decides whether to
block a DNS lookup, and then forgets it ever happened. Nothing about your
browsing ever leaves your device, and nothing about it is written to disk
even locally.

## What SafeShield never does

- **Never stores browsing history.** The VPN service
  ([`vpn/SafeShieldVpnService.kt`](app/src/main/java/com/safeshield/app/vpn/SafeShieldVpnService.kt))
  inspects a DNS query's domain name only in memory, only long enough to
  check it against the blocklist, and never writes it to Room, a log file,
  or anywhere else persistent. Look at the code: there is no table, file,
  or log statement anywhere in the DNS-handling path that a domain name
  passes through (confirmed by inspection of every `Log.*` call in the
  `vpn/` package — none of them include a domain or query).
- **Never uploads visited URLs.** The only network request SafeShield's
  own app makes is `GET /api/blocklist` and `GET /api/version` to its own
  backend (see [backend/README.md](backend/README.md)) — both take no
  parameters describing what you've browsed, and both return the same
  public data to every device that asks.
- **Never collects search history.**
- **Never reads private messages.** SafeShield has no permission that
  would let it, and no code path that attempts to.
- **Never records your screen.**
- **Never uses `AccessibilityService`** as a way to observe what you do
  in other apps — it isn't declared, requested, or used anywhere in this
  project.
- **Never monitors you secretly.** Every protection mode requires
  explicit, in-app consent before anything starts (see the onboarding and
  consent screens in `ui/onboarding/` and `ui/protection/`), and Strong
  Protection's device-management step additionally requires you to
  complete Android's own system consent UI — SafeShield cannot silently
  acquire Device Owner status (see
  [DEVICE_OWNER_PROVISIONING.md](DEVICE_OWNER_PROVISIONING.md)).

## What SafeShield does collect/store, and why

| Data | Where | Why | Leaves the device? |
|---|---|---|---|
| The blocklist itself (domains + categories) | Room, `domains` table | To decide BLOCK/ALLOW without a network request per lookup | No — downloaded from the backend, never uploaded |
| Your allowlist entries | Room, `allowlist` table | Domains you've chosen to always allow | No |
| Settings (protection on/off, adult-content filtering on/off, last sync time) | Room, `settings` table | So the app remembers your choices between launches | No |
| Administrator PIN | Android Keystore-backed encrypted storage (never plaintext — see [`security/PinManager.kt`](app/src/main/java/com/safeshield/app/security/PinManager.kt)) | Protects settings changes | No |

None of this is browsing activity — it's the app's own configuration,
stored the same way any app stores its settings.

## What the backend receives

SafeShield's backend ([backend/](backend)) serves `/health`,
`/api/blocklist`, and `/api/version` — three public, unauthenticated,
parameter-free endpoints. Standard HTTP server logs record method, path,
status code, and response time for these requests (ordinary web server
operational logging, the same as almost any web service), and nothing
about which domains you're blocking or visiting. See
[backend/README.md](backend/README.md#privacy) for detail.

## Platform limitations (stated plainly, not buried)

SafeShield cannot guarantee it can never be uninstalled, disabled, or
bypassed — see the README and [BYPASS_TESTING.md](BYPASS_TESTING.md) for
the specific, honest gaps (Private DNS and DNS-over-HTTPS currently bypass
this VPN's filtering; a factory reset or root access can remove or tamper
with the app regardless of Device Owner status). None of these are
privacy problems — they're the app failing to filter, not the app leaking
data — but they're documented here too because a security or privacy
policy that hides its own limitations isn't one you can trust.

## Contact / changes

This being an in-development project without a published listing yet,
there's no separate legal contact channel — questions belong as issues
against this repository. This document will be revised as later phases
(e.g. a real curated blocklist, any future account system) change what's
actually true; it's written to match the code, not the other way around.
