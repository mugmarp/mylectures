package com.mustime.core.analytics

import android.content.Context
import android.os.Build
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID

/**
 * Essential Analytics & Telemetry Manager.
 *
 * Provides privacy-compliant, essential usage telemetry to monitor:
 * 1. Distinct active vs inactive users (via persistent install UUID + last_active heartbeat)
 * 2. Feature utilization ranking (timetable, vacant room finder, tasks, notes, alarms)
 * 3. Programme coverage and device diagnostic compatibility
 *
 * Fully adheres to Google Play Data Safety policies:
 * - Pseudonymous app installation UUID (no names, emails, phone numbers, or note contents)
 * - Transparent disclosure in Terms & Regulations
 */
class AnalyticsManager(private val context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("mustime_analytics", Context.MODE_PRIVATE)

    private val KEY_INSTALL_ID = "telemetry_install_id"
    private val KEY_FIRST_LAUNCH = "telemetry_first_launch"
    private val KEY_LAST_ACTIVE = "telemetry_last_active"
    private val KEY_LAUNCH_COUNT = "telemetry_launch_count"
    private val KEY_FEATURE_COUNTS = "telemetry_feature_counts"
    private val KEY_ENROLLED_PROGRAMME = "telemetry_programme"

    private val firestore: FirebaseFirestore? = try {
        FirebaseFirestore.getInstance()
    } catch (_: Throwable) {
        null
    }

    private val _featureStats = MutableStateFlow<Map<String, Int>>(emptyMap())
    val featureStats: StateFlow<Map<String, Int>> = _featureStats.asStateFlow()

    init {
        ensureInstallId()
        loadFeatureStats()
    }

    fun getInstallId(): String {
        return ensureInstallId()
    }

    private fun ensureInstallId(): String {
        var id = prefs.getString(KEY_INSTALL_ID, null)
        if (id.isNullOrBlank()) {
            id = "inst_" + UUID.randomUUID().toString().replace("-", "").take(16)
            val now = System.currentTimeMillis()
            prefs.edit()
                .putString(KEY_INSTALL_ID, id)
                .putLong(KEY_FIRST_LAUNCH, now)
                .putLong(KEY_LAST_ACTIVE, now)
                .putInt(KEY_LAUNCH_COUNT, 1)
                .apply()
        }
        return id
    }

    /**
     * Record app launch and dispatch heartbeat telemetry
     */
    fun recordAppLaunch(programme: String, scope: CoroutineScope) {
        val now = System.currentTimeMillis()
        val count = prefs.getInt(KEY_LAUNCH_COUNT, 0) + 1
        prefs.edit()
            .putLong(KEY_LAST_ACTIVE, now)
            .putInt(KEY_LAUNCH_COUNT, count)
            .putString(KEY_ENROLLED_PROGRAMME, programme)
            .apply()

        trackFeature("app_launch")
        dispatchHeartbeat(programme, scope)
    }

    /**
     * Track user engagement with a specific feature
     */
    fun trackFeature(featureName: String) {
        try {
            val jsonStr = prefs.getString(KEY_FEATURE_COUNTS, "{}") ?: "{}"
            val json = JSONObject(jsonStr)
            val current = json.optInt(featureName, 0)
            json.put(featureName, current + 1)
            prefs.edit().putString(KEY_FEATURE_COUNTS, json.toString()).apply()

            val map = mutableMapOf<String, Int>()
            val keys = json.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                map[k] = json.getInt(k)
            }
            _featureStats.value = map
        } catch (_: Exception) {}
    }

    fun getFeatureCount(featureName: String): Int {
        return _featureStats.value[featureName] ?: 0
    }

    private fun loadFeatureStats() {
        try {
            val jsonStr = prefs.getString(KEY_FEATURE_COUNTS, "{}") ?: "{}"
            val json = JSONObject(jsonStr)
            val map = mutableMapOf<String, Int>()
            val keys = json.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                map[k] = json.getInt(k)
            }
            _featureStats.value = map
        } catch (_: Exception) {}
    }

    /**
     * Dispatches heartbeat and feature stats to cloud Firestore if available.
     * Queues locally when offline with zero disruption to the user experience.
     */
    fun dispatchHeartbeat(programme: String, scope: CoroutineScope) {
        val store = firestore ?: return
        val installId = getInstallId()
        val firstLaunch = prefs.getLong(KEY_FIRST_LAUNCH, System.currentTimeMillis())
        val launchCount = prefs.getInt(KEY_LAUNCH_COUNT, 1)
        val features = _featureStats.value

        scope.launch(Dispatchers.IO) {
            try {
                val telemetryDoc = mutableMapOf<String, Any>(
                    "install_id" to installId,
                    "app_version" to "1.0",
                    "enrolled_programme" to programme.ifBlank { "Unselected" },
                    "device_model" to "${Build.MANUFACTURER} ${Build.MODEL}",
                    "android_version" to (Build.VERSION.RELEASE ?: "Unknown"),
                    "sdk_int" to Build.VERSION.SDK_INT,
                    "first_launch_timestamp" to firstLaunch,
                    "last_active_at" to FieldValue.serverTimestamp(),
                    "total_launches" to launchCount,
                    "features_used" to features
                )

                store.collection("user_telemetry")
                    .document(installId)
                    .set(telemetryDoc, SetOptions.merge())
            } catch (_: Exception) {
                // Safely handles offline mode
            }
        }
    }
}
