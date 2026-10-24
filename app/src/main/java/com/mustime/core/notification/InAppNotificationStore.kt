package com.mustime.core.notification

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class NotificationItemType {
    CLASS_ALARM,
    CLASS_REMINDER,
    TASK_REMINDER,
    DAILY_DIGEST,
    SYSTEM
}

data class InAppNotificationItem(
    val id: String = UUID.randomUUID().toString(),
    val type: NotificationItemType,
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val relatedKey: String? = null,
    val navTab: Int? = null
)

class InAppNotificationStore(private val context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("mustime_inapp_notifications", Context.MODE_PRIVATE)

    private val _notifications = MutableStateFlow<List<InAppNotificationItem>>(loadSavedNotifications())
    val notifications = _notifications.asStateFlow()

    val unreadCount: Flow<Int> = _notifications.map { list -> list.count { !it.isRead } }

    private fun loadSavedNotifications(): List<InAppNotificationItem> {
        val raw = prefs.getString(KEY_NOTIFICATIONS, null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(raw)
            val list = mutableListOf<InAppNotificationItem>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val typeStr = obj.optString("type", NotificationItemType.SYSTEM.name)
                val type = try {
                    NotificationItemType.valueOf(typeStr)
                } catch (_: Exception) {
                    NotificationItemType.SYSTEM
                }
                list.add(
                    InAppNotificationItem(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        type = type,
                        title = obj.optString("title", ""),
                        message = obj.optString("message", ""),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        isRead = obj.optBoolean("isRead", false),
                        relatedKey = if (obj.has("relatedKey") && !obj.isNull("relatedKey")) obj.optString("relatedKey") else null,
                        navTab = if (obj.has("navTab") && !obj.isNull("navTab")) obj.optInt("navTab") else null
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun persist(list: List<InAppNotificationItem>) {
        val limited = list.take(100) // Keep latest 100 entries
        _notifications.value = limited
        try {
            val jsonArray = JSONArray()
            for (item in limited) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("type", item.type.name)
                    put("title", item.title)
                    put("message", item.message)
                    put("timestamp", item.timestamp)
                    put("isRead", item.isRead)
                    item.relatedKey?.let { put("relatedKey", it) }
                    item.navTab?.let { put("navTab", it) }
                }
                jsonArray.put(obj)
            }
            prefs.edit().putString(KEY_NOTIFICATIONS, jsonArray.toString()).apply()
        } catch (_: Exception) {}
    }

    fun recordNotification(
        type: NotificationItemType,
        title: String,
        message: String,
        relatedKey: String? = null,
        navTab: Int? = null
    ): InAppNotificationItem {
        val item = InAppNotificationItem(
            type = type,
            title = title,
            message = message,
            timestamp = System.currentTimeMillis(),
            isRead = false,
            relatedKey = relatedKey,
            navTab = navTab
        )
        val current = _notifications.value.toMutableList()
        current.add(0, item)
        persist(current)

        // Also emit for real-time in-app foreground banner
        _foregroundAlertFlow.tryEmit(item)
        return item
    }

    fun markAsRead(id: String) {
        val updated = _notifications.value.map {
            if (it.id == id) it.copy(isRead = true) else it
        }
        persist(updated)
    }

    fun markAllAsRead() {
        val updated = _notifications.value.map { it.copy(isRead = true) }
        persist(updated)
    }

    fun deleteNotification(id: String) {
        val updated = _notifications.value.filter { it.id != id }
        persist(updated)
    }

    fun clearAll() {
        persist(emptyList())
    }

    companion object {
        private const val KEY_NOTIFICATIONS = "notifications_json"

        @Volatile
        private var INSTANCE: InAppNotificationStore? = null

        fun getInstance(context: Context): InAppNotificationStore {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: InAppNotificationStore(context.applicationContext).also { INSTANCE = it }
            }
        }

        // Shared across instances for instant in-app heads-up alerts
        private val _foregroundAlertFlow = MutableSharedFlow<InAppNotificationItem>(
            replay = 0,
            extraBufferCapacity = 5
        )
        val foregroundAlertFlow = _foregroundAlertFlow.asSharedFlow()
    }
}
