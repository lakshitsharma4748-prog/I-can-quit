package com.safeshield.app.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.safeshield.app.ui.theme.SafeShieldTheme

private data class ExplainerItem(val title: String, val body: String)

private val PERMISSION_ITEMS = listOf(
    ExplainerItem(
        title = "VPN connection",
        body = "SafeShield uses Android's built-in VPN framework to inspect which " +
            "domains your device is trying to reach, so it can block adult " +
            "content. It does not route your traffic through any third-party " +
            "server."
    ),
    ExplainerItem(
        title = "Device management (optional)",
        body = "If you choose Strong Protection, SafeShield asks Android to " +
            "enroll this device under official device-management APIs so " +
            "protection is harder to disable. This step is entirely optional " +
            "and is explained again, in detail, before it happens."
    ),
    ExplainerItem(
        title = "What SafeShield does not do",
        body = "SafeShield does not read your messages, record your screen, log " +
            "your browsing history, or upload the sites you visit."
    )
)

/**
 * Explains what protection does and the permissions/management capabilities
 * that may be requested later, before the user picks a mode (PRD Core User
 * Flow steps 2–3).
 */
@Composable
fun OnboardingExplainScreen(onContinue: () -> Unit) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "What SafeShield will ask for",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "Before you turn anything on, here's exactly what SafeShield " +
                    "can access and why.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            PERMISSION_ITEMS.forEach { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = item.title, style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = item.body,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            }

            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Continue")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingExplainScreenPreview() {
    SafeShieldTheme {
        OnboardingExplainScreen(onContinue = {})
    }
}
