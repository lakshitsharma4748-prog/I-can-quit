package com.safeshield.app.updater

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.safeshield.app.BuildConfig
import com.safeshield.app.database.AppDatabase
import com.safeshield.app.database.SafeShieldRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.SocketTimeoutException

/**
 * The WorkManager job PRD Phase 12 describes: fetch the backend's
 * blocklist over HTTPS, validate it, and — only if valid — atomically
 * replace what's stored locally. Never runs per-DNS-query (only on this
 * job's schedule, see [BlocklistSyncScheduler]), and never applies
 * anything [BlocklistValidator] didn't accept.
 */
class BlocklistUpdater(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val json = try {
            fetchJson(BuildConfig.BLOCKLIST_API_BASE_URL + BLOCKLIST_PATH)
        } catch (e: SocketTimeoutException) {
            Log.w(TAG, "Blocklist fetch timed out; will retry")
            return@withContext Result.retry()
        } catch (e: IOException) {
            Log.w(TAG, "Blocklist fetch failed (${e.javaClass.simpleName}); will retry")
            return@withContext Result.retry()
        } catch (e: IllegalArgumentException) {
            // e.g. a non-HTTPS BLOCKLIST_API_BASE_URL — a configuration
            // problem, not a transient network one, so retrying won't help.
            Log.e(TAG, "Refusing to fetch blocklist: ${e.message}")
            return@withContext Result.failure()
        }

        when (val result = BlocklistValidator.parseAndValidate(json)) {
            is BlocklistValidator.ParseResult.Invalid -> {
                Log.w(TAG, "Rejected malformed blocklist response: ${result.reason}")
                // The previously stored, valid blocklist is left exactly as
                // it was — nothing downstream of this ever sees the rejected
                // data.
                Result.failure()
            }
            is BlocklistValidator.ParseResult.Valid -> {
                val database = AppDatabase.getInstance(applicationContext)
                val repository = SafeShieldRepository(database.domainDao(), database.allowlistDao(), database.settingsDao())
                repository.applyBlocklistSync(result.response.domains.map { it.domain to it.category })
                Result.success()
            }
        }
    }

    private fun fetchJson(urlString: String): String {
        val url = URL(urlString)
        require(url.protocol == "https") { "Blocklist URL must be HTTPS, was '${url.protocol}'" }

        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS
        connection.requestMethod = "GET"
        try {
            val status = connection.responseCode
            if (status != HttpURLConnection.HTTP_OK) {
                throw IOException("Unexpected HTTP status $status")
            }
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        private const val TAG = "SafeShield/BlocklistUpdater"
        private const val BLOCKLIST_PATH = "/api/blocklist"
        private const val CONNECT_TIMEOUT_MS = 10_000
        private const val READ_TIMEOUT_MS = 10_000
    }
}
