package com.safeshield.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.safeshield.app.ui.components.SafeShieldTopBar
import java.text.DateFormat
import java.util.Date

/**
 * Settings > Blocklist (PRD Phase 14 lists "Last update, Update now,
 * Categories" here; built now in Phase 12 since it's the natural place to
 * see the sync job this phase adds actually working). Category-level
 * management beyond the one blockAdultContent toggle Room already has
 * (Phase 3) is left for whenever there's more than one real synced
 * category to manage.
 */
@Composable
fun BlocklistScreen(
    onBack: () -> Unit,
    viewModel: BlocklistViewModel = viewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val domainCount by viewModel.domainCount.collectAsState()

    Scaffold(
        topBar = { SafeShieldTopBar(title = "Blocklist", onBack = onBack) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = "Last update", style = MaterialTheme.typography.titleLarge)
            Text(
                text = settings.lastBlocklistUpdate?.let { formatTimestamp(it) } ?: "Never synced",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "$domainCount domains currently stored locally.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(onClick = viewModel::syncNow, modifier = Modifier.fillMaxWidth()) {
                Text("Update now")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Block adult content", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "When off, synced adult-category domains are allowed even if still present in the blocklist.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = settings.blockAdultContent, onCheckedChange = viewModel::setBlockAdultContent)
            }

            Text(
                text = "Offline devices keep filtering with the last successfully downloaded blocklist — " +
                    "a failed or malformed update never replaces a working one.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatTimestamp(epochMillis: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(epochMillis))
