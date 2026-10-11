package com.mustime.features.feedback

import android.content.Context
import android.os.Build
import com.google.firebase.firestore.FirebaseFirestore
import com.mustime.features.timetable.data.ETagStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class FeedbackCategory(val title: String, val description: String) {
    BUG_REPORT("Bug Report", "Something isn't working as expected"),
    TIMETABLE_ISSUE("Timetable Discrepancy", "Lecture slot, venue, or lecturer error"),
    FEATURE_REQUEST("Feature Request", "New idea or improvement suggestion"),
    ROOM_FINDER("Room Finder Issue", "Free room availability inaccuracy"),
    GENERAL("General Thought", "App feedback or praise")
}

data class AnonymousFeedback(
    val id: String,
    val instanceId: String,
    val category: FeedbackCategory,
    val message: String,
    val formattedDate: String,
    val appVersion: String = "1.0",
    val androidRelease: String = Build.VERSION.RELEASE ?: "Unknown",
    val deviceModel: String = "${Build.MANUFACTURER} ${Build.MODEL}",
    val enrolledProgramme: String = ""
)

/**
 * Manages privacy-first, stateless feedback submissions.
 * Generates an anonymous, pseudonymous app-instance UUID (randomized, zero PII).
 * Submits directly to Firestore /feedback and maintains an offline retry queue.
 */
class FeedbackManager(private val context: Context, private val etagStore: ETagStore) {
    private val prefs = context.applicationContext.getSharedPreferences("mustime_feedback", Context.MODE_PRIVATE)
    private val INSTANCE_ID_KEY = "feedback_instance_id"
    private val LOCAL_FEEDBACK_LOG_KEY = "cached_feedback_queue"
    private val PENDING_RETRY_QUEUE_KEY = "pending_feedback_retry_queue"

    private val firestore: FirebaseFirestore? = try {
        FirebaseFirestore.getInstance()
    } catch (_: Throwable) {
        null
    }

    private val _recentSubmissions = MutableStateFlow<List<AnonymousFeedback>>(emptyList())
    val recentSubmissions: Flow<List<AnonymousFeedback>> = _recentSubmissions.asStateFlow()

    init {
        loadCachedFeedback()
    }

    /**
     * Retrieves or generates a pseudonymous App Instance UUID.
     * Truly anonymous: does not link to any Google account, IMEI, MAC, or student registration.
     */
    fun getOrCreateAppInstanceId(): String {
        var id = prefs.getString(INSTANCE_ID_KEY, null)
        if (id.isNullOrBlank()) {
            id = "app_" + UUID.randomUUID().toString().take(12)
            prefs.edit().putString(INSTANCE_ID_KEY, id).apply()
        }
        return id
    }

    suspend fun submitFeedback(
        category: FeedbackCategory,
        message: String,
        includeDiagnostics: Boolean = true
    ): Result<AnonymousFeedback> = withContext(Dispatchers.IO) {
        try {
            val instanceId = getOrCreateAppInstanceId()
            val dateFormat = SimpleDateFormat("MMM d, yyyy • HH:mm", Locale.getDefault())
            val enrolledProg = if (includeDiagnostics) (etagStore.getSavedProgramme() ?: "None") else "Omitted"

            val feedback = AnonymousFeedback(
                id = UUID.randomUUID().toString(),
                instanceId = instanceId,
                category = category,
                message = message.trim(),
                formattedDate = dateFormat.format(Date()),
                appVersion = "1.0 (Build 2026)",
                androidRelease = if (includeDiagnostics) Build.VERSION.RELEASE else "Hidden",
                deviceModel = if (includeDiagnostics) "${Build.MANUFACTURER} ${Build.MODEL}" else "Hidden",
                enrolledProgramme = enrolledProg
            )

            // 1. Persist locally in recent submissions UI list
            val currentList = _recentSubmissions.value.toMutableList()
            currentList.add(0, feedback)
            _recentSubmissions.value = currentList.take(20)
            saveCachedFeedback(currentList.take(20))

            // 2. Transmit to remote Firestore collection /feedback
            sendFeedbackToRemote(feedback)

            Result.success(feedback)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sends feedback item to Firestore /feedback.
     * If offline or transmission fails, queues into pending retry storage.
     */
    private suspend fun sendFeedbackToRemote(feedback: AnonymousFeedback): Boolean {
        val store = firestore
        if (store == null) {
            addToPendingQueue(feedback)
            return false
        }
        return try {
            val payload = hashMapOf(
                "id" to feedback.id,
                "instanceId" to feedback.instanceId,
                "category" to feedback.category.title,
                "categoryKey" to feedback.category.name,
                "message" to feedback.message,
                "formattedDate" to feedback.formattedDate,
                "appVersion" to feedback.appVersion,
                "androidRelease" to feedback.androidRelease,
                "deviceModel" to feedback.deviceModel,
                "enrolledProgramme" to feedback.enrolledProgramme,
                "submittedAt" to System.currentTimeMillis()
            )
            store.collection("feedback").add(payload).await()
            removeFromPendingQueue(feedback.id)
            true
        } catch (_: Exception) {
            addToPendingQueue(feedback)
            false
        }
    }

    /**
     * Retries flushing any offline pending feedback items when network/app becomes active.
     */
    suspend fun flushPendingSubmissions() = withContext(Dispatchers.IO) {
        val pending = loadPendingQueue()
        if (pending.isEmpty() || firestore == null) return@withContext

        val remaining = mutableListOf<AnonymousFeedback>()
        for (item in pending) {
            val sent = try {
                val payload = hashMapOf(
                    "id" to item.id,
                    "instanceId" to item.instanceId,
                    "category" to item.category.title,
                    "categoryKey" to item.category.name,
                    "message" to item.message,
                    "formattedDate" to item.formattedDate,
                    "appVersion" to item.appVersion,
                    "androidRelease" to item.androidRelease,
                    "deviceModel" to item.deviceModel,
                    "enrolledProgramme" to item.enrolledProgramme,
                    "submittedAt" to System.currentTimeMillis()
                )
                firestore.collection("feedback").add(payload).await()
                true
            } catch (_: Exception) {
                false
            }
            if (!sent) {
                remaining.add(item)
            }
        }
        savePendingQueue(remaining)
    }

    private fun addToPendingQueue(item: AnonymousFeedback) {
        val list = loadPendingQueue().toMutableList()
        if (list.none { it.id == item.id }) {
            list.add(item)
            savePendingQueue(list.take(50))
        }
    }

    private fun removeFromPendingQueue(id: String) {
        val list = loadPendingQueue().filterNot { it.id == id }
        savePendingQueue(list)
    }

    private fun loadPendingQueue(): List<AnonymousFeedback> {
        val raw = prefs.getString(PENDING_RETRY_QUEUE_KEY, null) ?: return emptyList()
        return parseFeedbackJson(raw)
    }

    private fun savePendingQueue(items: List<AnonymousFeedback>) {
        val array = serializeFeedbackJson(items)
        prefs.edit().putString(PENDING_RETRY_QUEUE_KEY, array.toString()).apply()
    }

    private fun loadCachedFeedback() {
        val raw = prefs.getString(LOCAL_FEEDBACK_LOG_KEY, null) ?: return
        _recentSubmissions.value = parseFeedbackJson(raw)
    }

    private fun saveCachedFeedback(items: List<AnonymousFeedback>) {
        val array = serializeFeedbackJson(items)
        prefs.edit().putString(LOCAL_FEEDBACK_LOG_KEY, array.toString()).apply()
    }

    private fun parseFeedbackJson(jsonStr: String): List<AnonymousFeedback> {
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<AnonymousFeedback>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val catStr = obj.optString("category", FeedbackCategory.GENERAL.name)
                val cat = try { FeedbackCategory.valueOf(catStr) } catch (_: Exception) { FeedbackCategory.GENERAL }
                list.add(
                    AnonymousFeedback(
                        id = obj.getString("id"),
                        instanceId = obj.getString("instanceId"),
                        category = cat,
                        message = obj.getString("message"),
                        formattedDate = obj.getString("formattedDate"),
                        appVersion = obj.optString("appVersion", "1.0"),
                        androidRelease = obj.optString("androidRelease", "14"),
                        deviceModel = obj.optString("deviceModel", "Android"),
                        enrolledProgramme = obj.optString("enrolledProgramme", "")
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun serializeFeedbackJson(items: List<AnonymousFeedback>): JSONArray {
        val array = JSONArray()
        items.forEach { item ->
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("instanceId", item.instanceId)
            obj.put("category", item.category.name)
            obj.put("message", item.message)
            obj.put("formattedDate", item.formattedDate)
            obj.put("appVersion", item.appVersion)
            obj.put("androidRelease", item.androidRelease)
            obj.put("deviceModel", item.deviceModel)
            obj.put("enrolledProgramme", item.enrolledProgramme)
            array.put(obj)
        }
        return array
    }
}
