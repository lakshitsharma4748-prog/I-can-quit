package com.safeshield.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.safeshield.app.ui.components.StatusCard
import com.safeshield.app.ui.components.StatusRow
import com.safeshield.app.ui.theme.SafeShieldTheme

/**
 * Home screen. Status values are static placeholders in this phase — none
 * of VPN, device-management, or blocklist logic exists yet (Phases 2, 6,
 * 12). Wiring real state through here happens as each of those phases
 * lands, without changing this screen's layout.
 */
@Composable
fun HomeScreen(
    onEnableProtection: () -> Unit,
    onEnableStrongProtection: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "SafeShield",
                style = MaterialTheme.typography.headlineMedium
            )

            StatusCard {
                StatusRow(
                    label = "Protection",
                    value = "INACTIVE",
                    indicatorColor = MaterialTheme.colorScheme.error,
                    filled = true
                )
                StatusRow(
                    label = "VPN",
                    value = "DISCONNECTED",
                    indicatorColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    filled = false
                )
                StatusRow(
                    label = "Device Management",
                    value = "NOT CONFIGURED",
                    indicatorColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    filled = false
                )
                StatusRow(
                    label = "Blocklist",
                    value = "NOT LOADED",
                    indicatorColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    filled = false
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onEnableProtection,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Enable Protection")
                }
                OutlinedButton(
                    onClick = onEnableStrongProtection,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Enable Strong Protection")
                }
                TextButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Settings")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    SafeShieldTheme {
        HomeScreen(onEnableProtection = {}, onEnableStrongProtection = {}, onOpenSettings = {})
    }
}
