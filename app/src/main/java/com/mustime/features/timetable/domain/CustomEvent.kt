package com.mustime.features.timetable.domain

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "custom_events",
    indices = [
        Index(value = ["dayOfWeek", "startTime"])
    ]
)
data class CustomEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val dayOfWeek: String,
    val startTime: String,
    val endTime: String,
    val location: String = "",
    val notes: String = "",
    val repeatWeekly: Boolean = true,
    val alarmMinutes: Int? = null,
    val category: String = "Study",
    val colorTag: String = "#2563EB"
)

