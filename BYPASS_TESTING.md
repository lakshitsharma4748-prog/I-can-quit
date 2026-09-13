# Bypass testing — scenarios, expected behavior, and known gaps

PRD Phase 9 asks SafeShield to test and handle a list of common network
scenarios, using only legitimate Android APIs — never exploits, never
attempts to defeat Android's own security model. This document is the
result of that pass: for each scenario, what SafeShield does today, and
what's honestly still a gap.

**This could not be executed as live testing in the sandbox this project
was built in** — there's no Android device, emulator, or real network
there (see README's "Note on this development environment"). Everything
below is a code-level analysis of what the current DNS-only VPN
architecture ([`vpn/SafeShieldVpnService.kt`](app/src/main/java/com/safeshield/app/vpn/SafeShieldVpnService.kt))
does and doesn't cover, plus a manual checklist for testing it for real.
Treat every "should work" below as a prediction to verify, not a
confirmed result.

## How SafeShield's VPN works, briefly

It's a **DNS-only** VPN: the `VpnService.Builder` routes only its two
virtual DNS addresses (one IPv4, one IPv6 — see Phase 9's addition below)
into the TUN interface. Every other destination continues over the
device's normal network path, untouched. This keeps the implementation
small and correct, at the cost of only being able to filter/see plain DNS
lookups — not arbitrary traffic.

## Scenario-by-scenario

| Scenario | Expected behavior | Status |
|---|---|---|
| Wi-Fi change | `registerDefaultNetworkCallback` + `setUnderlyingNetworks` (added this phase) tells the VPN which network is actually active as it changes, so Android doesn't have to guess | Should work — needs real-device verification |
| Mobile data | Same mechanism as Wi-Fi change | Should work — needs real-device verification |
| Switching networks | Same mechanism | Should work — needs real-device verification |
| Device reboot | `BootCompletedReceiver` (Phase 8) restarts the VPN if protection was on and consent is still valid | Should work — needs real-device verification |
| VPN reconnect | `SafeShieldVpnService.onRevoke()` / packet-loop IOException both tear down cleanly and report `VpnState.ERROR`; nothing currently *automatically* re-establishes after an error (the user or a future watchdog would need to) | Partial — detection works, automatic reconnect after an error does not exist yet |
| DNS configuration changes | The VPN forces its own DNS server address via `addDnsServer`, which Android uses for apps under this VPN regardless of the device's configured DNS | Should work — needs real-device verification |
| Private DNS (DNS-over-TLS) | **Known gap.** Android's Private DNS feature connects directly to the configured resolver's real IP over port 853, encrypted — that traffic never matches this VPN's narrow DNS-only routes (only the two virtual DNS addresses are routed in), so it bypasses filtering entirely today | **Not handled** — see below |
| Multiple browsers | Filtering happens at the DNS layer below any individual app, so it's inherently browser-agnostic — no per-app logic exists to bypass | Should work — needs real-device verification |
| IPv4 | Handled since Phase 2 | Implemented |
| IPv6 | Handled since Phase 9 (this phase) — `PacketProcessor` now parses IPv6/UDP packets the same way as IPv4, and the VPN adds an IPv6 virtual DNS address/route alongside the IPv4 one | Implemented — needs real-device verification |
| DoH (DNS-over-HTTPS) | **Known gap**, same root cause as Private DNS: DoH traffic is a normal-looking HTTPS connection (port 443) to the resolver's real IP, indistinguishable from other HTTPS traffic without TLS/SNI inspection this VPN doesn't do | **Not handled** — see below |
| DoT (DNS-over-TLS) | Same as "Private DNS" above (Private DNS *is* DoT) | **Not handled** — see below |

## The real gap: Private DNS / DoH / DoT

This is the one honest limitation worth calling out clearly rather than
burying in a table cell: **a user (or an app) that enables Private DNS, or
that uses an app with DNS-over-HTTPS built in (several modern browsers
support this directly), bypasses SafeShield's filtering entirely**, because
that traffic never enters the narrow DNS-only tunnel this VPN establishes.

Closing this gap for real would require a fundamentally different
architecture — a **full-tunnel VPN** that routes *all* device traffic
(`0.0.0.0/0` and `::/0`) through the TUN interface, plus logic to either:

- Block outbound connections to port 853 (DoT's standard port) that aren't
  to SafeShield's own resolver, forcing most devices' "Private DNS:
  Automatic" setting to fail open back to plain DNS, or
- Inspect TLS ClientHello SNI fields to identify and block known DoH
  provider hostnames.

Both are substantially more complex than a DNS-only VPN (essentially a
full NAT/packet-forwarding engine for arbitrary TCP/UDP traffic, not just
UDP/53), carry real risk of breaking unrelated connectivity if implemented
hastily, and are not part of this repository. This is flagged here rather
than attempted half-built, per the PRD's instruction to document
limitations discovered during testing rather than overstate what's
achieved.

## Manual test checklist (for a real device/emulator)

Once built (see README for the network-access caveat on building this
project), a human tester with a physical device should verify:

1. Enable Standard Protection. Confirm `blocked-example.test` and
   `adult-example.test` fail to resolve (e.g. `ping blocked-example.test`
   from a terminal app, or via a browser), while a normal domain resolves
   fine.
2. Toggle Wi-Fi off/on and confirm filtering keeps working without needing
   to reopen the app.
3. Switch from Wi-Fi to mobile data (and back) mid-session; confirm no
   interruption.
4. Reboot the device with protection on; confirm the VPN indicator
   reappears without opening SafeShield.
5. Enable Private DNS (Settings → Network → Private DNS) and confirm the
   test domains **do** resolve (this is the known, documented gap above —
   confirming it reproduces is still useful to track for a future phase).
6. Repeat step 1 on both an IPv4-only and an IPv6-capable network, if
   available.
7. Try at least two different browsers/apps and confirm filtering applies
   to both (it should, since filtering is DNS-layer, not per-app).
