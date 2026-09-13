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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.safeshield.app.ui.components.StatusCard
import com.safeshield.app.ui.components.StatusRow
import com.safeshield.app.ui.theme.SafeShieldTheme
import com.safeshield.app.vpn.VpnState

/**
 * Home screen. As of Phase 5, Protection/VPN rows reflect real state
 * ([protectionEnabled] from Room settings, [vpnState] from the running
 * VpnService). Device Management (Phase 6) and Blocklist (Phase 12/14)
 * remain static placeholders until those phases wire them up.
 */
@Composable
fun HomeScreen(
    protectionEnabled: Boolean,
    vpnState: VpnState,
    onEnableProtection: () -> Unit,
    onDisableProtectionRequested: () -> Unit,
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
                    value = if (protectionEnabled) "ACTIVE" else "INACTIVE",
                    indicatorColor = if (protectionEnabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    filled = true
                )
                StatusRow(
                    label = "VPN",
                    value = vpnStatusText(vpnState),
                    indicatorColor = vpnStatusColor(vpnState),
                    filled = vpnState == VpnState.CONNECTED
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
                if (protectionEnabled) {
                    OutlinedButton(
                        onClick = onDisableProtectionRequested,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Disable Protection")
                    }
                } else {
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

private fun vpnStatusText(state: VpnState): String = when (state) {
    VpnState.DISCONNECTED -> "DISCONNECTED"
    VpnState.CONNECTING -> "CONNECTING"
    VpnState.CONNECTED -> "CONNECTED"
    VpnState.ERROR -> "ERROR — stopped unexpectedly"
}

@Composable
private fun vpnStatusColor(state: VpnState): Color = when (state) {
    VpnState.CONNECTED -> MaterialTheme.colorScheme.primary
    VpnState.ERROR -> MaterialTheme.colorScheme.error
    VpnState.CONNECTING, VpnState.DISCONNECTED -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    SafeShieldTheme {
        HomeScreen(
            protectionEnabled = false,
            vpnState = VpnState.DISCONNECTED,
            onEnableProtection = {},
            onDisableProtectionRequested = {},
            onEnableStrongProtection = {},
            onOpenSettings = {}
        )
    }
}
