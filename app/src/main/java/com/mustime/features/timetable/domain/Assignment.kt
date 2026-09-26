package com.mustime.features.timetable.domain

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "assignments",
    indices = [
        Index(value = ["completed"]),
        Index(value = ["dueDate"]),
        Index(value = ["courseCode"])
    ]
)
data class Assignment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val courseCode: String,
    val dueDate: String,
    val reminderMinutes: Int? = null,
    val priority: String = "Medium",
    val notes: String = "",
    val completed: Boolean = false
)

