package com.mustime.core.sync

import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.mustime.core.database.AppDatabase
import com.mustime.features.timetable.domain.Assignment
import com.mustime.features.timetable.domain.CustomEvent
import com.mustime.features.timetable.domain.LectureNote
import com.mustime.features.timetable.domain.TimetableEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class SyncRepository(
    private val db: AppDatabase,
    private val firestore: FirebaseFirestore? = try {
        FirebaseFirestore.getInstance()
    } catch (e: Throwable) {
        null
    }
) {
    private val listeners = mutableListOf<ListenerRegistration>()

    /**
     * Starts observation of the central university timetable and version metadata.
     * Operates for all students without requiring personal account sign-in.
     */
    fun startTimetableSync(scope: CoroutineScope) {
        val store = firestore ?: return

        // 1. Sync global Timetable collection (Read-Only)
        listeners.add(
            store.collection("timetable")
                .addSnapshotListener { snap, _ ->
                    val changes = snap?.documentChanges ?: return@addSnapshotListener
                    val toUpsert = mutableListOf<TimetableEntry>()
                    val toRemove = mutableListOf<String>()

                    changes.forEach { change ->
                        if (change.document.id == "metadata") return@forEach
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

        // 1b. Sync dynamic Timetable Release & Version Metadata (Read-Only)
        listeners.add(
            store.collection("timetable").document("metadata")
                .addSnapshotListener { doc, _ ->
                    if (doc != null && doc.exists()) {
                        val versionLabel = doc.getString("version_label") ?: doc.getString("version") ?: "Final Timetable"
                        val isFinal = doc.getBoolean("is_final") ?: (doc.getString("status")?.equals("FINAL", ignoreCase = true) == true)
                        val acadYear = doc.getString("academic_year") ?: "2026/2027"
                        val semester = doc.getString("semester") ?: "Semester 1"
                        val releaseNotes = doc.getString("release_notes")
                        val totalSessions = doc.getLong("total_sessions")?.toInt() ?: 1304

                        val meta = com.mustime.features.timetable.domain.TimetableVersionResolver.resolve(
                            rawVersion = versionLabel,
                            isExplicitFinal = isFinal,
                            academicYear = acadYear,
                            semester = semester,
                            releaseNotes = releaseNotes,
                            totalEntries = totalSessions
                        )
                        scope.launch(Dispatchers.IO) {
                            db.timetableDao().upsertTimetableMetadata(meta)
                        }
                    }
                }
        )
    }

    /**
     * Actively fetches the latest timetable release metadata from Firestore on demand.
     */
    suspend fun fetchRemoteTimetableMetadata(): com.mustime.features.timetable.domain.TimetableMetadata? {
        val store = firestore ?: return null
        return try {
            val doc = store.collection("timetable").document("metadata").get().await()
            if (doc != null && doc.exists()) {
                val versionLabel = doc.getString("version_label") ?: doc.getString("version") ?: "Final Timetable"
                val isFinal = doc.getBoolean("is_final") ?: (doc.getString("status")?.equals("FINAL", ignoreCase = true) == true)
                val acadYear = doc.getString("academic_year") ?: "2026/2027"
                val semester = doc.getString("semester") ?: "Semester 1"
                val releaseNotes = doc.getString("release_notes")
                val totalSessions = doc.getLong("total_sessions")?.toInt() ?: 1304

                com.mustime.features.timetable.domain.TimetableVersionResolver.resolve(
                    rawVersion = versionLabel,
                    isExplicitFinal = isFinal,
                    academicYear = acadYear,
                    semester = semester,
                    releaseNotes = releaseNotes,
                    totalEntries = totalSessions
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Performs a full bi-directional synchronization pass for all user data:
     * - Lecture Notes
     * - Custom Events
     * - Coursework Assignments
     * - User Settings / Preferences
     *
     * Safely executes an Additive Union with Last-Write-Wins and non-destructive note preservation.
     */
    suspend fun performFullUserSync(
        userId: String,
        settingsManager: UserSettingsSyncManager
    ): SyncMergeStats = withContext(Dispatchers.IO) {
        // 1. Promote any un-scoped local Guest data to this authenticated user account
        db.timetableDao().promoteGuestNotes(userId)
        db.customEventDao().promoteGuestData(userId)
        db.assignmentDao().promoteGuestData(userId)

        // 2. Load strictly user-scoped records from Room SQLite
        val localNotes = db.timetableDao().getAllNotesRawSyncForUser(userId)
        val localEvents = db.customEventDao().getAllRawSyncForUser(userId)
        val localAssignments = db.assignmentDao().getAllRawSyncForUser(userId)
        val localSettings = settingsManager.exportCurrentSettings()

        val store = firestore ?: return@withContext SyncMergeStats(
            notesCount = localNotes.count { !it.isDeleted },
            eventsCount = localEvents.count { !it.isDeleted },
            assignmentsCount = localAssignments.count { !it.isDeleted }
        )

        val userDoc = store.collection("users").document(userId)

        try {
            // 3. Fetch Remote Notes
            val remoteNotesSnap = userDoc.collection("notes").get().await()
            val remoteNotes = remoteNotesSnap.documents.mapNotNull { it.toLectureNote() }

            // 4. Fetch Remote Custom Events
            val remoteEventsSnap = userDoc.collection("events").get().await()
            val remoteEvents = remoteEventsSnap.documents.mapNotNull { it.toCustomEvent() }

            // 5. Fetch Remote Assignments
            val remoteAssignmentsSnap = userDoc.collection("assignments").get().await()
            val remoteAssignments = remoteAssignmentsSnap.documents.mapNotNull { it.toAssignment() }

            // 6. Fetch Remote Settings
            val remoteSettingsDoc = userDoc.collection("settings").document("preferences").get().await()
            val remoteSettings = if (remoteSettingsDoc.exists() && remoteSettingsDoc.data != null) {
                UserSettingsSyncPayload.fromMap(remoteSettingsDoc.data!!)
            } else null

            // 7. Non-destructive merge Notes (keeping slot naturalKey intact with conflict drafts)
            val noteMerge = SyncConflictResolver.mergeNotes(localNotes, remoteNotes)
            db.timetableDao().upsertNotes(noteMerge.mergedList)
            noteMerge.toPushToRemote.forEach { note ->
                pushNoteToCloud(note, userId)
            }

            // 8. Merge Events
            val eventMerge = SyncConflictResolver.mergeEvents(localEvents, remoteEvents)
            db.customEventDao().upsertAll(eventMerge.mergedList)
            eventMerge.toPushToRemote.forEach { event ->
                pushEventToCloud(event, userId)
            }

            // 9. Merge Assignments
            val assignmentMerge = SyncConflictResolver.mergeAssignments(localAssignments, remoteAssignments)
            db.assignmentDao().upsertAll(assignmentMerge.mergedList)
            assignmentMerge.toPushToRemote.forEach { assignment ->
                pushAssignmentToCloud(assignment, userId)
            }

            // 10. Merge Settings
            val (resolvedSettings, shouldPushSettings) = SyncConflictResolver.mergeSettings(localSettings, remoteSettings)
            settingsManager.applySettings(resolvedSettings)
            if (shouldPushSettings) {
                pushSettingsToCloud(resolvedSettings, userId)
            }

            // 11. Vacuum old tombstones (> 30 days old) to prevent unbounded SQLite and memory growth
            val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000L)
            db.timetableDao().purgeNoteTombstones(thirtyDaysAgo)
            db.customEventDao().purgeTombstones(thirtyDaysAgo)
            db.assignmentDao().purgeTombstones(thirtyDaysAgo)

            val totalPushed = noteMerge.toPushToRemote.size + eventMerge.toPushToRemote.size + assignmentMerge.toPushToRemote.size + (if (shouldPushSettings) 1 else 0)
            val totalPulled = remoteNotes.size + remoteEvents.size + remoteAssignments.size

            SyncMergeStats(
                pushedCount = totalPushed,
                pulledCount = totalPulled,
                conflictsResolvedCount = noteMerge.conflictsForkedCount,
                notesCount = noteMerge.mergedList.count { !it.isDeleted },
                eventsCount = eventMerge.mergedList.count { !it.isDeleted },
                assignmentsCount = assignmentMerge.mergedList.count { !it.isDeleted }
            )
        } catch (e: Exception) {
            SyncMergeStats(
                notesCount = localNotes.count { !it.isDeleted },
                eventsCount = localEvents.count { !it.isDeleted },
                assignmentsCount = localAssignments.count { !it.isDeleted }
            )
        }
    }

    /**
     * Starts continuous real-time sync for a signed-in user across devices.
     */
    fun startAllSync(userId: String, scope: CoroutineScope) {
        val store = firestore ?: return
        stopAllSync()
        startTimetableSync(scope)

        val userDoc = store.collection("users").document(userId)

        // Notes Listener
        listeners.add(
            userDoc.collection("notes")
                .addSnapshotListener { snap, _ ->
                    val changes = snap?.documentChanges ?: return@addSnapshotListener
                    val toUpsert = mutableListOf<LectureNote>()
                    changes.forEach { change ->
                        val note = change.document.toLectureNote() ?: return@forEach
                        toUpsert.add(note)
                    }
                    if (toUpsert.isNotEmpty()) {
                        scope.launch(Dispatchers.IO) {
                            db.timetableDao().upsertNotes(toUpsert)
                        }
                    }
                }
        )

        // Events Listener
        listeners.add(
            userDoc.collection("events")
                .addSnapshotListener { snap, _ ->
                    val changes = snap?.documentChanges ?: return@addSnapshotListener
                    val toUpsert = mutableListOf<CustomEvent>()
                    changes.forEach { change ->
                        val event = change.document.toCustomEvent() ?: return@forEach
                        toUpsert.add(event)
                    }
                    if (toUpsert.isNotEmpty()) {
                        scope.launch(Dispatchers.IO) {
                            db.customEventDao().upsertAll(toUpsert)
                        }
                    }
                }
        )

        // Assignments Listener
        listeners.add(
            userDoc.collection("assignments")
                .addSnapshotListener { snap, _ ->
                    val changes = snap?.documentChanges ?: return@addSnapshotListener
                    val toUpsert = mutableListOf<Assignment>()
                    changes.forEach { change ->
                        val assignment = change.document.toAssignment() ?: return@forEach
                        toUpsert.add(assignment)
                    }
                    if (toUpsert.isNotEmpty()) {
                        scope.launch(Dispatchers.IO) {
                            db.assignmentDao().upsertAll(toUpsert)
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
            "natural_key" to note.naturalKey,
            "title" to note.title,
            "content" to note.content,
            "colour_tag" to note.colourTag,
            "alarm_minutes" to note.alarmMinutes,
            "tag" to note.tag,
            "attached_class" to note.attachedClass,
            "attachment_name" to note.attachmentName,
            "attachment_path" to note.attachmentPath,
            "is_pinned" to note.isPinned,
            "is_deleted" to note.isDeleted,
            "owner_id" to (note.ownerId ?: userId),
            "updated_at" to note.updatedAt
        )
        try {
            store.collection("users").document(userId)
                .collection("notes").document(note.naturalKey)
                .set(noteMap, SetOptions.merge())
                .await()
        } catch (_: Exception) { /* offline queue */ }
    }

    suspend fun pushEventToCloud(event: CustomEvent, userId: String) {
        val store = firestore ?: return
        val eventMap = mapOf(
            "client_uuid" to event.clientUuid,
            "title" to event.title,
            "day_of_week" to event.dayOfWeek,
            "start_time" to event.startTime,
            "end_time" to event.endTime,
            "location" to event.location,
            "notes" to event.notes,
            "repeat_weekly" to event.repeatWeekly,
            "alarm_minutes" to event.alarmMinutes,
            "category" to event.category,
            "color_tag" to event.colorTag,
            "owner_id" to (event.ownerId ?: userId),
            "updated_at" to event.updatedAt,
            "is_deleted" to event.isDeleted
        )
        try {
            store.collection("users").document(userId)
                .collection("events").document(event.clientUuid)
                .set(eventMap, SetOptions.merge())
                .await()
        } catch (_: Exception) { /* offline queue */ }
    }

    suspend fun pushAssignmentToCloud(assignment: Assignment, userId: String) {
        val store = firestore ?: return
        val assignmentMap = mapOf(
            "client_uuid" to assignment.clientUuid,
            "title" to assignment.title,
            "course_code" to assignment.courseCode,
            "due_date" to assignment.dueDate,
            "reminder_minutes" to assignment.reminderMinutes,
            "priority" to assignment.priority,
            "notes" to assignment.notes,
            "completed" to assignment.completed,
            "owner_id" to (assignment.ownerId ?: userId),
            "updated_at" to assignment.updatedAt,
            "is_deleted" to assignment.isDeleted
        )
        try {
            store.collection("users").document(userId)
                .collection("assignments").document(assignment.clientUuid)
                .set(assignmentMap, SetOptions.merge())
                .await()
        } catch (_: Exception) { /* offline queue */ }
    }

    suspend fun pushSettingsToCloud(settings: UserSettingsSyncPayload, userId: String) {
        val store = firestore ?: return
        try {
            store.collection("users").document(userId)
                .collection("settings").document("preferences")
                .set(settings.toMap(), SetOptions.merge())
                .await()
        } catch (_: Exception) { /* offline queue */ }
    }

    /**
     * Cleanly purges an account's personal data from local SQLite upon explicit sign-out,
     * preventing cross-user data leakage when another user signs in on the same device.
     */
    suspend fun purgeUserDataOnSignOut(userId: String) = withContext(Dispatchers.IO) {
        db.timetableDao().purgeUserNotes(userId)
        db.customEventDao().purgeUserData(userId)
        db.assignmentDao().purgeUserData(userId)
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
            shared_with = sharedList,
            floor = getLong("floor")?.toInt() ?: com.mustime.features.rooms.UniversityDirectory.resolveLevel(getString("room"))?.floorNumber
        )
    } catch (e: Exception) { null }
}

private fun com.google.firebase.firestore.DocumentSnapshot.toLectureNote(): LectureNote? {
    return try {
        LectureNote(
            naturalKey = getString("natural_key") ?: id,
            title = getString("title"),
            content = getString("content") ?: "",
            updatedAt = getLong("updated_at") ?: getTimestamp("updated_at")?.toDate()?.time ?: System.currentTimeMillis(),
            colourTag = getString("colour_tag"),
            alarmMinutes = getLong("alarm_minutes")?.toInt(),
            tag = getString("tag"),
            attachedClass = getString("attached_class"),
            attachmentName = getString("attachment_name"),
            attachmentPath = getString("attachment_path"),
            isPinned = getBoolean("is_pinned") ?: false,
            isDeleted = getBoolean("is_deleted") ?: false,
            ownerId = getString("owner_id")
        )
    } catch (e: Exception) { null }
}

private fun com.google.firebase.firestore.DocumentSnapshot.toCustomEvent(): CustomEvent? {
    return try {
        CustomEvent(
            clientUuid = getString("client_uuid") ?: id,
            title = getString("title") ?: return null,
            dayOfWeek = getString("day_of_week") ?: "Monday",
            startTime = getString("start_time") ?: "08:00",
            endTime = getString("end_time") ?: "09:00",
            location = getString("location") ?: "",
            notes = getString("notes") ?: "",
            repeatWeekly = getBoolean("repeat_weekly") ?: true,
            alarmMinutes = getLong("alarm_minutes")?.toInt(),
            category = getString("category") ?: "Study",
            colorTag = getString("color_tag") ?: "#2563EB",
            updatedAt = getLong("updated_at") ?: System.currentTimeMillis(),
            isDeleted = getBoolean("is_deleted") ?: false,
            ownerId = getString("owner_id")
        )
    } catch (e: Exception) { null }
}

private fun com.google.firebase.firestore.DocumentSnapshot.toAssignment(): Assignment? {
    return try {
        Assignment(
            clientUuid = getString("client_uuid") ?: id,
            title = getString("title") ?: return null,
            courseCode = getString("course_code") ?: "",
            dueDate = getString("due_date") ?: "",
            reminderMinutes = getLong("reminder_minutes")?.toInt(),
            priority = getString("priority") ?: "Medium",
            notes = getString("notes") ?: "",
            completed = getBoolean("completed") ?: false,
            updatedAt = getLong("updated_at") ?: System.currentTimeMillis(),
            isDeleted = getBoolean("is_deleted") ?: false,
            ownerId = getString("owner_id")
        )
    } catch (e: Exception) { null }
}
