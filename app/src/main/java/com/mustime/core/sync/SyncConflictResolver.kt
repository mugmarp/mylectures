package com.mustime.core.sync

import com.mustime.features.timetable.domain.Assignment
import com.mustime.features.timetable.domain.CustomEvent
import com.mustime.features.timetable.domain.LectureNote

/**
 * Robust conflict resolution and merge engine for distributed offline-first sync.
 *
 * Implements:
 * 1. Additive Union for collections (never loses records created on either side).
 * 2. Last-Write-Wins (LWW) based on timestamps for field updates and tombstones.
 * 3. Non-destructive conflict bifurcation for lecture notes (forks diverging contents
 *    so students never lose notes typed offline).
 */
object SyncConflictResolver {

    data class EventMergeResult(
        val mergedList: List<CustomEvent>,
        val toPushToRemote: List<CustomEvent>
    )

    data class AssignmentMergeResult(
        val mergedList: List<Assignment>,
        val toPushToRemote: List<Assignment>
    )

    data class NoteMergeResult(
        val mergedList: List<LectureNote>,
        val toPushToRemote: List<LectureNote>,
        val conflictsForkedCount: Int
    )

    /**
     * Merges local and remote CustomEvents.
     */
    fun mergeEvents(
        localEvents: List<CustomEvent>,
        remoteEvents: List<CustomEvent>
    ): EventMergeResult {
        val localByUuid = localEvents.associateBy { it.clientUuid }
        val remoteByUuid = remoteEvents.associateBy { it.clientUuid }

        val merged = mutableListOf<CustomEvent>()
        val toPush = mutableListOf<CustomEvent>()

        val allUuids = localByUuid.keys + remoteByUuid.keys

        for (uuid in allUuids) {
            val local = localByUuid[uuid]
            val remote = remoteByUuid[uuid]

            when {
                local != null && remote == null -> {
                    // Local only: keep and queue for remote push
                    merged.add(local)
                    toPush.add(local)
                }
                local == null && remote != null -> {
                    // Remote only: adopt locally with new local id (id = 0 for auto-gen)
                    merged.add(remote.copy(id = 0))
                }
                local != null && remote != null -> {
                    // Exists on both sides: Last-Write-Wins by updatedAt
                    if (remote.updatedAt > local.updatedAt) {
                        // Remote is newer: adopt remote attributes, preserve local SQLite ID & local owner
                        merged.add(remote.copy(id = local.id, ownerId = local.ownerId ?: remote.ownerId))
                    } else if (local.updatedAt > remote.updatedAt) {
                        // Local is newer: keep local, push to remote
                        merged.add(local)
                        toPush.add(local)
                    } else {
                        // Timestamps equal: keep local
                        merged.add(local)
                    }
                }
            }
        }

        return EventMergeResult(mergedList = merged, toPushToRemote = toPush)
    }

    /**
     * Merges local and remote Assignments.
     */
    fun mergeAssignments(
        localAssignments: List<Assignment>,
        remoteAssignments: List<Assignment>
    ): AssignmentMergeResult {
        val localByUuid = localAssignments.associateBy { it.clientUuid }
        val remoteByUuid = remoteAssignments.associateBy { it.clientUuid }

        val merged = mutableListOf<Assignment>()
        val toPush = mutableListOf<Assignment>()

        val allUuids = localByUuid.keys + remoteByUuid.keys

        for (uuid in allUuids) {
            val local = localByUuid[uuid]
            val remote = remoteByUuid[uuid]

            when {
                local != null && remote == null -> {
                    merged.add(local)
                    toPush.add(local)
                }
                local == null && remote != null -> {
                    merged.add(remote.copy(id = 0))
                }
                local != null && remote != null -> {
                    if (remote.updatedAt > local.updatedAt) {
                        merged.add(remote.copy(id = local.id, ownerId = local.ownerId ?: remote.ownerId))
                    } else if (local.updatedAt > remote.updatedAt) {
                        merged.add(local)
                        toPush.add(local)
                    } else {
                        merged.add(local)
                    }
                }
            }
        }

        return AssignmentMergeResult(mergedList = merged, toPushToRemote = toPush)
    }

    /**
     * Merges local and remote LectureNotes with non-destructive preservation.
     */
    fun mergeNotes(
        localNotes: List<LectureNote>,
        remoteNotes: List<LectureNote>
    ): NoteMergeResult {
        val localByKey = localNotes.associateBy { it.naturalKey }
        val remoteByKey = remoteNotes.associateBy { it.naturalKey }

        val merged = mutableListOf<LectureNote>()
        val toPush = mutableListOf<LectureNote>()
        var conflictsForked = 0

        val allKeys = localByKey.keys + remoteByKey.keys

        for (key in allKeys) {
            val local = localByKey[key]
            val remote = remoteByKey[key]

            when {
                local != null && remote == null -> {
                    merged.add(local)
                    toPush.add(local)
                }
                local == null && remote != null -> {
                    merged.add(remote)
                }
                local != null && remote != null -> {
                    val contentsDiverged = local.content.trim() != remote.content.trim() &&
                            local.content.isNotBlank() && remote.content.isNotBlank() &&
                            !local.isDeleted && !remote.isDeleted

                    if (contentsDiverged) {
                        // Conflict: Both sides have distinct text written offline!
                        // Instead of corrupting the slot naturalKey (which would orphan the note
                        // from the timetable card), preserve both student contributions directly
                        // inside the note using an explicit conflict draft section.
                        val (primary, secondary, forkedIsLocal) = if (remote.updatedAt >= local.updatedAt) {
                            Triple(remote, local, true)
                        } else {
                            Triple(local, remote, false)
                        }

                        val conflictDivider = if (forkedIsLocal) {
                            "\n\n--- [Offline Draft from This Device] ---\n"
                        } else {
                            "\n\n--- [Conflicting Revision from Remote Device] ---\n"
                        }

                        val mergedContent = "${primary.content.trim()}$conflictDivider${secondary.content.trim()}"
                        val unifiedNote = primary.copy(
                            content = mergedContent,
                            ownerId = local.ownerId ?: remote.ownerId,
                            updatedAt = maxOf(primary.updatedAt, secondary.updatedAt) + 1000L
                        )

                        merged.add(unifiedNote)
                        toPush.add(unifiedNote)
                        conflictsForked++
                    } else {
                        // Standard LWW
                        if (remote.updatedAt > local.updatedAt) {
                            merged.add(remote.copy(ownerId = local.ownerId ?: remote.ownerId))
                        } else if (local.updatedAt > remote.updatedAt) {
                            merged.add(local)
                            toPush.add(local)
                        } else {
                            merged.add(local)
                        }
                    }
                }
            }
        }

        return NoteMergeResult(
            mergedList = merged,
            toPushToRemote = toPush,
            conflictsForkedCount = conflictsForked
        )
    }

    /**
     * Resolves settings between local and remote using Last-Write-Wins.
     */
    fun mergeSettings(
        local: UserSettingsSyncPayload,
        remote: UserSettingsSyncPayload?
    ): Pair<UserSettingsSyncPayload, Boolean> {
        if (remote == null) return Pair(local, true) // Push local
        return if (remote.updatedAt > local.updatedAt) {
            Pair(remote, false) // Adopt remote
        } else {
            Pair(local, true) // Keep local and push
        }
    }
}
