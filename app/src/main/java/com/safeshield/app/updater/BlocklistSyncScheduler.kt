package com.safeshield.app.updater

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import java.util.concurrent.TimeUnit

/**
 * Schedules [BlocklistUpdater]. Called once from
 * [com.safeshield.app.SafeShieldApplication] at process start —
 * `enqueueUniquePeriodicWork` with [ExistingPeriodicWorkPolicy.KEEP] makes
 * repeated calls (every app launch) harmless rather than stacking up
 * duplicate periodic jobs.
 */
object BlocklistSyncScheduler {
    private const val PERIODIC_WORK_NAME = "safeshield_blocklist_sync"
    private const val MANUAL_WORK_NAME = "safeshield_blocklist_sync_manual"
    private val SYNC_INTERVAL = 12L to TimeUnit.HOURS

    private fun networkConstraints() = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    fun schedulePeriodicSync(context: Context) {
        val request = PeriodicWorkRequestBuilder<BlocklistUpdater>(SYNC_INTERVAL.first, SYNC_INTERVAL.second)
            .setConstraints(networkConstraints())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    /** Lets a "Update now" UI action (Settings > Blocklist) run a sync immediately, outside the periodic schedule. */
    fun triggerImmediateSync(context: Context) {
        val request = OneTimeWorkRequestBuilder<BlocklistUpdater>()
            .setConstraints(networkConstraints())
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            MANUAL_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}
