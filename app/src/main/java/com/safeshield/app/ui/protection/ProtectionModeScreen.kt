package com.safeshield.app.ui.protection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.safeshield.app.ui.components.SafeShieldTopBar
import com.safeshield.app.ui.theme.SafeShieldTheme

/**
 * Lets the user choose between Standard and Strong Protection (PRD Core
 * User Flow step 4). Reused both during first-run onboarding and later from
 * Settings/Home if the user wants to change modes.
 *
 * Selecting Standard here does not yet start anything — VPN filtering is
 * wired in Phase 2/5. Selecting Strong only navigates to the dedicated
 * consent screen; it never starts device provisioning directly.
 */
@Composable
fun ProtectionModeScreen(
    onSelectStandard: () -> Unit,
    onSelectStrong: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    Scaffold(
        topBar = { SafeShieldTopBar(title = "Choose your protection", onBack = onBack) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ModeCard(
                title = "Standard Protection",
                description = "VPN-based domain filtering. Settings are protected by " +
                    "an administrator PIN. Can be turned off from Android's normal " +
                    "app settings like any other app.",
                buttonLabel = "Select Standard",
                onSelect = onSelectStandard
            )
            ModeCard(
                title = "Strong Protection",
                description = "Everything in Standard Protection, plus legitimate " +
                    "Android device-management enrollment where your device " +
                    "supports it, making protection harder to disable or " +
                    "uninstall. Requires an extra confirmation step and Android's " +
                    "own system provisioning flow.",
                buttonLabel = "Continue to Strong Protection",
                onSelect = onSelectStrong
            )
            Text(
                text = "You can change this later from Settings. Neither mode " +
                    "guarantees protection can never be bypassed or uninstalled — " +
                    "see About & Privacy for details.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ModeCard(
    title: String,
    description: String,
    buttonLabel: String,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleLarge)
            Text(text = description, style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(
                onClick = onSelect,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(buttonLabel)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProtectionModeScreenPreview() {
    SafeShieldTheme {
        ProtectionModeScreen(onSelectStandard = {}, onSelectStrong = {}, onBack = {})
    }
}
