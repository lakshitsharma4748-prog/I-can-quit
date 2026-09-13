package com.safeshield.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.safeshield.app.ui.admin.AdminSecurityScreen
import com.safeshield.app.ui.home.HomeScreen
import com.safeshield.app.ui.onboarding.OnboardingExplainScreen
import com.safeshield.app.ui.onboarding.OnboardingWelcomeScreen
import com.safeshield.app.ui.protection.ProtectionModeScreen
import com.safeshield.app.ui.protection.StrongProtectionConsentScreen
import com.safeshield.app.ui.settings.AboutPrivacyScreen
import com.safeshield.app.ui.settings.SettingsScreen

/**
 * Top-level navigation graph for the whole app.
 *
 * There is no persisted "has completed onboarding" flag yet (that lands
 * with Phase 3/4's local storage), so every fresh process start currently
 * begins at onboarding. This is acceptable for a UI-only phase and will be
 * revisited once there is somewhere durable to store the flag.
 */
@Composable
fun SafeShieldNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.ONBOARDING_WELCOME) {
        composable(Routes.ONBOARDING_WELCOME) {
            OnboardingWelcomeScreen(
                onGetStarted = { navController.navigate(Routes.ONBOARDING_EXPLAIN) }
            )
        }
        composable(Routes.ONBOARDING_EXPLAIN) {
            OnboardingExplainScreen(
                onContinue = { navController.navigate(Routes.PROTECTION_MODE_SELECT) }
            )
        }
        composable(Routes.PROTECTION_MODE_SELECT) {
            ProtectionModeScreen(
                onSelectStandard = { navController.navigateToHomeClearingBackStack() },
                onSelectStrong = { navController.navigate(Routes.STRONG_PROTECTION_CONSENT) },
                // Never the graph's start destination, so there is always
                // somewhere to go back to (the explain screen during
                // onboarding, or Home/Settings if reached later).
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.STRONG_PROTECTION_CONSENT) {
            StrongProtectionConsentScreen(
                onCancel = { navController.popBackStack() },
                onConfirm = { navController.navigateToHomeClearingBackStack() }
            )
        }
        composable(Routes.HOME) {
            HomeScreen(
                onEnableProtection = { navController.navigate(Routes.PROTECTION_MODE_SELECT) },
                onEnableStrongProtection = { navController.navigate(Routes.STRONG_PROTECTION_CONSENT) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenProtectionMode = { navController.navigate(Routes.PROTECTION_MODE_SELECT) },
                onOpenSecurity = { navController.navigate(Routes.SETTINGS_SECURITY) },
                onOpenAboutPrivacy = { navController.navigate(Routes.SETTINGS_ABOUT_PRIVACY) }
            )
        }
        composable(Routes.SETTINGS_SECURITY) {
            AdminSecurityScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS_ABOUT_PRIVACY) {
            AboutPrivacyScreen(onBack = { navController.popBackStack() })
        }
    }
}

/**
 * Navigates to Home as a fresh root, clearing everything below it
 * (onboarding screens, mode selection, consent screens). Used any time a
 * protection-mode choice is finalized, whether that happened during
 * first-run onboarding or from Settings later.
 */
private fun NavHostController.navigateToHomeClearingBackStack() {
    navigate(Routes.HOME) {
        popUpTo(0) { inclusive = true }
        launchSingleTop = true
    }
}
