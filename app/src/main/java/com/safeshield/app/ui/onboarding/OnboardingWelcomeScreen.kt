package com.safeshield.app.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.safeshield.app.ui.theme.SafeShieldTheme

/**
 * First screen the user sees. Purely introductory — no permissions are
 * requested here (PRD Core User Flow step 2: "App explains what protection
 * does").
 */
@Composable
fun OnboardingWelcomeScreen(onGetStarted: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "SafeShield",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "A consent-based blocker for adult and explicit websites.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp)
        )
        Text(
            text = "You stay in control the whole time — SafeShield never turns " +
                "on protection, requests permissions, or changes device settings " +
                "without you explicitly choosing to.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp)
        )
        Button(
            onClick = onGetStarted,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp)
        ) {
            Text("Get started")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingWelcomeScreenPreview() {
    SafeShieldTheme {
        OnboardingWelcomeScreen(onGetStarted = {})
    }
}
