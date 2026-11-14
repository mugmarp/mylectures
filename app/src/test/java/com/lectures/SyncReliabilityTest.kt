package com.lectures

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.lectures.core.database.AppDatabase
import com.lectures.core.sync.*
import com.lectures.features.timetable.data.ETagStore
import com.lectures.features.timetable.data.TimetableRepository
import com.lectures.features.timetable.domain.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SyncReliabilityTest {
    private lateinit var db: AppDatabase
    private lateinit var store: ETagStore
    private lateinit var sync: SyncRepository
    private lateinit var repository: TimetableRepository
    private val uid = MutableStateFlow<String?>("alice")

    @Before fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        store = ETagStore(context)
        sync = SyncRepository(db, firestore = null, currentUserIdProvider = { uid.value })
        repository = TimetableRepository(db.timetableDao(), db.customEventDao(), db.assignmentDao(), store,
            currentUserIdProvider = { uid.value }, currentUserIds = uid)
    }
    @After fun tearDown() { sync.stopAllSync(); db.close() }
    private fun note(key: String = "note", owner: String? = "alice", time: Long = 10) =
        LectureNote(key, "Lecture", time, ownerId = owner)
    private fun event(owner: String? = "alice", time: Long = 10) =
        CustomEvent(clientUuid = "event", title = "Study", dayOfWeek = "Monday", startTime = "10:00", endTime = "11:00", ownerId = owner, updatedAt = time)
    private fun task(owner: String? = "alice", time: Long = 10) =
        Assignment(clientUuid = "task", title = "Coursework", courseCode = "CS101", dueDate = "2026-11-20", ownerId = owner, updatedAt = time)

    @Test fun sessionInvalidatesQueuedWorkAcrossStopAndAccountRoundTrip() {
        val sessions = UserSyncSession()
        val first = sessions.begin("alice")
        assertEquals(first, sessions.begin("alice"))
        sessions.stop()
        assertFalse(sessions.isCurrent(first))
        val second = sessions.begin("alice")
        sessions.begin("bob")
        val third = sessions.begin("alice")
        assertFalse(sessions.isCurrent(second))
        assertTrue(sessions.isCurrent(third))
    }

    @Test fun wrongAccountCannotPromoteGuestData() = runBlocking {
        db.timetableDao().upsertNotes(listOf(note(owner = null)))
        val result = sync.performFullUserSync("bob", UserSettingsSyncManager(store))
        assertEquals(SyncState.ERROR, result.status)
        assertNull(db.timetableDao().getNoteRawDirect("note")!!.ownerId)
    }

    @Test fun unavailableServiceReportsFailureAndRetainsOwnedData() = runBlocking {
        db.timetableDao().upsertNotes(listOf(note(owner = null)))
        val result = sync.performFullUserSync("alice", UserSettingsSyncManager(store))
        assertEquals(SyncState.ERROR, result.status)
        assertEquals(1, result.notesCount)
        assertEquals("alice", db.timetableDao().getNoteRawDirect("note")!!.ownerId)
    }

    @Test fun wrongOwnerUploadsFailBeforeAccessingFirestore() = runBlocking {
        assertTrue(sync.pushNoteToCloud(note(owner = "bob"), "alice").exceptionOrNull() is SecurityException)
        assertTrue(sync.pushEventToCloud(event(owner = "bob"), "alice").exceptionOrNull() is SecurityException)
        assertTrue(sync.pushAssignmentToCloud(task(owner = "bob"), "alice").exceptionOrNull() is SecurityException)
        assertTrue(sync.pushSettingsToCloud(UserSettingsSyncPayload(), "bob").exceptionOrNull() is SecurityException)
    }

    @Test fun repeatedRemoteUpdatesPreserveRoomIds() = runBlocking {
        val eventId = db.customEventDao().upsert(event())
        val taskId = db.assignmentDao().upsert(task())
        val token = sync.beginSession("alice")
        for (time in listOf(20L, 30L)) {
            sync.applyRemoteEvents(token, listOf(RemoteRecordChange(event(time = time))))
            sync.applyRemoteAssignments(token, listOf(RemoteRecordChange(task(time = time))))
        }
        assertEquals(eventId, db.customEventDao().getByUuid("event")!!.id)
        assertEquals(taskId, db.assignmentDao().getByUuid("task")!!.id)
        assertEquals(30L, db.assignmentDao().getByUuid("task")!!.updatedAt)
    }

    @Test fun staleRemoteRemovalCannotDeleteNewerLocalWork() = runBlocking {
        db.timetableDao().upsertNotes(listOf(note(time = 20)))
        db.customEventDao().upsert(event(time = 20))
        db.assignmentDao().upsert(task(time = 20))
        val token = sync.beginSession("alice")
        sync.applyRemoteNotes(token, listOf(RemoteRecordChange(note(time = 10), removed = true)))
        sync.applyRemoteEvents(token, listOf(RemoteRecordChange(event(time = 10), removed = true)))
        sync.applyRemoteAssignments(token, listOf(RemoteRecordChange(task(time = 10), removed = true)))
        assertFalse(db.timetableDao().getNoteRawDirect("note")!!.isDeleted)
        assertFalse(db.customEventDao().getByUuid("event")!!.isDeleted)
        assertFalse(db.assignmentDao().getByUuid("task")!!.isDeleted)
        sync.applyRemoteAssignments(token, listOf(RemoteRecordChange(task(time = 30), removed = true)))
        assertTrue(db.assignmentDao().getByUuid("task")!!.isDeleted)
    }

    @Test fun oldListenerCannotReinsertRecordsAfterStop() = runBlocking {
        val old = sync.beginSession("alice")
        sync.stopUserSync()
        sync.beginSession("alice")
        try {
            sync.applyRemoteNotes(old, listOf(RemoteRecordChange(note())))
            fail("A stopped listener must be rejected")
        } catch (_: CancellationException) { }
        assertNull(db.timetableDao().getNoteRawDirect("note"))
    }

    @Test fun listenersCannotOverwriteOtherAccountsOrAdoptSpoofedOwners() = runBlocking {
        db.timetableDao().upsertNotes(listOf(note(owner = "bob")))
        db.customEventDao().upsert(event(owner = "bob"))
        db.assignmentDao().upsert(task(owner = "bob"))
        val token = sync.beginSession("alice")
        sync.applyRemoteNotes(token, listOf(RemoteRecordChange(note(time = 30)), RemoteRecordChange(note("spoof", "bob"))))
        sync.applyRemoteEvents(token, listOf(RemoteRecordChange(event(time = 30))))
        sync.applyRemoteAssignments(token, listOf(RemoteRecordChange(task(time = 30))))
        assertEquals("bob", db.timetableDao().getNoteRawDirect("note")!!.ownerId)
        assertEquals("bob", db.customEventDao().getByUuid("event")!!.ownerId)
        assertEquals("bob", db.assignmentDao().getByUuid("task")!!.ownerId)
        assertNull(db.timetableDao().getNoteRawDirect("spoof"))
    }

    @Test fun purgeRemovesOnlySignedOutAccountAndCallsReminderCleanup() = runBlocking {
        db.timetableDao().upsertNotes(listOf(note("alice", "alice"), note("bob", "bob"), note("guest", null)))
        db.assignmentDao().upsert(task())
        db.customEventDao().upsert(event())
        var cleaned = false
        val purger = SyncRepository(db, firestore = null, currentUserIdProvider = { uid.value }, beforeUserDataPurge = { tasks, notes ->
            assertEquals(listOf("alice"), notes.map { it.ownerId })
            assertEquals(listOf("alice"), tasks.map { it.ownerId })
            cleaned = true
        })
        uid.value = "bob"
        val bobSession = purger.beginSession("bob")
        purger.purgeUserDataOnSignOut("alice")
        assertTrue(cleaned)
        assertNull(db.timetableDao().getNoteRawDirect("alice"))
        assertNotNull(db.timetableDao().getNoteRawDirect("bob"))
        assertNotNull(db.timetableDao().getNoteRawDirect("guest"))
        assertTrue(db.assignmentDao().getAllRawSync().isEmpty())
        assertTrue(db.customEventDao().getAllRawSync().isEmpty())
        purger.applyRemoteNotes(bobSession, listOf(RemoteRecordChange(note("bob-new", "bob"))))
        assertNotNull(db.timetableDao().getNoteRawDirect("bob-new"))
    }

    @Test fun tombstoneCleanupDoesNotPurgeOtherAccounts() = runBlocking {
        db.timetableDao().upsertNotes(listOf(note("alice", "alice").copy(isDeleted = true), note("bob", "bob").copy(isDeleted = true)))
        db.customEventDao().upsertAll(listOf(event().copy(isDeleted = true), event("bob").copy(clientUuid = "bob", isDeleted = true)))
        db.assignmentDao().upsertAll(listOf(task().copy(isDeleted = true), task("bob").copy(clientUuid = "bob", isDeleted = true)))
        assertEquals(1, db.timetableDao().purgeNoteTombstonesForUser("alice", 100))
        assertEquals(1, db.customEventDao().purgeTombstonesForUser("alice", 100))
        assertEquals(1, db.assignmentDao().purgeTombstonesForUser("alice", 100))
        assertNotNull(db.timetableDao().getNoteRawDirect("bob"))
        assertNotNull(db.customEventDao().getByUuid("bob"))
        assertNotNull(db.assignmentDao().getByUuid("bob"))
    }

    @Test fun personalListsFollowAccountChangesAndRetainGuestVisibility() = runBlocking {
        db.timetableDao().upsertNotes(listOf(note("alice", "alice"), note("bob", "bob"), note("guest", null)))
        val states = Channel<Set<String>>(Channel.UNLIMITED)
        val observer = launch {
            repository.getAllNotes().collect { rows -> states.send(rows.map { it.naturalKey }.toSet()) }
        }
        try {
            withTimeout(5_000L) {
                assertEquals(setOf("alice", "guest"), states.receive())
                uid.value = "bob"
                assertEquals(setOf("bob", "guest"), states.receive())
                uid.value = null
                assertEquals(setOf("guest"), states.receive())
            }
        } finally {
            observer.cancelAndJoin()
            states.close()
        }
    }

    @Test fun localWritesStampCurrentOwnerAndRejectCrossAccountChanges() = runBlocking {
        repository.upsertNote(note(owner = null))
        val id = repository.saveAssignment(task(owner = null))
        repository.saveCustomEvent(event(owner = null))
        assertEquals("alice", db.timetableDao().getNoteRawDirect("note")!!.ownerId)
        assertEquals("alice", db.assignmentDao().getByUuid("task")!!.ownerId)
        assertEquals("alice", db.customEventDao().getByUuid("event")!!.ownerId)
        uid.value = "bob"
        try { repository.deleteAssignment(id); fail("Must reject another account's task") } catch (_: SecurityException) { }
        try { repository.upsertNote(note(owner = "alice")); fail("Must reject another account's note") } catch (_: SecurityException) { }
    }

    @Test fun settingsExportsKeepRevisionAndRemoteImportPreservesTimestamp() = runBlocking {
        val manager = UserSettingsSyncManager(store)
        manager.applySettings(UserSettingsSyncPayload(updatedAt = 123, themeMode = "DARK"))
        assertEquals(123L, manager.exportCurrentSettings().updatedAt)
        assertEquals(manager.exportCurrentSettings(), manager.exportCurrentSettings())
        store.setThemeMode("LIGHT")
        assertTrue(manager.exportCurrentSettings().updatedAt > 123)
    }
}
