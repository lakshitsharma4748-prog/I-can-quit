package com.safeshield.app.ui.navigation

/**
 * All navigation destinations in the app. Kept as plain string constants
 * (rather than a sealed class with arguments) since no screen currently
 * takes navigation arguments — revisit if/when one needs to (e.g. an
 * allowlist entry id in a later phase).
 */
object Routes {
    const val ONBOARDING_WELCOME = "onboarding/welcome"
    const val ONBOARDING_EXPLAIN = "onboarding/explain"
    const val PROTECTION_MODE_SELECT = "protection/mode-select"
    const val STRONG_PROTECTION_CONSENT = "protection/strong-consent"
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val SETTINGS_SECURITY = "settings/security"
    const val SETTINGS_ABOUT_PRIVACY = "settings/about-privacy"
    const val SETTINGS_DEVICE_MANAGEMENT = "settings/device-management"
    const val SETTINGS_BLOCKLIST = "settings/blocklist"
}
