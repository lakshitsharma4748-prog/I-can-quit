package com.safeshield.app.vpn

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Process-wide "a DNS query was just blocked" signal (PRD Phase 15).
 * Carries only a timestamp — never the domain that was blocked. The
 * blocked-website experience only ever shows a fixed, category-level
 * explanation ("matches an adult-content category"), never the specific
 * site, so there's nothing else for this event to usefully carry.
 *
 * [SafeShieldVpnService] emits; the UI (foreground: an in-app screen) and
 * the notification path (background) both just react to "something was
 * blocked, when."
 */
object BlockedEventBus {
    private val _events = MutableSharedFlow<Long>(extraBufferCapacity = 8)
    val events: SharedFlow<Long> = _events.asSharedFlow()

    internal fun notifyBlocked(atEpochMillis: Long = System.currentTimeMillis()) {
        _events.tryEmit(atEpochMillis)
    }
}
