package com.safeshield.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.safeshield.app.device.DeviceManagementController
import com.safeshield.app.device.DeviceManagementViewModel
import com.safeshield.app.device.ManagementState
import com.safeshield.app.device.displayLabel
import com.safeshield.app.ui.components.SafeShieldTopBar

/**
 * Settings > Device Management (PRD §14: "Management status, Provisioning
 * information, Supported policies"). Built in Phase 6 and extended in
 * Phase 7 to show the real, per-policy outcome from
 * [DeviceManagementController.applyStrongProtectionPolicies] rather than
 * only a general description — a policy that failed to apply on this
 * device is shown as failed, not silently dropped.
 */
@Composable
fun DeviceManagementScreen(
    onBack: () -> Unit,
    onStartStrongProtectionSetup: () -> Unit,
    viewModel: DeviceManagementViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val policyOutcomes by viewModel.policyOutcomes.collectAsState()

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

            if (policyOutcomes.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    policyOutcomes.forEach { outcome -> PolicyOutcomeRow(outcome) }
                }
            }

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

@Composable
private fun PolicyOutcomeRow(outcome: DeviceManagementController.PolicyOutcome) {
    Row(verticalAlignment = Alignment.Top) {
        Text(
            text = if (outcome.applied) "✓" else "✗",
            color = if (outcome.applied) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(end = 8.dp)
        )
        Column {
            Text(text = outcome.label, style = MaterialTheme.typography.bodyMedium)
            if (!outcome.applied && outcome.failureReason != null) {
                Text(
                    text = outcome.failureReason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
