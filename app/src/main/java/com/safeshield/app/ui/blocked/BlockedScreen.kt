package com.safeshield.app.ui.blocked

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.safeshield.app.ui.theme.SafeShieldTheme

/**
 * PRD Phase 15's "blocked website experience" — shown only when SafeShield
 * itself is already in the foreground and a block happens (see
 * [com.safeshield.app.vpn.BlockedEventBus]); the far more common case,
 * blocking something requested from another app, shows a system
 * notification instead (SafeShieldVpnService.buildBlockedNotification),
 * since this screen can't take over whatever app the user was actually in.
 *
 * Deliberately never names the domain that was blocked — only the fixed,
 * category-level explanation, matching the PRD's exact mockup text.
 */
@Composable
fun BlockedScreen(onDismiss: () -> Unit) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Website Blocked",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            Text(
                text = "SafeShield blocked this website because it matches an " +
                    "adult-content category.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(
                text = "Protection is active.",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp)
            )
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp)
            ) {
                Text("OK")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BlockedScreenPreview() {
    SafeShieldTheme {
        BlockedScreen(onDismiss = {})
    }
}
