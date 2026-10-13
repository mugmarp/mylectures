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
    /**
     * Relative path of the copied attachment inside the app's private
     * `files/attachments/` directory (e.g. `attachments/1234_slides.pdf`).
     *
     * The picked content URI is copied into internal storage on selection so the
     * file survives the picker's transient read grant; only this stable path is
     * persisted. Null for legacy name-only rows.
     */
    val attachmentPath: String? = null,
    val isPinned: Boolean = false
)
