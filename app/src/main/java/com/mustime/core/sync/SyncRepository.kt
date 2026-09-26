package com.mustime.core.sync

import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.mustime.core.database.AppDatabase
import com.mustime.features.timetable.domain.LectureNote
import com.mustime.features.timetable.domain.TimetableEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SyncRepository(
    private val db: AppDatabase,
    private val firestore: FirebaseFirestore? = try {
        FirebaseFirestore.getInstance()
    } catch (e: Throwable) {
        null
    }
) {
    private val listeners = mutableListOf<ListenerRegistration>()

    fun startAllSync(userId: String, scope: CoroutineScope) {
        val store = firestore ?: return
        stopAllSync()
        val userDoc = store.collection("users").document(userId)

        // 1. Sync global Timetable collection (Read-Only)
        listeners.add(
            store.collection("timetable")
                .addSnapshotListener { snap, _ ->
                    val changes = snap?.documentChanges ?: return@addSnapshotListener
                    val toUpsert = mutableListOf<TimetableEntry>()
                    val toRemove = mutableListOf<String>()

                    changes.forEach { change ->
                        val entry = change.document.toTimetableEntry() ?: return@forEach
                        when (change.type) {
                            DocumentChange.Type.ADDED,
                            DocumentChange.Type.MODIFIED -> toUpsert.add(entry)
                            DocumentChange.Type.REMOVED -> toRemove.add(entry.naturalKey)
                        }
                    }

                    if (toUpsert.isNotEmpty() || toRemove.isNotEmpty()) {
                        scope.launch(Dispatchers.IO) {
                            if (toUpsert.isNotEmpty()) {
                                db.timetableDao().upsertEntries(toUpsert)
                            }
                            toRemove.forEach { key ->
                                db.timetableDao().deleteByNaturalKey(key)
                            }
                        }
                    }
                }
        )

        // 2. Sync user-specific Lecture Notes (Bi-directional)
        listeners.add(
            userDoc.collection("notes")
                .addSnapshotListener { snap, _ ->
                    val changes = snap?.documentChanges ?: return@addSnapshotListener
                    val toUpsert = mutableListOf<LectureNote>()
                    val toRemove = mutableListOf<String>()

                    changes.forEach { change ->
                        val note = change.document.toLectureNote() ?: return@forEach
                        if (change.type != DocumentChange.Type.REMOVED) {
                            toUpsert.add(note)
                        } else {
                            toRemove.add(note.naturalKey)
                        }
                    }

                    if (toUpsert.isNotEmpty() || toRemove.isNotEmpty()) {
                        scope.launch(Dispatchers.IO) {
                            toUpsert.forEach { db.timetableDao().upsertNote(it) }
                            toRemove.forEach { db.timetableDao().deleteNote(it) }
                        }
                    }
                }
        )
    }

    fun stopAllSync() {
        listeners.forEach { it.remove() }
        listeners.clear()
    }

    suspend fun pushNoteToCloud(note: LectureNote, userId: String) {
        val store = firestore ?: return
        val noteMap = mapOf(
            "content" to note.content,
            "alarm_minutes" to note.alarmMinutes,
            "updated_at" to FieldValue.serverTimestamp()
        )
        try {
            store.collection("users").document(userId)
                .collection("notes").document(note.naturalKey)
                .set(noteMap, SetOptions.merge())
                .await()
        } catch (e: Exception) { /* queued offline */ }
    }
}

private fun com.google.firebase.firestore.DocumentSnapshot.toTimetableEntry(): TimetableEntry? {
    return try {
        val sharedWithRaw = get("shared_with") as? List<*>
        val sharedList = sharedWithRaw?.mapNotNull { it?.toString() } ?: emptyList()
        TimetableEntry(
            natural_key = id,
            program_group = getString("program_group") ?: return null,
            day = getString("day") ?: return null,
            time_slot = getString("time_slot") ?: "",
            start_time = getString("start_time") ?: "",
            end_time = getString("end_time"),
            course_code = getString("course_code") ?: return null,
            course_title = getString("course_title") ?: "",
            session_type = getString("session_type"),
            lecturer = getString("lecturer"),
            room = getString("room"),
            shared_with = sharedList
        )
    } catch (e: Exception) { null }
}

private fun com.google.firebase.firestore.DocumentSnapshot.toLectureNote(): LectureNote? {
    return try {
        LectureNote(
            naturalKey = getString("natural_key") ?: id,
            content = getString("content") ?: "",
            updatedAt = getTimestamp("updated_at")?.toDate()?.time ?: System.currentTimeMillis(),
            colourTag = getString("colour_tag"),
            alarmMinutes = getLong("alarm_minutes")?.toInt()
        )
    } catch (e: Exception) { null }
}
