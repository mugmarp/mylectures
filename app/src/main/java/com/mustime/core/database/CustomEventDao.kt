package com.mustime.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.mustime.features.timetable.domain.CustomEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomEventDao {
    @Query("SELECT * FROM custom_events ORDER BY dayOfWeek, startTime")
    fun getAll(): Flow<List<CustomEvent>>

    @Upsert
    suspend fun upsert(event: CustomEvent): Long

    @Query("DELETE FROM custom_events WHERE id = :id")
    suspend fun delete(id: Long)
}
