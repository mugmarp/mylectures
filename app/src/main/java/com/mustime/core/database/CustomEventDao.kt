package com.mustime.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.mustime.features.timetable.domain.CustomEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomEventDao {
    @Query("SELECT * FROM custom_events WHERE isDeleted = 0 ORDER BY dayOfWeek, startTime")
    fun getAll(): Flow<List<CustomEvent>>

    @Upsert
    suspend fun upsert(event: CustomEvent): Long

    @Upsert
    suspend fun upsertAll(events: List<CustomEvent>)

    @Query("UPDATE custom_events SET isDeleted = 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun softDelete(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM custom_events WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM custom_events WHERE clientUuid = :uuid LIMIT 1")
    suspend fun getByUuid(uuid: String): CustomEvent?

    @Query("SELECT * FROM custom_events")
    suspend fun getAllRawSync(): List<CustomEvent>

    @Query("SELECT * FROM custom_events WHERE ownerId = :userId OR ownerId IS NULL")
    suspend fun getAllRawSyncForUser(userId: String): List<CustomEvent>

    @Query("UPDATE custom_events SET ownerId = :userId WHERE ownerId IS NULL")
    suspend fun promoteGuestData(userId: String)

    @Query("DELETE FROM custom_events WHERE isDeleted = 1 AND updatedAt < :cutoffTimestamp")
    suspend fun purgeTombstones(cutoffTimestamp: Long): Int

    @Query("DELETE FROM custom_events WHERE ownerId = :userId")
    suspend fun purgeUserData(userId: String)

    @Query("DELETE FROM custom_events")
    suspend fun deleteAll()
}
