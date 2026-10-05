package com.mustime.features.timetable.data

import android.content.Context
import android.util.JsonReader
import android.util.JsonToken
import androidx.room.withTransaction
import com.mustime.core.database.AppDatabase
import com.mustime.features.timetable.domain.Assignment
import com.mustime.features.timetable.domain.LectureNote
import com.mustime.features.timetable.domain.TimetableEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

class DataLoader(private val context: Context, private val db: AppDatabase) {

    suspend fun loadInitialDataIfNeeded() {
        withContext(Dispatchers.IO) {
            val dao = db.timetableDao()
            val assignmentDao = db.assignmentDao()

            val entryCount = dao.getEntryCount()

            if (entryCount < 1000) {
                val entries = loadFromAssets()
                if (entries.isNotEmpty()) {
                    db.withTransaction {
                        if (dao.getEntryCount() > 0) {
                            dao.deleteAllEntries()
                        }
                        dao.insertAll(entries)
                    }
                }
            }

            // Seed initial note if none exist
            if (dao.getNotesWithAlarms().isEmpty()) {
                val sampleNote = LectureNote(
                    naturalKey = "GEN101|University Studies|Monday|09:00|sample1",
                    title = "Welcome to Lectures - Semester Study Guide",
                    content = "Keep track of your course schedules, lecture notes, revision tasks, and assignment deadlines all in one place.\n• Attach your notes to your scheduled timetable lectures.\n• Set customizable pre-class alerts so you never miss a lecture.\n• Use markdown formatting and attach key files or voice recordings.",
                    updatedAt = System.currentTimeMillis() - 3600000L,
                    colourTag = "#2563EB",
                    alarmMinutes = 30,
                    tag = "Study Guide",
                    attachedClass = "GEN101: University Studies · Monday, 09:00 – 11:00",
                    isPinned = true
                )
                dao.upsertNote(sampleNote)
            }

            // Seed initial assignments if none exist
            if (assignmentDao.getCount() == 0) {
                assignmentDao.upsert(
                    Assignment(
                        id = 0,
                        title = "Semester Coursework Assignment",
                        courseCode = "GEN101",
                        dueDate = "Friday, 17:00",
                        reminderMinutes = 60,
                        priority = "Medium",
                        notes = "Check course outline and submit deliverables before the deadline.",
                        completed = false
                    )
                )
            }
        }
    }

    suspend fun ensureDataForGroup(group: String) {
        loadInitialDataIfNeeded()
    }

    fun loadFromAssets(): List<TimetableEntry> {
        val list = ArrayList<TimetableEntry>(1350)
        try {
            context.assets.open("timetable_export.json").use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8), 32768).use { br ->
                    val reader = JsonReader(br)
                    reader.beginArray()
                    while (reader.hasNext()) {
                        reader.beginObject()
                        var naturalKey = ""
                        var programGroup = ""
                        var day = ""
                        var timeSlot = ""
                        var startTime = ""
                        var endTime: String? = null
                        var courseCode = ""
                        var courseTitle = ""
                        var sessionType: String? = null
                        var lecturer: String? = null
                        var room: String? = null
                        val sharedWith = mutableListOf<String>()

                        while (reader.hasNext()) {
                            when (reader.nextName()) {
                                "natural_key" -> naturalKey = reader.nextString()
                                "program_group" -> programGroup = reader.nextString()
                                "day" -> day = reader.nextString()
                                "time_slot" -> timeSlot = reader.nextString()
                                "start_time" -> startTime = reader.nextString()
                                "end_time" -> {
                                    if (reader.peek() == JsonToken.NULL) {
                                        reader.nextNull()
                                    } else {
                                        endTime = reader.nextString()
                                    }
                                }
                                "course_code" -> courseCode = reader.nextString()
                                "course_title" -> courseTitle = reader.nextString()
                                "session_type" -> {
                                    if (reader.peek() == JsonToken.NULL) {
                                        reader.nextNull()
                                    } else {
                                        sessionType = reader.nextString()
                                    }
                                }
                                "lecturer" -> {
                                    if (reader.peek() == JsonToken.NULL) {
                                        reader.nextNull()
                                    } else {
                                        lecturer = reader.nextString()
                                    }
                                }
                                "room" -> {
                                    if (reader.peek() == JsonToken.NULL) {
                                        reader.nextNull()
                                    } else {
                                        room = reader.nextString()
                                    }
                                }
                                "shared_with" -> {
                                    if (reader.peek() == JsonToken.BEGIN_ARRAY) {
                                        reader.beginArray()
                                        while (reader.hasNext()) {
                                            sharedWith.add(reader.nextString())
                                        }
                                        reader.endArray()
                                    } else {
                                        reader.skipValue()
                                    }
                                }
                                else -> reader.skipValue()
                            }
                        }
                        reader.endObject()

                        if (naturalKey.isNotEmpty()) {
                            list.add(
                                TimetableEntry(
                                    natural_key = naturalKey,
                                    program_group = programGroup,
                                    day = day,
                                    time_slot = timeSlot,
                                    start_time = startTime,
                                    end_time = endTime,
                                    course_code = courseCode,
                                    course_title = courseTitle,
                                    session_type = sessionType,
                                    lecturer = lecturer,
                                    room = room,
                                    shared_with = sharedWith,
                                    floor = com.mustime.features.rooms.UniversityDirectory.resolveLevel(room)?.floorNumber
                                )
                            )
                        }
                    }
                    reader.endArray()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}
