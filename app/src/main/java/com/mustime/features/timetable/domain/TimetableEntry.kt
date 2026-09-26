package com.mustime.features.timetable.domain

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "timetable",
    indices = [
        Index(value = ["program_group", "start_time"]),
        Index(value = ["day", "start_time"]),
        Index(value = ["course_code"])
    ]
)
data class TimetableEntry(
    @PrimaryKey
    val natural_key: String,
    val program_group: String,
    val day: String,
    val time_slot: String,
    val start_time: String,
    val end_time: String?,
    val course_code: String,
    val course_title: String,
    val session_type: String?,
    val lecturer: String?,
    val room: String?,
    val shared_with: List<String> = emptyList()
) {
    val id: Long get() = (natural_key.hashCode().toLong() and 0x7FFFFFFFL)
    val naturalKey: String get() = natural_key
    val programmeGroup: String get() = program_group
    val dayOfWeek: String get() = day
    val courseCode: String get() = course_code
    val courseTitle: String get() = course_title
    val startTime: String get() = start_time
    val endTime: String
        get() {
            if (!end_time.isNullOrBlank()) return end_time
            if (time_slot.contains("-")) {
                val candidate = time_slot.substringAfter("-").trim()
                if (candidate.isNotEmpty()) {
                    return if (candidate.length == 4 && candidate[1] == ':') "0$candidate" else candidate
                }
            }
            // If still missing, derive 1 hour after start_time
            if (start_time.contains(":")) {
                val parts = start_time.split(":")
                val h = parts[0].trim().toIntOrNull() ?: return ""
                val m = parts[1].trim().toIntOrNull() ?: 0
                val totalMin = h * 60 + m + 60
                return String.format(java.util.Locale.US, "%02d:%02d", totalMin / 60, totalMin % 60)
            }
            return ""
        }
    val sessionType: String? get() = session_type
    val timeSlot: String get() = time_slot
    val sharedWithRaw: String get() = shared_with.joinToString(",")
    val draftVersion: String get() = "FINAL"
    val lastSyncedAt: Long get() = System.currentTimeMillis()
}

