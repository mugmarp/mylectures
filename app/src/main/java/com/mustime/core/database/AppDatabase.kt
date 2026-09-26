package com.mustime.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mustime.features.timetable.domain.Assignment
import com.mustime.features.timetable.domain.CustomEvent
import com.mustime.features.timetable.domain.LectureNote
import com.mustime.features.timetable.domain.TimetableEntry

@Database(
    entities = [
        TimetableEntry::class,
        LectureNote::class,
        CustomEvent::class,
        Assignment::class
    ],
    version = 6,
    exportSchema = false
)
@androidx.room.TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun timetableDao(): TimetableDao
    abstract fun customEventDao(): CustomEventDao
    abstract fun assignmentDao(): AssignmentDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Migrate lecture_notes: recreation with naturalKey as PrimaryKey
                db.execSQL("DROP TABLE IF EXISTS lecture_notes_old")
                db.execSQL("ALTER TABLE lecture_notes RENAME TO lecture_notes_old")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS lecture_notes (
                        naturalKey TEXT NOT NULL PRIMARY KEY,
                        content TEXT NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        colourTag TEXT,
                        alarmMinutes INTEGER
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT OR REPLACE INTO lecture_notes (naturalKey, content, updatedAt, colourTag, alarmMinutes)
                    SELECT naturalKey, content, updatedAt, colourTag, alarmMinutes FROM lecture_notes_old
                """.trimIndent())
                db.execSQL("DROP TABLE IF EXISTS lecture_notes_old")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_lecture_notes_updatedAt ON lecture_notes (updatedAt)")

                // Add indices to assignments
                db.execSQL("CREATE INDEX IF NOT EXISTS index_assignments_completed ON assignments (completed)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_assignments_dueDate ON assignments (dueDate)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_assignments_courseCode ON assignments (courseCode)")

                // Add indices to custom_events
                db.execSQL("CREATE INDEX IF NOT EXISTS index_custom_events_dayOfWeek_startTime ON custom_events (dayOfWeek, startTime)")

                // Add indices to timetable_entries
                db.execSQL("CREATE INDEX IF NOT EXISTS index_timetable_entries_programmeGroup_startTime ON timetable_entries (programmeGroup, startTime)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_timetable_entries_dayOfWeek_startTime ON timetable_entries (dayOfWeek, startTime)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_timetable_entries_courseCode ON timetable_entries (courseCode)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS timetable")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS timetable (
                        natural_key TEXT NOT NULL PRIMARY KEY,
                        program_group TEXT NOT NULL,
                        day TEXT NOT NULL,
                        time_slot TEXT NOT NULL,
                        start_time TEXT NOT NULL,
                        end_time TEXT,
                        course_code TEXT NOT NULL,
                        course_title TEXT NOT NULL,
                        session_type TEXT,
                        lecturer TEXT,
                        room TEXT,
                        shared_with TEXT NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_timetable_program_group_start_time ON timetable (program_group, start_time)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_timetable_day_start_time ON timetable (day, start_time)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_timetable_course_code ON timetable (course_code)")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE custom_events ADD COLUMN category TEXT NOT NULL DEFAULT 'Study'")
                db.execSQL("ALTER TABLE custom_events ADD COLUMN colorTag TEXT NOT NULL DEFAULT '#2563EB'")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE lecture_notes ADD COLUMN title TEXT")
                db.execSQL("ALTER TABLE lecture_notes ADD COLUMN tag TEXT")
                db.execSQL("ALTER TABLE lecture_notes ADD COLUMN attachedClass TEXT")
                db.execSQL("ALTER TABLE lecture_notes ADD COLUMN attachmentName TEXT")
                db.execSQL("ALTER TABLE lecture_notes ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}

