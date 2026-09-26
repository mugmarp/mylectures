package com.mustime.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.mustime.features.timetable.domain.LectureNote
import com.mustime.features.timetable.domain.TimetableEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface TimetableDao {
    @Query("SELECT * FROM timetable ORDER BY start_time ASC")
    fun getAllEntries(): Flow<List<TimetableEntry>>

    @Query("SELECT * FROM timetable ORDER BY start_time ASC")
    suspend fun getAllEntriesSync(): List<TimetableEntry>

    @Query("SELECT * FROM timetable WHERE program_group = :programme ORDER BY start_time ASC")
    fun getScheduleForProgramme(programme: String): Flow<List<TimetableEntry>>

    @Query("SELECT * FROM timetable WHERE program_group LIKE '%' || :programme || '%' OR shared_with LIKE '%' || :programme || '%' ORDER BY start_time ASC")
    fun getScheduleIncludingShared(programme: String): Flow<List<TimetableEntry>>

    @Query("""
        SELECT * FROM timetable 
        WHERE program_group = :programme 
           OR program_group LIKE '%' || :code || '%' 
           OR shared_with LIKE '%' || :code || '%' 
           OR (:alias IS NOT NULL AND (program_group LIKE '%' || :alias || '%' OR shared_with LIKE '%' || :alias || '%'))
        ORDER BY start_time ASC
    """)
    fun getScheduleCandidates(programme: String, code: String, alias: String?): Flow<List<TimetableEntry>>

    @Query("SELECT * FROM timetable WHERE day = :day ORDER BY start_time ASC")
    fun getScheduleForDay(day: String): Flow<List<TimetableEntry>>

    @Query("SELECT DISTINCT program_group FROM timetable")
    fun getAllProgrammeGroups(): Flow<List<String>>

    @Upsert
    suspend fun upsertEntries(entries: List<TimetableEntry>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<TimetableEntry>)

    @Query("DELETE FROM timetable WHERE natural_key = :naturalKey")
    suspend fun deleteByNaturalKey(naturalKey: String)

    @Query("DELETE FROM timetable WHERE program_group = :programme")
    suspend fun deleteEntriesForProgramme(programme: String)

    @Query("DELETE FROM timetable")
    suspend fun deleteAllEntries()

    @Query("SELECT COUNT(*) FROM timetable")
    suspend fun getEntryCount(): Int

    @Query("SELECT * FROM lecture_notes WHERE naturalKey = :key LIMIT 1")
    fun getNoteForKey(key: String): Flow<LectureNote?>

    @Query("SELECT * FROM lecture_notes WHERE naturalKey = :key LIMIT 1")
    suspend fun getNoteDirect(key: String): LectureNote?

    @Upsert
    suspend fun upsertNote(note: LectureNote)

    @Query("DELETE FROM lecture_notes WHERE naturalKey = :key")
    suspend fun deleteNote(key: String)

    @Query("SELECT * FROM lecture_notes WHERE alarmMinutes IS NOT NULL")
    suspend fun getNotesWithAlarms(): List<LectureNote>

    @Query("SELECT * FROM lecture_notes ORDER BY updatedAt DESC")
    fun getAllNotes(): Flow<List<LectureNote>>

    @Query("SELECT * FROM lecture_notes ORDER BY updatedAt DESC")
    suspend fun getAllNotesSync(): List<LectureNote>

    @Query("DELETE FROM lecture_notes")
    suspend fun deleteAllNotes()
}

