package com.safeshield.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.safeshield.app.ui.components.SafeShieldTopBar
import com.safeshield.app.ui.theme.SafeShieldTheme

private data class SettingsEntry(val title: String, val subtitle: String, val onClick: () -> Unit)

/**
 * Settings hub. Blocklist and Allowlist sections are added once Phase 12
 * (sync) gives them something worth a dedicated screen.
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenProtectionMode: () -> Unit,
    onOpenSecurity: () -> Unit,
    onOpenDeviceManagement: () -> Unit,
    onOpenAboutPrivacy: () -> Unit
) {
    val entries = listOf(
        SettingsEntry(
            title = "Protection",
            subtitle = "Switch between Standard and Strong Protection",
            onClick = onOpenProtectionMode
        ),
        SettingsEntry(
            title = "Security",
            subtitle = "Administrator PIN",
            onClick = onOpenSecurity
        ),
        SettingsEntry(
            title = "Device Management",
            subtitle = "Management status, provisioning, supported policies",
            onClick = onOpenDeviceManagement
        ),
        SettingsEntry(
            title = "About & Privacy",
            subtitle = "What SafeShield does and does not do with your data",
            onClick = onOpenAboutPrivacy
        )
    )

    Scaffold(
        topBar = { SafeShieldTopBar(title = "Settings", onBack = onBack) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            entries.forEach { entry ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable(onClick = entry.onClick),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = entry.title, style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = entry.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    SafeShieldTheme {
        SettingsScreen(
            onBack = {},
            onOpenProtectionMode = {},
            onOpenSecurity = {},
            onOpenDeviceManagement = {},
            onOpenAboutPrivacy = {}
        )
    }
}
