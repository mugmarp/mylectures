package com.lectures

import com.lectures.core.sync.SyncConflictResolver
import com.lectures.core.sync.UserSettingsSyncPayload
import com.lectures.features.timetable.domain.LectureNote
import org.junit.Assert.*
import org.junit.Test

class SyncConflictPreservationTest {
    private fun note(content: String, time: Long) = LectureNote("slot", content, time, ownerId = "alice")

    @Test fun repeatedConflictMergeDoesNotDuplicateDraftsOrAdvanceRevision() {
        val local = note("My offline draft", 1)
        val remote = note("Another device's draft", 2)
        val first = SyncConflictResolver.mergeNotes(listOf(local), listOf(remote))
        val second = SyncConflictResolver.mergeNotes(first.mergedList, listOf(remote))
        assertEquals(1, first.conflictsForkedCount)
        assertEquals(0, second.conflictsForkedCount)
        assertEquals(first.mergedList, second.mergedList)
    }

    @Test fun shortDraftContainedInLongerTextIsStillPreservedAsARevision() {
        val result = SyncConflictResolver.mergeNotes(listOf(note("DNA", 1)), listOf(note("DNA replication", 2)))
        assertEquals(1, result.conflictsForkedCount)
        assertTrue(result.mergedList.single().content.endsWith("\nDNA"))
    }

    @Test fun newerRemoteContentRetainsCoherentLocalAttachmentPair() {
        val local = note("Same", 1).copy(attachmentPath = "attachments/local.pdf", attachmentName = "Local.pdf")
        val remote = note("Same", 2).copy(attachmentPath = "attachments/other.pdf", attachmentName = "Other.pdf")
        val merged = SyncConflictResolver.mergeNotes(listOf(local), listOf(remote)).mergedList.single()
        assertEquals("attachments/local.pdf", merged.attachmentPath)
        assertEquals("Local.pdf", merged.attachmentName)
    }

    @Test fun remoteOnlyRecordDoesNotAdoptAnotherDevicesPrivateFilePath() {
        val remote = note("Notes", 2).copy(attachmentPath = "attachments/remote.pdf", attachmentName = "Slides.pdf")
        val merged = SyncConflictResolver.mergeNotes(emptyList(), listOf(remote)).mergedList.single()
        assertNull(merged.attachmentPath)
        assertEquals("Slides.pdf", merged.attachmentName)
    }

    @Test fun newerTombstoneWinsWithoutForkingContent() {
        val result = SyncConflictResolver.mergeNotes(listOf(note("Old", 1)), listOf(note("Deleted", 2).copy(isDeleted = true)))
        assertTrue(result.mergedList.single().isDeleted)
        assertEquals(0, result.conflictsForkedCount)
    }

    @Test fun identicalSettingsDoNotUploadAgain() {
        val settings = UserSettingsSyncPayload(updatedAt = 123)
        assertFalse(SyncConflictResolver.mergeSettings(settings, settings).second)
    }
    @Test fun legacySettingsWithoutRevisionAreNotTreatedAsFreshEdits() {
        val remote = UserSettingsSyncPayload.fromMap(mapOf("theme_mode" to "DARK"))
        val local = UserSettingsSyncPayload(themeMode = "LIGHT", updatedAt = 123)
        assertEquals(0L, remote.updatedAt)
        assertEquals(local, SyncConflictResolver.mergeSettings(local, remote).first)
    }

}
