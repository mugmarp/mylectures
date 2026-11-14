package com.lectures.core.sync

import android.util.Log
import androidx.room.withTransaction
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.lectures.core.database.AppDatabase
import com.lectures.features.timetable.domain.Assignment
import com.lectures.features.timetable.domain.CustomEvent
import com.lectures.features.timetable.domain.LectureNote
import com.lectures.features.timetable.domain.TimetableEntry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class SyncRepository(
    private val db: AppDatabase,
    private val firestore: FirebaseFirestore? = try {
        FirebaseFirestore.getInstance()
    } catch (e: Throwable) {
        null
    },
    private val currentUserIdProvider: () -> String? = {
        runCatching { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid }.getOrNull()
    },
    private val beforeUserDataPurge: suspend (List<Assignment>, List<LectureNote>) -> Unit = { _, _ -> }
) {
    private val sessions = UserSyncSession()
    private val personalDataMutex = Mutex()
    private val fullSyncMutex = Mutex()
    private var userScope: CoroutineScope? = null
    private val listenerLock = Any()
    private val timetableListeners = mutableListOf<ListenerRegistration>()
    private val userListeners = mutableListOf<ListenerRegistration>()
    private var activeUserId: String? = null

    private val _lastSyncError = MutableStateFlow<String?>(null)
    val lastSyncError: StateFlow<String?> = _lastSyncError.asStateFlow()

    /**
     * Starts observation of the central university timetable and version metadata.
     * Idempotent: retains the existing public listeners when already running.
     */
    fun startTimetableSync(scope: CoroutineScope) {
        synchronized(listenerLock) {
            val store = firestore ?: return
            if (timetableListeners.isNotEmpty()) return

            // 1. Sync global Timetable collection (Read-Only)
            val entriesListener = store.collection("timetable")
                .addSnapshotListener { snap, error ->
                    if (error != null) {
                        Log.w(TAG, "Timetable snapshot listener error: ${error.message}")
                        _lastSyncError.value = error.localizedMessage ?: error.code.name
                        return@addSnapshotListener
                    }
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

            // 1b. Sync dynamic Timetable Release & Version Metadata (Read-Only)
            val metadataListener = store.collection("timetable").document("metadata")
                .addSnapshotListener { doc, error ->
                    if (error != null) {
                        Log.w(TAG, "Timetable metadata listener error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (doc != null && doc.exists()) {
                        val versionLabel = doc.getString("version_label") ?: doc.getString("version") ?: "Final Timetable"
                        val isFinal = doc.getBoolean("is_final") ?: (doc.getString("status")?.equals("FINAL", ignoreCase = true) == true)
                        val acadYear = doc.getString("academic_year") ?: "2026/2027"
                        val semester = doc.getString("semester") ?: "Semester 1"
                        val releaseNotes = doc.getString("release_notes")
                        val totalSessions = doc.getLong("total_sessions")?.toInt() ?: 1304

                        val meta = com.lectures.features.timetable.domain.TimetableVersionResolver.resolve(
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

            timetableListeners.add(entriesListener)
            timetableListeners.add(metadataListener)
        }
    }

    /**
     * Actively fetches the latest timetable release metadata from Firestore on demand.
     */
    suspend fun fetchRemoteTimetableMetadata(): com.lectures.features.timetable.domain.TimetableMetadata? {
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

                com.lectures.features.timetable.domain.TimetableVersionResolver.resolve(
                    rawVersion = versionLabel,
                    isExplicitFinal = isFinal,
                    academicYear = acadYear,
                    semester = semester,
                    releaseNotes = releaseNotes,
                    totalEntries = totalSessions
                )
            } else null
        } catch (e: Exception) {
            if (e is CancellationException) throw e
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
     * Reports explicit success, partial failure, or hard failure state without silently swallowing errors.
     */
    suspend fun performFullUserSync(
        userId: String,
        settingsManager: UserSettingsSyncManager
    ): SyncMergeStats = withContext(Dispatchers.IO) {
        fullSyncMutex.withLock {
            try {
                withTimeout(30_000L) { performUserSync(userId, settingsManager) }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                SyncMergeStats(status = SyncState.ERROR, failedCount = 1, errorMessage = "Cloud sync timed out. Local data is preserved.")
            }
        }
    }

    private suspend fun performUserSync(userId: String, settingsManager: UserSettingsSyncManager): SyncMergeStats {
        if (userId.isBlank() || currentUserIdProvider() != userId) {
            return SyncMergeStats(status = SyncState.ERROR, errorMessage = "Sign in to the matching account before syncing.")
        }
        val token = beginSession(userId)

        // 1. Promote any un-scoped local Guest data to this authenticated user account
        withSession(token) {
            db.withTransaction {
                db.timetableDao().promoteGuestNotes(userId)
                db.customEventDao().promoteGuestData(userId)
                db.assignmentDao().promoteGuestData(userId)
            }
        }

        // 2. Load strictly user-scoped records from Room SQLite
        val localNotes = db.timetableDao().getAllNotesRawSyncForUser(userId)
        val localEvents = db.customEventDao().getAllRawSyncForUser(userId)
        val localAssignments = db.assignmentDao().getAllRawSyncForUser(userId)

        val store = firestore ?: return SyncMergeStats(
            notesCount = localNotes.count { !it.isDeleted },
            eventsCount = localEvents.count { !it.isDeleted },
            assignmentsCount = localAssignments.count { !it.isDeleted },
            status = SyncState.ERROR,
            errorMessage = "Cloud sync service is unavailable."
        )

        val userDoc = store.collection("users").document(userId)

        return try {
            // 3. Fetch Remote Notes
            val remoteNotesSnap = userDoc.collection("notes").get().await()
            val remoteNotes = remoteNotesSnap.documents.mapNotNull { it.toLectureNote(userId) }

            // 4. Fetch Remote Custom Events
            val remoteEventsSnap = userDoc.collection("events").get().await()
            val remoteEvents = remoteEventsSnap.documents.mapNotNull { it.toCustomEvent(userId) }

            // 5. Fetch Remote Assignments
            val remoteAssignmentsSnap = userDoc.collection("assignments").get().await()
            val remoteAssignments = remoteAssignmentsSnap.documents.mapNotNull { it.toAssignment(userId) }

            // 6. Fetch Remote Settings
            val remoteSettingsDoc = userDoc.collection("settings").document("preferences").get().await()
            val remoteSettings = if (remoteSettingsDoc.exists() && remoteSettingsDoc.data != null) {
                UserSettingsSyncPayload.fromMap(remoteSettingsDoc.data!!)
            } else null

            // 7. Non-destructive merge Notes (keeping slot naturalKey intact with conflict drafts)
            ensureSession(token)
            val noteMerge = withSession(token) {
                db.withTransaction {
                    val owned = db.timetableDao().getAllNotesRawSyncForUser(userId)
                    val safeRemote = remoteNotes.filter { incoming ->
                        val owner = db.timetableDao().getNoteRawDirect(incoming.naturalKey)?.ownerId
                        owner == null || owner == userId
                    }
                    check(safeRemote.size == remoteNotes.size) { "A note slot belongs to another local account. Sign out of that account before syncing this slot." }
                    SyncConflictResolver.mergeNotes(owned, safeRemote).also { result ->
                        db.timetableDao().upsertNotes(result.mergedList.map { it.copy(ownerId = userId) })
                    }
                }
            }

            var pushedSuccess = 0
            var pushFailures = (remoteNotesSnap.size() - remoteNotes.size) +
                (remoteEventsSnap.size() - remoteEvents.size) + (remoteAssignmentsSnap.size() - remoteAssignments.size)
            var firstPushError: String? = if (pushFailures > 0) "Some cloud records could not be read safely." else null

            noteMerge.toPushToRemote.forEach { note ->
                ensureSession(token)
                val result = pushNoteToCloud(note.copy(ownerId = note.ownerId ?: userId), userId)
                if (result.isSuccess) pushedSuccess++ else {
                    pushFailures++
                    if (firstPushError == null) firstPushError = result.exceptionOrNull()?.localizedMessage
                }
            }

            // 8. Merge Events
            val eventMerge = withSession(token) {
                db.withTransaction {
                    check(remoteEvents.all { incoming ->
                        val owner = db.customEventDao().getByUuid(incoming.clientUuid)?.ownerId
                        owner == null || owner == userId
                    }) { "An activity belongs to another local account." }
                    SyncConflictResolver.mergeEvents(db.customEventDao().getAllRawSyncForUser(userId), remoteEvents).also { result ->
                        db.customEventDao().upsertAll(result.mergedList.map { it.copy(ownerId = userId) })
                    }
                }
            }
            eventMerge.toPushToRemote.forEach { event ->
                ensureSession(token)
                val result = pushEventToCloud(event.copy(ownerId = event.ownerId ?: userId), userId)
                if (result.isSuccess) pushedSuccess++ else {
                    pushFailures++
                    if (firstPushError == null) firstPushError = result.exceptionOrNull()?.localizedMessage
                }
            }

            // 9. Merge Assignments
            val assignmentMerge = withSession(token) {
                db.withTransaction {
                    check(remoteAssignments.all { incoming ->
                        val owner = db.assignmentDao().getByUuid(incoming.clientUuid)?.ownerId
                        owner == null || owner == userId
                    }) { "A task belongs to another local account." }
                    SyncConflictResolver.mergeAssignments(db.assignmentDao().getAllRawSyncForUser(userId), remoteAssignments).also { result ->
                        db.assignmentDao().upsertAll(result.mergedList.map { it.copy(ownerId = userId) })
                    }
                }
            }
            assignmentMerge.toPushToRemote.forEach { assignment ->
                ensureSession(token)
                val result = pushAssignmentToCloud(assignment.copy(ownerId = assignment.ownerId ?: userId), userId)
                if (result.isSuccess) pushedSuccess++ else {
                    pushFailures++
                    if (firstPushError == null) firstPushError = result.exceptionOrNull()?.localizedMessage
                }
            }

            // 10. Merge Settings
            val (resolvedSettings, shouldPushSettings) = SyncConflictResolver.mergeSettings(settingsManager.exportCurrentSettings(), remoteSettings)
            withSession(token) { settingsManager.applySettings(resolvedSettings) }
            if (shouldPushSettings) {
                val result = pushSettingsToCloud(resolvedSettings, userId)
                if (result.isSuccess) pushedSuccess++ else {
                    pushFailures++
                    if (firstPushError == null) firstPushError = result.exceptionOrNull()?.localizedMessage
                }
            }

            // 11. Vacuum old tombstones (> 30 days old) only when all pushes succeeded
            if (pushFailures == 0) {
                val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000L)
                withSession(token) {
                    db.withTransaction {
                        db.timetableDao().purgeNoteTombstonesForUser(userId, thirtyDaysAgo)
                        db.customEventDao().purgeTombstonesForUser(userId, thirtyDaysAgo)
                        db.assignmentDao().purgeTombstonesForUser(userId, thirtyDaysAgo)
                    }
                }
            }

            ensureSession(token)
            val totalPulled = remoteNotes.size + remoteEvents.size + remoteAssignments.size
            val finalStatus = if (pushFailures == 0) SyncState.SUCCESS else SyncState.ERROR
            if (pushFailures > 0) {
                _lastSyncError.value = firstPushError ?: "Some items failed to upload to cloud."
            } else {
                _lastSyncError.value = null
            }

            SyncMergeStats(
                pushedCount = pushedSuccess,
                pulledCount = totalPulled,
                conflictsResolvedCount = noteMerge.conflictsForkedCount,
                notesCount = noteMerge.mergedList.count { !it.isDeleted },
                eventsCount = eventMerge.mergedList.count { !it.isDeleted },
                assignmentsCount = assignmentMerge.mergedList.count { !it.isDeleted },
                status = finalStatus,
                failedCount = pushFailures,
                errorMessage = if (pushFailures > 0) (firstPushError ?: "$pushFailures item(s) could not sync to cloud.") else null
            )
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            val errMsg = e.localizedMessage ?: "Cloud sync failed."
            _lastSyncError.value = errMsg
            Log.w(TAG, "performFullUserSync failed: $errMsg", e)
            SyncMergeStats(
                notesCount = localNotes.count { !it.isDeleted },
                eventsCount = localEvents.count { !it.isDeleted },
                assignmentsCount = localAssignments.count { !it.isDeleted },
                status = SyncState.ERROR,
                failedCount = 1,
                errorMessage = errMsg
            )
        }
    }

    /**
     * Starts continuous real-time sync for a signed-in user across devices.
     */
    fun startAllSync(userId: String, scope: CoroutineScope) {
        synchronized(listenerLock) {
            if (userId.isBlank() || currentUserIdProvider() != userId) return
            val store = firestore ?: return
            val token = beginSession(userId)
            if (activeUserId == userId && userListeners.isNotEmpty()) return
            if (timetableListeners.isEmpty()) {
                startTimetableSync(scope)
            }

            val accountScope = CoroutineScope(scope.coroutineContext + SupervisorJob(scope.coroutineContext[Job]))
            userScope?.cancel()
            userScope = accountScope
            activeUserId = userId
            val userDoc = store.collection("users").document(userId)

            val notesListener = userDoc.collection("notes")
                .addSnapshotListener { snap, error ->
                    if (!isSessionCurrent(token)) return@addSnapshotListener
                    if (error != null) {
                        _lastSyncError.value = error.localizedMessage ?: error.code.name
                        return@addSnapshotListener
                    }
                    val changes = snap?.documentChanges?.mapNotNull { change ->
                        val record = change.document.toLectureNote(userId)
                        if (record == null) _lastSyncError.value = "Some cloud records could not be read safely."
                        record?.let { RemoteRecordChange(it, change.type == DocumentChange.Type.REMOVED) }
                    } ?: return@addSnapshotListener
                    launchUserWork(accountScope, token) { applyRemoteNotes(token, changes) }
                }

            val eventsListener = userDoc.collection("events")
                .addSnapshotListener { snap, error ->
                    if (!isSessionCurrent(token)) return@addSnapshotListener
                    if (error != null) {
                        _lastSyncError.value = error.localizedMessage ?: error.code.name
                        return@addSnapshotListener
                    }
                    val changes = snap?.documentChanges?.mapNotNull { change ->
                        val record = change.document.toCustomEvent(userId)
                        if (record == null) _lastSyncError.value = "Some cloud records could not be read safely."
                        record?.let { RemoteRecordChange(it, change.type == DocumentChange.Type.REMOVED) }
                    } ?: return@addSnapshotListener
                    launchUserWork(accountScope, token) { applyRemoteEvents(token, changes) }
                }

            val assignmentsListener = userDoc.collection("assignments")
                .addSnapshotListener { snap, error ->
                    if (!isSessionCurrent(token)) return@addSnapshotListener
                    if (error != null) {
                        _lastSyncError.value = error.localizedMessage ?: error.code.name
                        return@addSnapshotListener
                    }
                    val changes = snap?.documentChanges?.mapNotNull { change ->
                        val record = change.document.toAssignment(userId)
                        if (record == null) _lastSyncError.value = "Some cloud records could not be read safely."
                        record?.let { RemoteRecordChange(it, change.type == DocumentChange.Type.REMOVED) }
                    } ?: return@addSnapshotListener
                    launchUserWork(accountScope, token) { applyRemoteAssignments(token, changes) }
                }

            if (!isSessionCurrent(token)) {
                notesListener.remove()
                eventsListener.remove()
                assignmentsListener.remove()
                accountScope.cancel()
                return
            }
            userListeners.add(notesListener)
            userListeners.add(eventsListener)
            userListeners.add(assignmentsListener)
        }
    }

    fun stopTimetableSync() {
        synchronized(listenerLock) {
            timetableListeners.forEach { it.remove() }
            timetableListeners.clear()
        }
    }

    fun stopUserSync() {
        synchronized(listenerLock) {
            sessions.stop()
            userScope?.cancel()
            userScope = null
            userListeners.forEach { it.remove() }
            userListeners.clear()
            activeUserId = null
        }
    }

    fun stopAllSync() {
        stopTimetableSync()
        stopUserSync()
    }

    suspend fun pushNoteToCloud(note: LectureNote, userId: String): Result<Unit> {
        if (!canPush(userId, note.ownerId)) return Result.failure(SecurityException("Account does not own this record."))
        val store = firestore ?: return Result.failure(IllegalStateException("Firestore unavailable"))
        val noteMap = mapOf(
            "natural_key" to note.naturalKey,
            "title" to note.title,
            "content" to note.content,
            "colour_tag" to note.colourTag,
            "alarm_minutes" to note.alarmMinutes,
            "tag" to note.tag,
            "attached_class" to note.attachedClass,
            "attachment_name" to note.attachmentName,
            "attachment_path" to null,
            "is_pinned" to note.isPinned,
            "is_deleted" to note.isDeleted,
            "owner_id" to (note.ownerId ?: userId),
            "updated_at" to note.updatedAt
        )
        return try {
            store.collection("users").document(userId)
                .collection("notes").document(note.naturalKey)
                .set(noteMap, SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    suspend fun pushEventToCloud(event: CustomEvent, userId: String): Result<Unit> {
        if (!canPush(userId, event.ownerId)) return Result.failure(SecurityException("Account does not own this record."))
        val store = firestore ?: return Result.failure(IllegalStateException("Firestore unavailable"))
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
        return try {
            store.collection("users").document(userId)
                .collection("events").document(event.clientUuid)
                .set(eventMap, SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    suspend fun pushAssignmentToCloud(assignment: Assignment, userId: String): Result<Unit> {
        if (!canPush(userId, assignment.ownerId)) return Result.failure(SecurityException("Account does not own this record."))
        val store = firestore ?: return Result.failure(IllegalStateException("Firestore unavailable"))
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
        return try {
            store.collection("users").document(userId)
                .collection("assignments").document(assignment.clientUuid)
                .set(assignmentMap, SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    suspend fun pushSettingsToCloud(settings: UserSettingsSyncPayload, userId: String): Result<Unit> {
        if (!canPush(userId, userId)) return Result.failure(SecurityException("Account changed before sync."))
        val store = firestore ?: return Result.failure(IllegalStateException("Firestore unavailable"))
        return try {
            store.collection("users").document(userId)
                .collection("settings").document("preferences")
                .set(settings.toMap(), SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    /**
     * Stops user cloud listeners and cleanly purges an account's personal data from local SQLite
     * upon explicit sign-out, preventing cross-user data leakage on the same device.
     */
    suspend fun purgeUserDataOnSignOut(userId: String) = withContext(NonCancellable + Dispatchers.IO) {
        require(userId.isNotBlank())
        synchronized(listenerLock) {
            if (activeUserId == userId || sessions.currentUserId() == userId) stopUserSync()
        }
        personalDataMutex.withLock {
            val tasks = db.assignmentDao().getAllRawSyncForUser(userId).filter { it.ownerId == userId }
            val notes = db.timetableDao().getAllNotesRawSyncForUser(userId).filter { it.ownerId == userId }
            beforeUserDataPurge(tasks, notes)
            db.withTransaction {
                db.timetableDao().purgeUserNotes(userId)
                db.customEventDao().purgeUserData(userId)
                db.assignmentDao().purgeUserData(userId)
            }
        }
    }

    internal suspend fun applyRemoteNotes(
        token: UserSyncSession.Token,
        changes: List<RemoteRecordChange<LectureNote>>
    ) = withSession(token) {
        db.withTransaction {
            for (change in changes) {
                val remote = change.record
                if (remote.ownerId != null && remote.ownerId != token.userId) continue
                val existing = db.timetableDao().getNoteRawDirect(remote.naturalKey)
                if (existing?.ownerId != null && existing.ownerId != token.userId) continue
                if (change.removed) {
                    if (existing != null && remote.updatedAt >= existing.updatedAt) {
                        db.timetableDao().upsertNotes(listOf(existing.copy(isDeleted = true, updatedAt = remote.updatedAt)))
                    }
                } else {
                    val merged = SyncConflictResolver.mergeNotes(listOfNotNull(existing), listOf(remote.copy(ownerId = token.userId)))
                    db.timetableDao().upsertNotes(merged.mergedList)
                }
            }
        }
    }

    internal suspend fun applyRemoteEvents(
        token: UserSyncSession.Token,
        changes: List<RemoteRecordChange<CustomEvent>>
    ) = withSession(token) {
        db.withTransaction {
            for (change in changes) {
                val remote = change.record
                if (remote.ownerId != null && remote.ownerId != token.userId) continue
                val existing = db.customEventDao().getByUuid(remote.clientUuid)
                if (existing?.ownerId != null && existing.ownerId != token.userId) continue
                if (change.removed) {
                    if (existing != null && remote.updatedAt >= existing.updatedAt) {
                        db.customEventDao().upsertAll(listOf(existing.copy(isDeleted = true, updatedAt = remote.updatedAt)))
                    }
                } else {
                    val merged = SyncConflictResolver.mergeEvents(listOfNotNull(existing), listOf(remote.copy(ownerId = token.userId)))
                    db.customEventDao().upsertAll(merged.mergedList)
                }
            }
        }
    }

    internal suspend fun applyRemoteAssignments(
        token: UserSyncSession.Token,
        changes: List<RemoteRecordChange<Assignment>>
    ) = withSession(token) {
        db.withTransaction {
            for (change in changes) {
                val remote = change.record
                if (remote.ownerId != null && remote.ownerId != token.userId) continue
                val existing = db.assignmentDao().getByUuid(remote.clientUuid)
                if (existing?.ownerId != null && existing.ownerId != token.userId) continue
                if (change.removed) {
                    if (existing != null && remote.updatedAt >= existing.updatedAt) {
                        db.assignmentDao().upsertAll(listOf(existing.copy(isDeleted = true, updatedAt = remote.updatedAt)))
                    }
                } else {
                    val merged = SyncConflictResolver.mergeAssignments(listOfNotNull(existing), listOf(remote.copy(ownerId = token.userId)))
                    db.assignmentDao().upsertAll(merged.mergedList)
                }
            }
        }
    }

    private fun launchUserWork(scope: CoroutineScope, token: UserSyncSession.Token, block: suspend () -> Unit) {
        scope.launch(Dispatchers.IO) {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (isSessionCurrent(token)) _lastSyncError.value = e.localizedMessage ?: "Could not apply cloud changes."
                Log.w(TAG, "Cloud update could not be applied", e)
            }
        }
    }

    internal fun beginSession(userId: String): UserSyncSession.Token = synchronized(listenerLock) {
        if (activeUserId != null && activeUserId != userId) stopUserSync()
        sessions.begin(userId)
    }

    private fun canPush(userId: String, ownerId: String?): Boolean =
        userId.isNotBlank() && currentUserIdProvider() == userId && (ownerId == null || ownerId == userId)

    private fun isSessionCurrent(token: UserSyncSession.Token): Boolean =
        sessions.isCurrent(token) && currentUserIdProvider() == token.userId

    private fun ensureSession(token: UserSyncSession.Token) {
        if (!isSessionCurrent(token)) throw CancellationException("Account sync session ended.")
    }

    private suspend fun <T> withSession(token: UserSyncSession.Token, action: suspend () -> T): T =
        personalDataMutex.withLock {
            ensureSession(token)
            action()
        }

    companion object {
        private const val TAG = "SyncRepository"
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
            floor = getLong("floor")?.toInt() ?: com.lectures.features.rooms.UniversityDirectory.resolveLevel(getString("room"))?.floorNumber
        )
    } catch (e: Exception) { null }
}

private fun com.google.firebase.firestore.DocumentSnapshot.toLectureNote(fallbackOwnerId: String? = null): LectureNote? {
    return try {
        if (fallbackOwnerId != null && getString("owner_id") != null && getString("owner_id") != fallbackOwnerId) return null
        if (getString("natural_key") != null && getString("natural_key") != id) return null
        LectureNote(
            naturalKey = getString("natural_key") ?: id,
            title = getString("title"),
            content = getString("content") ?: "",
            updatedAt = when (val revision = get("updated_at")) {
                is Number -> revision.toLong()
                is com.google.firebase.Timestamp -> revision.toDate().time
                null -> 0L
                else -> return null
            },
            colourTag = getString("colour_tag"),
            alarmMinutes = getLong("alarm_minutes")?.toInt(),
            tag = getString("tag"),
            attachedClass = getString("attached_class"),
            attachmentName = getString("attachment_name"),
            attachmentPath = null,
            isPinned = getBoolean("is_pinned") ?: false,
            isDeleted = getBoolean("is_deleted") ?: false,
            ownerId = getString("owner_id") ?: fallbackOwnerId
        )
    } catch (e: Exception) { null }
}

private fun com.google.firebase.firestore.DocumentSnapshot.toCustomEvent(fallbackOwnerId: String? = null): CustomEvent? {
    return try {
        if (fallbackOwnerId != null && getString("owner_id") != null && getString("owner_id") != fallbackOwnerId) return null
        if (getString("client_uuid") != null && getString("client_uuid") != id) return null
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
            updatedAt = getLong("updated_at") ?: 0L,
            isDeleted = getBoolean("is_deleted") ?: false,
            ownerId = getString("owner_id") ?: fallbackOwnerId
        )
    } catch (e: Exception) { null }
}

private fun com.google.firebase.firestore.DocumentSnapshot.toAssignment(fallbackOwnerId: String? = null): Assignment? {
    return try {
        if (fallbackOwnerId != null && getString("owner_id") != null && getString("owner_id") != fallbackOwnerId) return null
        if (getString("client_uuid") != null && getString("client_uuid") != id) return null
        Assignment(
            clientUuid = getString("client_uuid") ?: id,
            title = getString("title") ?: return null,
            courseCode = getString("course_code") ?: "",
            dueDate = getString("due_date") ?: "",
            reminderMinutes = getLong("reminder_minutes")?.toInt(),
            priority = getString("priority") ?: "Medium",
            notes = getString("notes") ?: "",
            completed = getBoolean("completed") ?: false,
            updatedAt = getLong("updated_at") ?: 0L,
            isDeleted = getBoolean("is_deleted") ?: false,
            ownerId = getString("owner_id") ?: fallbackOwnerId
        )
    } catch (e: Exception) { null }
}
