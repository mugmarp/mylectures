package com.mustime.features.timetable.domain

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

import java.util.UUID

@Entity(
    tableName = "custom_events",
    indices = [
        Index(value = ["dayOfWeek", "startTime"]),
        Index(value = ["clientUuid"], unique = true),
        Index(value = ["ownerId"])
    ]
)
data class CustomEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientUuid: String = UUID.randomUUID().toString(),
    val title: String,
    val dayOfWeek: String,
    val startTime: String,
    val endTime: String,
    val location: String = "",
    val notes: String = "",
    val repeatWeekly: Boolean = true,
    val alarmMinutes: Int? = null,
    val category: String = "Study",
    val colorTag: String = "#2563EB",
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val ownerId: String? = null
)

