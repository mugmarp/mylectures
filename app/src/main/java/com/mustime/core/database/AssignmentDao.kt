package com.mustime.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.mustime.features.timetable.domain.Assignment
import kotlinx.coroutines.flow.Flow

@Dao
interface AssignmentDao {
    @Query("SELECT * FROM assignments ORDER BY completed ASC, id DESC")
    fun getAll(): Flow<List<Assignment>>

    @Query("SELECT * FROM assignments WHERE completed = :completed ORDER BY id DESC")
    fun getByCompletion(completed: Boolean): Flow<List<Assignment>>

    @Query("SELECT * FROM assignments WHERE courseCode = :courseCode ORDER BY completed ASC, id DESC")
    fun getByCourse(courseCode: String): Flow<List<Assignment>>

    @Upsert
    suspend fun upsert(assignment: Assignment): Long

    @Query("DELETE FROM assignments WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM assignments")
    suspend fun getCount(): Int

    @Query("UPDATE assignments SET completed = :completed WHERE id = :id")
    suspend fun updateCompletion(id: Long, completed: Boolean)

    @Query("DELETE FROM assignments")
    suspend fun deleteAll()
}
