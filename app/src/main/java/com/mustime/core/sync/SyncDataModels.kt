package com.mustime.core.sync

/**
 * High-level status of the cloud sync engine for user account data.
 */
enum class SyncState {
    IDLE,
    SYNCING,
    SUCCESS,
    ERROR
}

/**
 * Account authentication status representation.
 */
data class SyncAccountInfo(
    val userId: String? = null,
    val email: String? = null,
    val displayName: String? = null,
    val isCloudSyncEnabled: Boolean = false,
    val lastSyncTimestamp: Long? = null
)

/**
 * Summary metrics of a sync / merge pass.
 */
data class SyncMergeStats(
    val pushedCount: Int = 0,
    val pulledCount: Int = 0,
    val conflictsResolvedCount: Int = 0,
    val notesCount: Int = 0,
    val eventsCount: Int = 0,
    val assignmentsCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * User Settings payload for Firestore `/users/{uid}/settings/preferences`.
 */
data class UserSettingsSyncPayload(
    val programme: String? = null,
    val themeMode: String = "LIGHT",
    val themeAccent: Int = 0,
    val notificationsEnabled: Boolean = true,
    val classAlarmLeadMinutes: Int = 30,
    val taskReminderLeadHours: Int = 2,
    val alarmVibration: Boolean = true,
    val alarmSound: String = "Chime",
    val academicYear: String = "2026/2027",
    val semester: String = "Semester 1",
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "programme" to programme,
        "theme_mode" to themeMode,
        "theme_accent" to themeAccent,
        "notifications_enabled" to notificationsEnabled,
        "class_alarm_lead_minutes" to classAlarmLeadMinutes,
        "task_reminder_lead_hours" to taskReminderLeadHours,
        "alarm_vibration" to alarmVibration,
        "alarm_sound" to alarmSound,
        "academic_year" to academicYear,
        "semester" to semester,
        "updated_at" to updatedAt
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): UserSettingsSyncPayload {
            return UserSettingsSyncPayload(
                programme = map["programme"] as? String,
                themeMode = (map["theme_mode"] as? String) ?: "LIGHT",
                themeAccent = (map["theme_accent"] as? Number)?.toInt() ?: 0,
                notificationsEnabled = (map["notifications_enabled"] as? Boolean) ?: true,
                classAlarmLeadMinutes = (map["class_alarm_lead_minutes"] as? Number)?.toInt() ?: 30,
                taskReminderLeadHours = (map["task_reminder_lead_hours"] as? Number)?.toInt() ?: 2,
                alarmVibration = (map["alarm_vibration"] as? Boolean) ?: true,
                alarmSound = (map["alarm_sound"] as? String) ?: "Chime",
                academicYear = (map["academic_year"] as? String) ?: "2026/2027",
                semester = (map["semester"] as? String) ?: "Semester 1",
                updatedAt = (map["updated_at"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }
}
