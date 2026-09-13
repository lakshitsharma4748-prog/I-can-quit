package com.safeshield.app

import android.app.Application
import com.safeshield.app.updater.BlocklistSyncScheduler

class SafeShieldApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        BlocklistSyncScheduler.schedulePeriodicSync(this)
    }
}
