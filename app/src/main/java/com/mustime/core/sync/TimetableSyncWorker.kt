package com.mustime.core.sync
import com.mustime.BuildConfig

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mustime.TimetableApplication
import com.mustime.core.database.AppDatabase
import com.mustime.core.notification.NotificationHelper
import com.mustime.core.update.InAppUpdateManager
import com.mustime.features.timetable.domain.TimetableMetadata
import com.mustime.features.timetable.domain.TimetableVersionResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Background WorkManager worker that periodically polls remote timetable metadata,
 * detects published updates or revisions, updates local Room cache,
 * and posts a system notification to inform students.
 * Also checks for app version updates (APK) and notifies via InAppUpdateManager.
 */
class TimetableSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val app = applicationContext as? TimetableApplication
        val db: AppDatabase? = app?.database
        if (db == null) {
            Log.w(TAG, "Application or AppDatabase not initialized; retrying later")
            return@withContext Result.retry()
        }

        try {
            val localMeta: TimetableMetadata? = db.timetableDao().getTimetableMetadataSync()
            Log.d(TAG, "Current local timetable metadata: ${localMeta?.versionLabel} (updated: ${localMeta?.lastUpdated})")

            // 1. Try remote HTTP fetch if endpoint is reachable
            var remoteMeta: TimetableMetadata? = fetchFromHttp()

            // 2. If HTTP did not produce metadata, fallback to Firestore sync repository
            if (remoteMeta == null) {
                remoteMeta = app?.syncRepository?.fetchRemoteTimetableMetadata()
            }

            if (remoteMeta == null) {
                Log.d(TAG, "No remote timetable metadata could be fetched at this time")
            } else {
                // 3. Compare with local metadata to detect new publications
                val isNewer = isNewerVersion(localMeta, remoteMeta)

                if (isNewer) {
                    Log.i(TAG, "New timetable version detected! Local: '${localMeta?.versionLabel}', Remote: '${remoteMeta.versionLabel}'")
                    // Persist new metadata to Room
                    db.timetableDao().upsertTimetableMetadata(remoteMeta)

                    // Notify student via System Tray Notification and In-App notification store
                    NotificationHelper.showTimetableUpdatedNotification(
                        context = applicationContext,
                        versionLabel = remoteMeta.versionLabel,
                        releaseNotes = remoteMeta.releaseNotes
                    )
                } else {
                    Log.d(TAG, "Timetable is up to date (${localMeta?.versionLabel})")
                }
            }

            // 4. Check for app version updates (APK)
            checkForAppUpdate()

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error executing TimetableSyncWorker: ${e.message}", e)
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private suspend fun checkForAppUpdate() {
        try {
            val prefs = applicationContext.getSharedPreferences("mustime_sync_prefs", Context.MODE_PRIVATE)
            val versionJsonUrl = prefs.getString("app_version_json_url", DEFAULT_VERSION_JSON_URL) ?: return

            val versionInfo = InAppUpdateManager.fetchVersionInfo(versionJsonUrl, BuildConfig.VERSION_CODE)
            if (versionInfo != null) {
                Log.i(TAG, "New app version available: ${versionInfo.versionName} (code ${versionInfo.versionCode})")
                
                // Store update info for UI to pick up
                val updatePrefs = applicationContext.getSharedPreferences("app_update_prefs", Context.MODE_PRIVATE)
                updatePrefs.edit().apply {
                    putInt("available_version_code", versionInfo.versionCode)
                    putString("available_version_name", versionInfo.versionName)
                    putString("release_notes", versionInfo.releaseNotes)
                    putString("download_url", versionInfo.downloadUrl)
                    putLong("file_size_bytes", versionInfo.fileSizeBytes)
                    putString("sha256", versionInfo.sha256)
                    putBoolean("is_mandatory", versionInfo.isMandatory)
                    putLong("checked_at", System.currentTimeMillis())
                    apply()
                }

                // Show notification about available update
                NotificationHelper.showAppUpdateNotification(
                    context = applicationContext,
                    versionName = versionInfo.versionName,
                    releaseNotes = versionInfo.releaseNotes,
                    isMandatory = versionInfo.isMandatory
                )
            } else {
                Log.d(TAG, "App is up to date (v${BuildConfig.VERSION_NAME}, code ${BuildConfig.VERSION_CODE})")
            }
        } catch (e: Exception) {
            Log.w(TAG, "App version check failed: ${e.message}")
        }
    }

    private fun isNewerVersion(local: TimetableMetadata?, remote: TimetableMetadata): Boolean {
        if (local == null) return true
        if (remote.versionLabel != local.versionLabel) return true
        if (remote.lastUpdated > local.lastUpdated) return true
        if (remote.draftNumber != null && local.draftNumber != null && remote.draftNumber > local.draftNumber) return true
        if (remote.isFinal && !local.isFinal) return true
        return false
    }

    private fun fetchFromHttp(): TimetableMetadata? {
        val prefs = applicationContext.getSharedPreferences("mustime_sync_prefs", Context.MODE_PRIVATE)
        val url = prefs.getString(PREF_REMOTE_METADATA_URL, DEFAULT_METADATA_URL) ?: return null

        return try {
            val client = OkHttpClient.Builder()
                .connectTimeout(12, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()

            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("User-Agent", "Lectures-Android-SyncWorker")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.d(TAG, "Remote HTTP check returned code ${response.code}")
                    return null
                }
                val bodyString = response.body?.string() ?: return null
                val obj = JSONObject(bodyString)

                val rawVersion = obj.optString("version_label", obj.optString("version", "Final Timetable"))
                val isExplicitFinal = if (obj.has("is_final")) obj.optBoolean("is_final") else null
                val acadYear = obj.optString("academic_year", "2026/2027")
                val semester = obj.optString("semester", "Semester 1")
                val releaseNotes = if (obj.has("release_notes")) obj.optString("release_notes") else null
                val totalSessions = obj.optInt("total_sessions", 1304)

                TimetableVersionResolver.resolve(
                    rawVersion = rawVersion,
                    isExplicitFinal = isExplicitFinal,
                    academicYear = acadYear,
                    semester = semester,
                    releaseNotes = releaseNotes,
                    totalEntries = totalSessions
                )
            }
        } catch (e: Exception) {
            Log.d(TAG, "fetchFromHttp skipped or network unavailable: ${e.message}")
            null
        }
    }

    companion object {
        const val TAG = "TimetableSyncWorker"
        const val WORK_NAME = "PeriodicTimetableSyncWork"
        const val PREF_REMOTE_METADATA_URL = "remote_timetable_metadata_url"
        const val DEFAULT_METADATA_URL = "https://raw.githubusercontent.com/mustime/timetable/main/timetable_metadata.json"
        const val DEFAULT_VERSION_JSON_URL = "https://raw.githubusercontent.com/mugmarp/mylectures/main/version.json"
    }
}
