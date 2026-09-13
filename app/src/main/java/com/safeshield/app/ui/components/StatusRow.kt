package com.safeshield.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * A single status line on the Home screen, e.g.
 *
 * ```
 * Protection
 * ● INACTIVE
 * ```
 *
 * [indicatorColor] is intentionally caller-supplied rather than inferred from
 * [value] — later phases will drive it from real VPN/device-management
 * state, not from string matching.
 */
@Composable
fun StatusRow(
    label: String,
    value: String,
    indicatorColor: Color,
    modifier: Modifier = Modifier,
    filled: Boolean = true
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (filled) "●" else "○", // ● or ○
                color = indicatorColor,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = " $value",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Groups related [StatusRow]s in a card, used on the Home screen.
 */
@Composable
fun StatusCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            content()
        }
    }
}
