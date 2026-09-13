package com.safeshield.app.ui.navigation

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.safeshield.app.ui.admin.AdminSecurityScreen
import com.safeshield.app.ui.components.PinConfirmDialog
import com.safeshield.app.ui.home.HomeScreen
import com.safeshield.app.ui.onboarding.OnboardingExplainScreen
import com.safeshield.app.ui.onboarding.OnboardingWelcomeScreen
import com.safeshield.app.ui.protection.ProtectionModeScreen
import com.safeshield.app.ui.protection.ProtectionViewModel
import com.safeshield.app.ui.protection.StrongProtectionConsentScreen
import com.safeshield.app.ui.settings.AboutPrivacyScreen
import com.safeshield.app.ui.settings.SettingsScreen
import com.safeshield.app.vpn.SafeShieldVpnService
import kotlinx.coroutines.launch

/**
 * Top-level navigation graph for the whole app.
 *
 * There is no persisted "has completed onboarding" flag yet (that lands
 * with a later phase's local storage), so every fresh process start
 * currently begins at onboarding. This is acceptable for now and will be
 * revisited once there is somewhere durable to store the flag.
 */
@Composable
fun SafeShieldNavHost(navController: NavHostController = rememberNavController()) {
    val context = LocalContext.current
    val protectionViewModel: ProtectionViewModel = viewModel()

    // VPN consent (VpnService.prepare()) needs an ActivityResultLauncher,
    // which only exists at this Compose/Activity layer — ProtectionViewModel
    // itself just starts/stops the already-consented service.
    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            protectionViewModel.activateProtection()
        }
        // A denied/cancelled consent prompt leaves protection off — Home
        // keeps showing INACTIVE, which is already the truthful state.
    }

    fun requestProtectionActivation() {
        val consentIntent = SafeShieldVpnService.prepareIntent(context)
        if (consentIntent != null) {
            vpnPermissionLauncher.launch(consentIntent)
        } else {
            protectionViewModel.activateProtection()
        }
    }

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
                onSelectStandard = {
                    requestProtectionActivation()
                    navController.navigateToHomeClearingBackStack()
                },
                onSelectStrong = { navController.navigate(Routes.STRONG_PROTECTION_CONSENT) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.STRONG_PROTECTION_CONSENT) {
            StrongProtectionConsentScreen(
                onCancel = { navController.popBackStack() },
                onConfirm = {
                    // Phase 6 adds real Device Owner provisioning on top of
                    // this; for now, confirming Strong Protection activates
                    // the same VPN filtering Standard Protection does.
                    requestProtectionActivation()
                    navController.navigateToHomeClearingBackStack()
                }
            )
        }
        composable(Routes.HOME) {
            val settings by protectionViewModel.settings.collectAsState()
            val vpnState by protectionViewModel.vpnState.collectAsState()
            val coroutineScope = rememberCoroutineScope()
            var showDisableConfirm by remember { mutableStateOf(false) }
            var pinErrorMessage by remember { mutableStateOf<String?>(null) }

            HomeScreen(
                protectionEnabled = settings.protectionEnabled,
                vpnState = vpnState,
                onEnableProtection = { navController.navigate(Routes.PROTECTION_MODE_SELECT) },
                onDisableProtectionRequested = {
                    if (protectionViewModel.isPinRequiredToDisable()) {
                        pinErrorMessage = null
                        showDisableConfirm = true
                    } else {
                        protectionViewModel.deactivateProtection()
                    }
                },
                onEnableStrongProtection = { navController.navigate(Routes.STRONG_PROTECTION_CONSENT) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) }
            )

            if (showDisableConfirm) {
                PinConfirmDialog(
                    title = "Enter PIN to disable protection",
                    errorMessage = pinErrorMessage,
                    onDismiss = { showDisableConfirm = false },
                    onConfirm = { pin ->
                        coroutineScope.launch {
                            if (protectionViewModel.verifyPinToDisable(pin)) {
                                protectionViewModel.deactivateProtection()
                                showDisableConfirm = false
                            } else {
                                pinErrorMessage = "Incorrect PIN"
                            }
                        }
                    }
                )
            }
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
