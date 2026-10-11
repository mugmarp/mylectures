package com.mustime.features.timetable.domain

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TimetableStage {
    DRAFT,
    FINAL
}

@Entity(tableName = "timetable_metadata")
data class TimetableMetadata(
    @PrimaryKey val id: Int = 1,
    val versionLabel: String,         // e.g. "Draft 1", "Draft 2", "Draft 3", "Final Timetable"
    val status: String,               // e.g. "Active Draft", "Official Final", "Approved Final"
    val isFinal: Boolean = false,     // true when Draft phase completes and Final Timetable is released
    val draftNumber: Int? = null,     // null when isFinal = true; 1, 2, 3... when isFinal = false
    val academicYear: String = "2026/2027",
    val semester: String = "Semester 1",
    val releaseNotes: String? = null,
    val lastUpdated: Long = System.currentTimeMillis(),
    val source: String = "MUST Academic Registrar",
    val totalSessions: Int = 1304
) {
    val stage: TimetableStage
        get() = if (isFinal) TimetableStage.FINAL else TimetableStage.DRAFT

    val displayBadge: String
        get() = when {
            isFinal -> "FINAL"
            draftNumber != null -> "DRAFT $draftNumber"
            versionLabel.contains("Draft", ignoreCase = true) -> versionLabel.uppercase()
            else -> status.uppercase()
        }

    val displaySummary: String
        get() = when {
            isFinal -> "$versionLabel · Official Release"
            draftNumber != null -> "Draft $draftNumber · Synced ($status)"
            else -> "$versionLabel · Synced ($status)"
        }
}

