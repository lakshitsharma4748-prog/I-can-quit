package com.safeshield.app.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.safeshield.app.security.PinManager
import com.safeshield.app.ui.components.SafeShieldTopBar
import com.safeshield.app.ui.theme.SafeShieldTheme
import kotlinx.coroutines.delay

/**
 * Administrator PIN screen (PRD Phase 4). The normal user cannot bypass
 * this from within the app UI — there is no "skip" affordance — though as
 * with any Android app, whether protected settings can be reached some
 * other way (e.g. clearing app data) is a platform limitation, not
 * something this screen can prevent; see About & Privacy.
 */
@Composable
fun AdminSecurityScreen(
    onBack: () -> Unit,
    viewModel: AdminSecurityViewModel = viewModel()
) {
    val isPinSet by viewModel.isPinSet.collectAsState()
    val message by viewModel.message.collectAsState()

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
                text = "Protects settings like turning protection off, editing the " +
                    "blocklist or allowlist, and changing the PIN itself. Stored " +
                    "using Android Keystore-backed encryption, never as plain " +
                    "text; repeated incorrect attempts are rate-limited.",
                style = MaterialTheme.typography.bodyMedium
            )

            if (isPinSet) {
                ChangePinForm(
                    onSubmit = viewModel::changePin,
                    message = message,
                    onMessageShown = viewModel::clearMessage
                )
            } else {
                SetPinForm(
                    onSubmit = viewModel::setInitialPin,
                    message = message,
                    onMessageShown = viewModel::clearMessage
                )
            }
        }
    }
}

@Composable
private fun SetPinForm(
    onSubmit: (newPin: String, confirmPin: String) -> Unit,
    message: PinScreenMessage,
    onMessageShown: () -> Unit
) {
    var newPin by rememberSaveable { mutableStateOf("") }
    var confirmPin by rememberSaveable { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PinField(label = "New PIN (${PinManager.MIN_PIN_LENGTH}-${PinManager.MAX_PIN_LENGTH} digits)", value = newPin, onValueChange = { newPin = it })
        PinField(label = "Confirm new PIN", value = confirmPin, onValueChange = { confirmPin = it })
        MessageText(message)
        Button(
            onClick = {
                onSubmit(newPin, confirmPin)
                newPin = ""
                confirmPin = ""
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Set Administrator PIN")
        }
    }

    ResetMessageOnLeave(message, onMessageShown)
}

@Composable
private fun ChangePinForm(
    onSubmit: (current: String, newPin: String, confirmPin: String) -> Unit,
    message: PinScreenMessage,
    onMessageShown: () -> Unit
) {
    var currentPin by rememberSaveable { mutableStateOf("") }
    var newPin by rememberSaveable { mutableStateOf("") }
    var confirmPin by rememberSaveable { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PinField(label = "Current PIN", value = currentPin, onValueChange = { currentPin = it })
        PinField(label = "New PIN", value = newPin, onValueChange = { newPin = it })
        PinField(label = "Confirm new PIN", value = confirmPin, onValueChange = { confirmPin = it })
        MessageText(message)
        Button(
            onClick = {
                onSubmit(currentPin, newPin, confirmPin)
                currentPin = ""
                newPin = ""
                confirmPin = ""
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Change Administrator PIN")
        }
    }

    ResetMessageOnLeave(message, onMessageShown)
}

@Composable
private fun PinField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { input -> if (input.length <= PinManager.MAX_PIN_LENGTH && input.all { it.isDigit() }) onValueChange(input) },
        label = { Text(label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun MessageText(message: PinScreenMessage) {
    val text = when (message) {
        is PinScreenMessage.None -> return
        is PinScreenMessage.PinSaved -> "PIN saved."
        is PinScreenMessage.PinsDoNotMatch -> "The PINs you entered don't match."
        is PinScreenMessage.InvalidPinFormat ->
            "PIN must be ${PinManager.MIN_PIN_LENGTH}-${PinManager.MAX_PIN_LENGTH} digits."
        is PinScreenMessage.IncorrectCurrentPin -> "That's not the correct current PIN."
        is PinScreenMessage.LockedOut ->
            "Too many incorrect attempts. Try again in ${message.retryAfterSeconds}s."
    }
    val color = if (message is PinScreenMessage.PinSaved) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.error
    }
    Text(text = text, color = color, style = MaterialTheme.typography.bodyMedium)
}

/** Clears a one-shot message a few seconds after it's shown, whichever form is currently displayed. */
@Composable
private fun ResetMessageOnLeave(message: PinScreenMessage, onMessageShown: () -> Unit) {
    LaunchedEffect(message) {
        if (message !is PinScreenMessage.None) {
            delay(4_000)
            onMessageShown()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AdminSecurityScreenPreview() {
    SafeShieldTheme {
        // The real screen requires a ViewModel/Application context, so the
        // preview can't render meaningfully here without a fake — skipped
        // rather than faked with a placeholder that could drift from the
        // real screen's structure.
    }
}
