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
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

/**
 * The WorkManager job PRD Phase 12 describes: fetch the backend's
 * blocklist over HTTPS, validate it, and — only if valid — atomically
 * replace what's stored locally. Never runs per-DNS-query (only on this
 * job's schedule, see [BlocklistSyncScheduler]), and never applies
 * anything [BlocklistValidator] didn't accept.
 *
 * Checks `GET /api/version` first and skips the full `/api/blocklist`
 * fetch when its `blocklistVersion` matches what's already applied — the
 * backend controller was written specifically to support this ("lets a
 * client decide whether a blocklist refresh is worth fetching before
 * actually fetching it"), so this uses it rather than always downloading
 * the full list on every 12-hour cycle.
 */
class BlocklistUpdater(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val database = AppDatabase.getInstance(applicationContext)
        val repository = SafeShieldRepository(database.domainDao(), database.allowlistDao(), database.settingsDao())

        val remoteVersion = fetchRemoteBlocklistVersion()
        if (remoteVersion != null && remoteVersion == repository.currentSettings().lastAppliedBlocklistVersion) {
            Log.i(TAG, "Blocklist already up to date; skipping full fetch")
            repository.recordBlocklistVersionCheck(remoteVersion)
            return@withContext Result.success()
        }

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
                repository.applyBlocklistSync(
                    entries = result.response.domains.map { it.domain to it.category },
                    version = result.response.version
                )
                Result.success()
            }
        }
    }

    /**
     * Best-effort: any failure here (network, malformed JSON, unexpected
     * shape) just means the optimization is skipped and the full
     * `/api/blocklist` is fetched as if this had never been tried — it
     * must never fail the sync outright over a problem with an endpoint
     * that only exists to make syncing more efficient.
     */
    private fun fetchRemoteBlocklistVersion(): String? = try {
        val json = fetchJson(BuildConfig.BLOCKLIST_API_BASE_URL + VERSION_PATH)
        Json { ignoreUnknownKeys = true }.decodeFromString<VersionResponse>(json).blocklistVersion
    } catch (e: Exception) {
        Log.w(TAG, "Could not check /api/version, will fetch the full blocklist instead: ${e.javaClass.simpleName}")
        null
    }

    private fun fetchJson(urlString: String): String {
        val url = URL(urlString)
        require(url.protocol == "https") { "URL must be HTTPS, was '${url.protocol}'" }

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
        private const val VERSION_PATH = "/api/version"
        private const val CONNECT_TIMEOUT_MS = 10_000
        private const val READ_TIMEOUT_MS = 10_000
    }
}
