package com.mustime.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.mustime.features.timetable.domain.Assignment
import kotlinx.coroutines.flow.Flow

@Dao
interface AssignmentDao {
    @Query("SELECT * FROM assignments WHERE isDeleted = 0 ORDER BY completed ASC, id DESC")
    fun getAll(): Flow<List<Assignment>>

    @Query("SELECT * FROM assignments WHERE completed = :completed AND isDeleted = 0 ORDER BY id DESC")
    fun getByCompletion(completed: Boolean): Flow<List<Assignment>>

    @Query("SELECT * FROM assignments WHERE courseCode = :courseCode AND isDeleted = 0 ORDER BY completed ASC, id DESC")
    fun getByCourse(courseCode: String): Flow<List<Assignment>>

    @Upsert
    suspend fun upsert(assignment: Assignment): Long

    @Upsert
    suspend fun upsertAll(assignments: List<Assignment>)

    @Query("UPDATE assignments SET isDeleted = 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun softDelete(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM assignments WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM assignments WHERE isDeleted = 0")
    suspend fun getCount(): Int

    @Query("UPDATE assignments SET completed = :completed, updatedAt = :timestamp WHERE id = :id")
    suspend fun updateCompletion(id: Long, completed: Boolean, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT * FROM assignments WHERE clientUuid = :uuid LIMIT 1")
    suspend fun getByUuid(uuid: String): Assignment?

    @Query("SELECT * FROM assignments")
    suspend fun getAllRawSync(): List<Assignment>

    @Query("SELECT * FROM assignments WHERE ownerId = :userId OR ownerId IS NULL")
    suspend fun getAllRawSyncForUser(userId: String): List<Assignment>

    @Query("UPDATE assignments SET ownerId = :userId WHERE ownerId IS NULL")
    suspend fun promoteGuestData(userId: String)

    @Query("DELETE FROM assignments WHERE isDeleted = 1 AND updatedAt < :cutoffTimestamp")
    suspend fun purgeTombstones(cutoffTimestamp: Long): Int

    @Query("DELETE FROM assignments WHERE ownerId = :userId")
    suspend fun purgeUserData(userId: String)

    @Query("DELETE FROM assignments")
    suspend fun deleteAll()
}
