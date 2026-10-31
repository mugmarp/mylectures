package com.mustime.features.timetable.domain

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

import java.util.UUID

@Entity(
    tableName = "assignments",
    indices = [
        Index(value = ["completed"]),
        Index(value = ["dueDate"]),
        Index(value = ["courseCode"]),
        Index(value = ["clientUuid"], unique = true),
        Index(value = ["ownerId"])
    ]
)
data class Assignment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientUuid: String = UUID.randomUUID().toString(),
    val title: String,
    val courseCode: String,
    val dueDate: String,
    val reminderMinutes: Int? = null,
    val priority: String = "Medium",
    val notes: String = "",
    val completed: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val ownerId: String? = null
)

