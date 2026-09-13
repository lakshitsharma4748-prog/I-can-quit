package com.safeshield.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.safeshield.app.ui.components.SafeShieldTopBar
import com.safeshield.app.ui.theme.SafeShieldTheme

private val PRIVACY_POINTS = listOf(
    "SafeShield does not store your browsing history.",
    "SafeShield does not upload the URLs you visit.",
    "SafeShield does not collect your search history.",
    "SafeShield does not read private messages.",
    "SafeShield does not record your screen.",
    "SafeShield does not monitor you secretly, ever."
)

/**
 * About & Privacy screen. Content here is final copy (not a placeholder) —
 * it describes what the product commits to, independent of which phase of
 * development it's currently in.
 */
@Composable
fun AboutPrivacyScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = { SafeShieldTopBar(title = "About & Privacy", onBack = onBack) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = "SafeShield", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "A consent-based Android app that blocks adult and " +
                        "explicit websites, using only official Android APIs.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = "Privacy", style = MaterialTheme.typography.titleLarge)
                PRIVACY_POINTS.forEach { point ->
                    Text(text = "•  $point", style = MaterialTheme.typography.bodyMedium)
                }
                Text(
                    text = "The only data SafeShield's backend ever receives is what " +
                        "is strictly necessary to keep the blocklist and app version " +
                        "up to date — never which sites you visit.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = "Platform limitations", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "A normal Android app cannot guarantee that a user can " +
                        "never uninstall, disable, or bypass it on every device. " +
                        "Strong Protection raises the bar using legitimate " +
                        "managed-device / Device Owner provisioning, where your " +
                        "device and Android version support it, but this remains " +
                        "subject to Android, manufacturer, and OS-version " +
                        "limitations. SafeShield does not claim to be " +
                        "100% unbypassable.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AboutPrivacyScreenPreview() {
    SafeShieldTheme {
        AboutPrivacyScreen(onBack = {})
    }
}
