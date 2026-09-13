package com.safeshield.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable

/**
 * Standard top bar used by every non-Home screen, with an optional back
 * action. Home has no back target so it doesn't use this composable.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafeShieldTopBar(
    title: String,
    onBack: (() -> Unit)? = null
) {
    CenterAlignedTopAppBar(
        title = { Text(title) },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors()
    )
}
