package com.safeshield.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.safeshield.app.device.DeviceManagementViewModel
import com.safeshield.app.device.ManagementState
import com.safeshield.app.device.displayLabel
import com.safeshield.app.ui.components.SafeShieldTopBar

/**
 * Settings > Device Management (PRD Phase 14: "Management status,
 * Provisioning information, Supported policies"). Built now, in Phase 6,
 * since it's the natural home for showing what [DeviceManagementViewModel]
 * already tracks — there's no reason to leave it as a placeholder until
 * Phase 14 just because that's where the PRD's UI polish pass mentions it.
 */
@Composable
fun DeviceManagementScreen(
    onBack: () -> Unit,
    onStartStrongProtectionSetup: () -> Unit,
    viewModel: DeviceManagementViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = { SafeShieldTopBar(title = "Device Management", onBack = onBack) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = "Status", style = MaterialTheme.typography.titleLarge)
            Text(text = state.displayLabel(), style = MaterialTheme.typography.bodyLarge)

            Text(text = "Supported policies", style = MaterialTheme.typography.titleLarge)
            Text(text = viewModel.describeSupportedPolicies(), style = MaterialTheme.typography.bodyMedium)

            Text(
                text = "A normal Android app cannot guarantee stronger management " +
                    "than what your device and Android version actually support. " +
                    "See About & Privacy for the full platform-limitations note.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (state == ManagementState.NOT_MANAGED) {
                Button(
                    onClick = onStartStrongProtectionSetup,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Set up Strong Protection")
                }
            }
        }
    }
}
