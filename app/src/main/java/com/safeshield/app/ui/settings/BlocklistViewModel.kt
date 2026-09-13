package com.safeshield.app.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.safeshield.app.database.AppDatabase
import com.safeshield.app.database.SafeShieldRepository
import com.safeshield.app.database.SettingsEntity
import com.safeshield.app.updater.BlocklistSyncScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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

    /** Enqueues an immediate WorkManager sync — actual success/failure shows up later via [settings]`.lastBlocklistUpdate` changing (or not). */
    fun syncNow() {
        BlocklistSyncScheduler.triggerImmediateSync(getApplication())
    }

    fun setBlockAdultContent(enabled: Boolean) {
        viewModelScope.launch { repository.setBlockAdultContent(enabled) }
    }
}
