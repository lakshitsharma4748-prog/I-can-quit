package com.safeshield.app.ui.admin

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.safeshield.app.ui.components.ComingSoonNotice
import com.safeshield.app.ui.components.SafeShieldTopBar
import com.safeshield.app.ui.theme.SafeShieldTheme

/**
 * Administrator/security screen shell. Real PIN creation, verification,
 * secure storage, and rate-limiting are implemented in Phase 4 — this
 * phase only reserves the screen and is explicit that the PIN isn't
 * enforced yet, rather than showing a button that silently does nothing.
 */
@Composable
fun AdminSecurityScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = { SafeShieldTopBar(title = "Security", onBack = onBack) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Administrator PIN",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "Once enabled, protected settings — turning protection off, " +
                    "editing the blocklist or allowlist, and changing the PIN " +
                    "itself — will require this PIN. It will be stored using " +
                    "Android Keystore, never as plain text, and incorrect " +
                    "attempts will be rate-limited.",
                style = MaterialTheme.typography.bodyMedium
            )
            ComingSoonNotice(
                message = "Administrator PIN protection is not enforced yet in this " +
                    "build. It will be implemented in a later phase."
            )
            Button(
                onClick = {},
                enabled = false,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Set Administrator PIN")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AdminSecurityScreenPreview() {
    SafeShieldTheme {
        AdminSecurityScreen(onBack = {})
    }
}
