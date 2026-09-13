package com.safeshield.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.safeshield.app.ui.navigation.SafeShieldNavHost
import com.safeshield.app.ui.theme.SafeShieldTheme

/**
 * Application entry point. Hosts the full navigation graph (onboarding,
 * home, settings, protection setup, admin/security, about & privacy) —
 * see [SafeShieldNavHost] for the screen wiring.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SafeShieldTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    SafeShieldNavHost()
                }
            }
        }
    }
}
