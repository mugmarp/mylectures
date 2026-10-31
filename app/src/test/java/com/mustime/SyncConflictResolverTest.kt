package com.mustime

import com.mustime.core.sync.SyncConflictResolver
import com.mustime.core.sync.UserSettingsSyncPayload
import com.mustime.features.timetable.domain.Assignment
import com.mustime.features.timetable.domain.CustomEvent
import com.mustime.features.timetable.domain.LectureNote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncConflictResolverTest {

    @Test
    fun testEventsAdditiveUnionAndLWW() {
        val localEvent1 = CustomEvent(
            id = 1,
            clientUuid = "uuid-1",
            title = "Maths Revision",
            dayOfWeek = "Monday",
            startTime = "10:00",
            endTime = "12:00",
            updatedAt = 1000L
        )
        val localEvent2 = CustomEvent(
            id = 2,
            clientUuid = "uuid-2",
            title = "Physics Group",
            dayOfWeek = "Tuesday",
            startTime = "14:00",
            endTime = "16:00",
            updatedAt = 1000L
        )

        // Remote has updated uuid-1 (newer), and new uuid-3
        val remoteEvent1 = CustomEvent(
            id = 0,
            clientUuid = "uuid-1",
            title = "Maths Revision (Updated Venue)",
            dayOfWeek = "Monday",
            startTime = "10:00",
            endTime = "12:00",
            updatedAt = 2000L
        )
        val remoteEvent3 = CustomEvent(
            id = 0,
            clientUuid = "uuid-3",
            title = "Chemistry Lab Discussion",
            dayOfWeek = "Wednesday",
            startTime = "09:00",
            endTime = "11:00",
            updatedAt = 1500L
        )

        val result = SyncConflictResolver.mergeEvents(
            localEvents = listOf(localEvent1, localEvent2),
            remoteEvents = listOf(remoteEvent1, remoteEvent3)
        )

        // Total 3 distinct events
        assertEquals(3, result.mergedList.size)

        // uuid-1 should be the newer remote version, but retaining local SQLite id = 1
        val merged1 = result.mergedList.first { it.clientUuid == "uuid-1" }
        assertEquals("Maths Revision (Updated Venue)", merged1.title)
        assertEquals(1L, merged1.id)

        // uuid-2 was local-only -> must be queued to push to remote
        assertTrue(result.toPushToRemote.any { it.clientUuid == "uuid-2" })

        // uuid-3 was remote-only -> should be adopted locally with id = 0 (auto-gen)
        val merged3 = result.mergedList.first { it.clientUuid == "uuid-3" }
        assertEquals(0L, merged3.id)
    }

    @Test
    fun testAssignmentsAdditiveUnionAndLWW() {
        val localTask = Assignment(
            id = 10,
            clientUuid = "task-1",
            title = "Data Structures Project",
            courseCode = "CSC2101",
            dueDate = "2026-11-05",
            completed = false,
            updatedAt = 1000L
        )
        val remoteTask = Assignment(
            id = 0,
            clientUuid = "task-1",
            title = "Data Structures Project",
            courseCode = "CSC2101",
            dueDate = "2026-11-05",
            completed = true,
            updatedAt = 2000L // Remote marked it completed later
        )

        val result = SyncConflictResolver.mergeAssignments(
            localAssignments = listOf(localTask),
            remoteAssignments = listOf(remoteTask)
        )

        assertEquals(1, result.mergedList.size)
        val merged = result.mergedList.first()
        assertTrue(merged.completed)
        assertEquals(10L, merged.id) // Local SQLite ID preserved
        assertTrue(result.toPushToRemote.isEmpty())
    }

    @Test
    fun testNotesDivergentContentCreatesInPlaceNonDestructiveMerge() {
        val key = "CSC2101_Monday_08:00"
        val localNote = LectureNote(
            naturalKey = key,
            title = "Sorting Algorithms",
            content = "Student typed offline: QuickSort pivot optimization and median-of-three.",
            updatedAt = 2000L
        )
        val remoteNote = LectureNote(
            naturalKey = key,
            title = "Sorting Algorithms",
            content = "Written from laptop: MergeSort complexity analysis and recursion tree.",
            updatedAt = 2500L
        )

        val result = SyncConflictResolver.mergeNotes(
            localNotes = listOf(localNote),
            remoteNotes = listOf(remoteNote)
        )

        // Must preserve the slot's natural key without orphaning it into an arbitrary string!
        assertEquals(1, result.mergedList.size)
        assertEquals(1, result.conflictsForkedCount)

        val resolvedNote = result.mergedList.first()
        assertEquals(key, resolvedNote.naturalKey)

        // Both student contributions preserved non-destructively!
        assertTrue(resolvedNote.content.contains("MergeSort"))
        assertTrue(resolvedNote.content.contains("QuickSort"))
        assertTrue(resolvedNote.content.contains("Offline Draft from This Device"))

        // Must be queued to push so both devices unify
        assertEquals(1, result.toPushToRemote.size)
        assertEquals(key, result.toPushToRemote.first().naturalKey)
    }

    @Test
    fun testSettingsLWWResolution() {
        val localSettings = UserSettingsSyncPayload(
            programme = "BCS II",
            classAlarmLeadMinutes = 15,
            updatedAt = 1000L
        )
        val remoteSettings = UserSettingsSyncPayload(
            programme = "BCS II",
            classAlarmLeadMinutes = 45,
            updatedAt = 2000L
        )

        val (resolved, shouldPush) = SyncConflictResolver.mergeSettings(localSettings, remoteSettings)
        assertEquals(45, resolved.classAlarmLeadMinutes)
        assertFalse(shouldPush)
    }
}
