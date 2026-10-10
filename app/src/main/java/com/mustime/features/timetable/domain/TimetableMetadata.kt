package com.mustime.features.timetable.domain

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "timetable_metadata")
data class TimetableMetadata(
    @PrimaryKey val id: Int = 1,
    val versionLabel: String,         // e.g. "Draft 1", "Draft 2", "Draft 3", "Final Timetable"
    val status: String,               // e.g. "Active Draft", "Under Review", "Final Approved"
    val isFinal: Boolean = false,     // true when Draft phase completes and Final Timetable is released
    val academicYear: String = "2026/2027",
    val semester: String = "Semester 1",
    val releaseNotes: String? = null,
    val lastUpdated: Long = System.currentTimeMillis(),
    val source: String = "MUST Academic Registrar"
)
