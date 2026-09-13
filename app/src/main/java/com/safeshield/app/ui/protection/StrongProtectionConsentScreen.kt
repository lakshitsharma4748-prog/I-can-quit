package com.safeshield.app.ui.protection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
 * Strong Protection consent screen (PRD §3). Shown before SafeShield ever
 * launches Android's device-management provisioning flow.
 *
 * In this phase, confirming does not start real provisioning — that lands
 * in Phase 6 (DevicePolicyManager integration). Confirming here only
 * returns to Home, which still reports Device Management as NOT CONFIGURED
 * until real provisioning exists, so nothing is misrepresented to the user.
 */
@Composable
fun StrongProtectionConsentScreen(
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    Scaffold(
        topBar = { SafeShieldTopBar(title = "Enable Strong Protection", onBack = onCancel) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "By continuing, you agree to let SafeShield manage supported " +
                    "device settings and enforce the selected content-blocking " +
                    "policies.",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Depending on your device and Android version, management " +
                    "may restrict changing certain settings or uninstalling/" +
                    "disabling SafeShield while the device is managed.",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "SafeShield does not secretly record browsing history or " +
                    "read private messages.",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Exact capabilities depend on your device manufacturer and " +
                    "Android version. Not every device supports every policy, and " +
                    "no app can guarantee protection can never be bypassed or " +
                    "uninstalled. The next screen will be Android's own system " +
                    "prompt — SafeShield cannot enable device management without " +
                    "you explicitly confirming it there too.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("I Understand & Enable")
                }
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StrongProtectionConsentScreenPreview() {
    SafeShieldTheme {
        StrongProtectionConsentScreen(onCancel = {}, onConfirm = {})
    }
}
