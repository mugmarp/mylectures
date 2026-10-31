package com.mustime.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mustime.features.timetable.domain.Assignment
import com.mustime.features.timetable.domain.CustomEvent
import com.mustime.features.timetable.domain.LectureNote
import com.mustime.features.timetable.domain.TimetableEntry
import com.mustime.features.timetable.domain.TimetableMetadata

@Database(
    entities = [
        TimetableEntry::class,
        LectureNote::class,
        CustomEvent::class,
        Assignment::class,
        TimetableMetadata::class
    ],
    version = 12,
    exportSchema = true
)
@androidx.room.TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun timetableDao(): TimetableDao
    abstract fun customEventDao(): CustomEventDao
    abstract fun assignmentDao(): AssignmentDao

    companion object {
        const val PREPOPULATED_DB_NAME = "mustime_prepopulated.db"
        
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

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE timetable ADD COLUMN floor INTEGER DEFAULT NULL")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS timetable_metadata (
                        id INTEGER PRIMARY KEY NOT NULL,
                        versionLabel TEXT NOT NULL,
                        status TEXT NOT NULL,
                        isFinal INTEGER NOT NULL DEFAULT 1,
                        academicYear TEXT NOT NULL,
                        semester TEXT NOT NULL,
                        releaseNotes TEXT,
                        lastUpdated INTEGER NOT NULL,
                        source TEXT NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT OR REPLACE INTO timetable_metadata (
                        id, versionLabel, status, isFinal, academicYear, semester, releaseNotes, lastUpdated, source
                    ) VALUES (
                        1, 'Final Timetable', 'Official Final', 1, '2026/2027', 'Semester 1', 'Authoritative MUST Teaching Timetable approved by Academic Registrar', ${System.currentTimeMillis()}, 'MUST Academic Registrar'
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS timetable_metadata_old")
                db.execSQL("ALTER TABLE timetable_metadata RENAME TO timetable_metadata_old")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS timetable_metadata (
                        id INTEGER PRIMARY KEY NOT NULL,
                        versionLabel TEXT NOT NULL,
                        status TEXT NOT NULL,
                        isFinal INTEGER NOT NULL DEFAULT 1,
                        draftNumber INTEGER DEFAULT NULL,
                        academicYear TEXT NOT NULL,
                        semester TEXT NOT NULL,
                        releaseNotes TEXT,
                        lastUpdated INTEGER NOT NULL,
                        source TEXT NOT NULL,
                        totalSessions INTEGER NOT NULL DEFAULT 1304
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT OR REPLACE INTO timetable_metadata (
                        id, versionLabel, status, isFinal, draftNumber, academicYear, semester, releaseNotes, lastUpdated, source, totalSessions
                    ) VALUES (
                        1, 'Final Timetable', 'Official Final', 1, NULL, '2026/2027', 'Semester 1', 'Authoritative MUST Teaching Timetable approved by Academic Registrar', ${System.currentTimeMillis()}, 'MUST Academic Registrar', 1304
                    )
                """.trimIndent())
                db.execSQL("DROP TABLE IF EXISTS timetable_metadata_old")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Store a stable relative path to a copy of the picked attachment
                // inside files/attachments/. Legacy rows keep attachmentName only.
                db.execSQL("ALTER TABLE lecture_notes ADD COLUMN attachmentPath TEXT")
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Upgrade custom_events: add clientUuid, updatedAt, isDeleted, and index
                db.execSQL("ALTER TABLE custom_events ADD COLUMN clientUuid TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE custom_events ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE custom_events ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE custom_events SET clientUuid = lower(hex(randomblob(16))), updatedAt = ${System.currentTimeMillis()} WHERE clientUuid = ''")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_custom_events_clientUuid ON custom_events (clientUuid)")

                // 2. Upgrade assignments: add clientUuid, updatedAt, isDeleted, and index
                db.execSQL("ALTER TABLE assignments ADD COLUMN clientUuid TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE assignments ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE assignments ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE assignments SET clientUuid = lower(hex(randomblob(16))), updatedAt = ${System.currentTimeMillis()} WHERE clientUuid = ''")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_assignments_clientUuid ON assignments (clientUuid)")

                // 3. Upgrade lecture_notes: add isDeleted flag and index
                db.execSQL("ALTER TABLE lecture_notes ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_lecture_notes_isDeleted ON lecture_notes (isDeleted)")
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Multi-tenant user scoping & guest data isolation
                db.execSQL("ALTER TABLE custom_events ADD COLUMN ownerId TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE assignments ADD COLUMN ownerId TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE lecture_notes ADD COLUMN ownerId TEXT DEFAULT NULL")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_custom_events_ownerId ON custom_events (ownerId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_assignments_ownerId ON assignments (ownerId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_lecture_notes_ownerId ON lecture_notes (ownerId)")
            }
        }
    }
}


