package com.mustime.features.timetable.domain

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "lecture_notes",
    indices = [
        Index(value = ["updatedAt"])
    ]
)
data class LectureNote(
    @PrimaryKey val naturalKey: String,
    val content: String,
    val updatedAt: Long,
    val colourTag: String? = null,
    val alarmMinutes: Int? = null,
    val title: String? = null,
    val tag: String? = null,
    val attachedClass: String? = null,
    val attachmentName: String? = null,
    val isPinned: Boolean = false
)
