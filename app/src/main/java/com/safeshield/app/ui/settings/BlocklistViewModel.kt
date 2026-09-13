package com.safeshield.app.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import com.safeshield.app.database.AppDatabase
import com.safeshield.app.database.SafeShieldRepository
import com.safeshield.app.database.SettingsEntity
import com.safeshield.app.updater.BlocklistSyncScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Loading/error state for a manually-triggered sync (PRD Phase 14). */
enum class SyncUiState {
    IDLE,
    SYNCING,
    SUCCEEDED,
    FAILED
}

class BlocklistViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SafeShieldRepository

    init {
        val database = AppDatabase.getInstance(application)
        repository = SafeShieldRepository(database.domainDao(), database.allowlistDao(), database.settingsDao())
    }

    val settings: StateFlow<SettingsEntity> = repository.settings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        SettingsEntity()
    )

    val domainCount: StateFlow<Int> = repository.allDomains
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** Reflects the most recent manually-triggered sync's WorkManager state; IDLE before one has ever run this session. */
    val syncState: StateFlow<SyncUiState> = BlocklistSyncScheduler.observeManualSyncState(application)
        .map { infos -> infos.firstOrNull()?.state.toSyncUiState() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SyncUiState.IDLE)

    /** Enqueues an immediate WorkManager sync; [syncState] reflects its progress. */
    fun syncNow() {
        BlocklistSyncScheduler.triggerImmediateSync(getApplication())
    }

    fun setBlockAdultContent(enabled: Boolean) {
        viewModelScope.launch { repository.setBlockAdultContent(enabled) }
    }

    private fun WorkInfo.State?.toSyncUiState(): SyncUiState = when (this) {
        WorkInfo.State.RUNNING, WorkInfo.State.ENQUEUED -> SyncUiState.SYNCING
        WorkInfo.State.SUCCEEDED -> SyncUiState.SUCCEEDED
        WorkInfo.State.FAILED -> SyncUiState.FAILED
        WorkInfo.State.CANCELLED, WorkInfo.State.BLOCKED, null -> SyncUiState.IDLE
    }
}
