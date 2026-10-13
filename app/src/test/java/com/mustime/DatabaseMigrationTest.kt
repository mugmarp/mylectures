package com.mustime

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.mustime.core.database.AppDatabase
import com.mustime.features.rooms.BuildingLevel
import com.mustime.features.timetable.domain.TimetableEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DatabaseMigrationTest {

    @Test
    fun testMigration6To7AddsFloorColumn() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase("test_migration.db")

        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name("test_migration.db")
            .callback(object : SupportSQLiteOpenHelper.Callback(6) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    // Version 6 schema for timetable table (no floor column)
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

                    db.execSQL("""
                        INSERT INTO timetable (
                            natural_key, program_group, day, time_slot, start_time, end_time,
                            course_code, course_title, session_type, lecturer, room, shared_with
                        ) VALUES (
                            'BCS I|CSC1101|Monday|08:00', 'BCS I', 'Monday', '08:00 - 10:00', '08:00', '10:00',
                            'CSC1101', 'Intro to Programming', 'Lecture', 'Dr. Akatukwasa', 'S403', '[]'
                        )
                    """.trimIndent())
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val db = helper.writableDatabase

        // Verify column count before migration
        fun getColumns(database: SupportSQLiteDatabase): List<String> {
            val list = mutableListOf<String>()
            database.query("PRAGMA table_info(timetable)").use { cursor ->
                val nameIdx = cursor.getColumnIndex("name")
                while (cursor.moveToNext()) {
                    list.add(cursor.getString(nameIdx))
                }
            }
            return list
        }

        val columnNamesBefore = getColumns(db)
        assertTrue(!columnNamesBefore.contains("floor"))

        // Run Migration 6 -> 7
        AppDatabase.MIGRATION_6_7.migrate(db)

        // Verify floor column exists after migration
        val columnNamesAfter = getColumns(db)
        assertTrue("floor column must exist after migration", columnNamesAfter.contains("floor"))

        // Verify original row is preserved with NULL floor
        db.query("SELECT natural_key, room, floor FROM timetable WHERE natural_key = 'BCS I|CSC1101|Monday|08:00'").use { cursorAfter ->
            assertTrue(cursorAfter.moveToFirst())
            val floorIndex = cursorAfter.getColumnIndex("floor")
            assertTrue(cursorAfter.isNull(floorIndex))
            val roomIndex = cursorAfter.getColumnIndex("room")
            assertEquals("S403", cursorAfter.getString(roomIndex))
        }

        // Insert new row with floor integer
        db.execSQL("""
            INSERT INTO timetable (
                natural_key, program_group, day, time_slot, start_time, end_time,
                course_code, course_title, session_type, lecturer, room, shared_with, floor
            ) VALUES (
                'BCS I|CSC1102|Tuesday|10:00', 'BCS I', 'Tuesday', '10:00 - 12:00', '10:00', '12:00',
                'CSC1102', 'Data Structures', 'Lecture', 'Dr. Mugisha', 'S204', '[]', 2
            )
        """.trimIndent())

        db.query("SELECT * FROM timetable WHERE natural_key = 'BCS I|CSC1102|Tuesday|10:00'").use { cursorNew ->
            assertTrue(cursorNew.moveToFirst())
            assertEquals(2, cursorNew.getInt(cursorNew.getColumnIndexOrThrow("floor")))
        }

        db.close()
        helper.close()
        context.deleteDatabase("test_migration.db")
    }

    @Test
    fun testMigration4To5AddsCategoryAndColorTag() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase("test_migration_4_5.db")

        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name("test_migration_4_5.db")
            .callback(object : SupportSQLiteOpenHelper.Callback(4) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS custom_events (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            title TEXT NOT NULL,
                            dayOfWeek TEXT NOT NULL,
                            startTime TEXT NOT NULL,
                            endTime TEXT NOT NULL,
                            venue TEXT,
                            description TEXT
                        )
                    """.trimIndent())
                    db.execSQL("INSERT INTO custom_events (title, dayOfWeek, startTime, endTime) VALUES ('Study Group', 'Monday', '10:00', '12:00')")
                }
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val db = helper.writableDatabase

        AppDatabase.MIGRATION_4_5.migrate(db)

        db.query("SELECT category, colorTag FROM custom_events WHERE title = 'Study Group'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Study", cursor.getString(0))
            assertEquals("#2563EB", cursor.getString(1))
        }

        db.close()
        helper.close()
        context.deleteDatabase("test_migration_4_5.db")
    }

    @Test
    fun testMigration5To6AddsNotesColumns() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase("test_migration_5_6.db")

        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name("test_migration_5_6.db")
            .callback(object : SupportSQLiteOpenHelper.Callback(5) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS lecture_notes (
                            naturalKey TEXT NOT NULL PRIMARY KEY,
                            content TEXT NOT NULL,
                            updatedAt INTEGER NOT NULL,
                            colourTag TEXT,
                            alarmMinutes INTEGER
                        )
                    """.trimIndent())
                    db.execSQL("INSERT INTO lecture_notes (naturalKey, content, updatedAt) VALUES ('note1', 'Test content', 1000)")
                }
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val db = helper.writableDatabase

        AppDatabase.MIGRATION_5_6.migrate(db)

        db.query("SELECT naturalKey, title, isPinned FROM lecture_notes WHERE naturalKey = 'note1'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("note1", cursor.getString(0))
            assertNull(cursor.getString(1))
            assertEquals(0, cursor.getInt(2))
        }

        db.close()
        helper.close()
        context.deleteDatabase("test_migration_5_6.db")
    }

    @Test
    fun testTimetableEntryFloorLevelResolution() {
        // Entry with explicit floor stored in database
        val entryWithFloor = TimetableEntry(
            natural_key = "key1",
            program_group = "BCS I",
            day = "Monday",
            time_slot = "08:00 - 10:00",
            start_time = "08:00",
            end_time = "10:00",
            course_code = "CSC101",
            course_title = "CS",
            session_type = "Lecture",
            lecturer = "Staff",
            room = "Custom Room",
            floor = 3
        )
        assertEquals(BuildingLevel.THIRD, entryWithFloor.floorLevel)
        assertEquals(3, entryWithFloor.floor)

        // Entry with null floor falls back to directory room resolution
        val entryFallbackDirectory = TimetableEntry(
            natural_key = "key2",
            program_group = "BCS I",
            day = "Monday",
            time_slot = "08:00 - 10:00",
            start_time = "08:00",
            end_time = "10:00",
            course_code = "CSC101",
            course_title = "CS",
            session_type = "Lecture",
            lecturer = "Staff",
            room = "S403",
            floor = null
        )
        assertEquals(BuildingLevel.FOURTH, entryFallbackDirectory.floorLevel)
    }

    @Test
    fun testBuildingLevelFromFloorNumber() {
        assertEquals(BuildingLevel.GROUND, BuildingLevel.fromFloorNumber(0))
        assertEquals(BuildingLevel.FIRST, BuildingLevel.fromFloorNumber(1))
        assertEquals(BuildingLevel.SECOND, BuildingLevel.fromFloorNumber(2))
        assertEquals(BuildingLevel.THIRD, BuildingLevel.fromFloorNumber(3))
        assertEquals(BuildingLevel.FOURTH, BuildingLevel.fromFloorNumber(4))
        assertNull(BuildingLevel.fromFloorNumber(null))
        assertNull(BuildingLevel.fromFloorNumber(99))
    }

    @Test
    fun testMigration7To8CreatesTimetableMetadataTable() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase("test_migration_7_8.db")

        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name("test_migration_7_8.db")
            .callback(object : SupportSQLiteOpenHelper.Callback(7) {
                override fun onCreate(db: SupportSQLiteDatabase) {
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
                            shared_with TEXT NOT NULL,
                            floor INTEGER DEFAULT NULL
                        )
                    """.trimIndent())
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val db = helper.writableDatabase

        AppDatabase.MIGRATION_7_8.migrate(db)

        val cursor = db.query("SELECT versionLabel, status, isFinal, academicYear FROM timetable_metadata WHERE id = 1")
        assertTrue(cursor.moveToFirst())
        assertEquals("Final Timetable", cursor.getString(0))
        assertEquals("Official Final", cursor.getString(1))
        assertEquals(1, cursor.getInt(2))
        assertEquals("2026/2027", cursor.getString(3))
        cursor.close()
        db.close()
    }

    @Test
    fun testMigration8To9AddsDraftNumberAndTotalSessions() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase("test_migration_8_9.db")

        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name("test_migration_8_9.db")
            .callback(object : SupportSQLiteOpenHelper.Callback(8) {
                override fun onCreate(db: SupportSQLiteDatabase) {
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
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val db = helper.writableDatabase

        AppDatabase.MIGRATION_8_9.migrate(db)

        val cursor = db.query("SELECT versionLabel, status, isFinal, academicYear, draftNumber, totalSessions FROM timetable_metadata WHERE id = 1")
        assertTrue(cursor.moveToFirst())
        assertEquals("Final Timetable", cursor.getString(0))
        assertEquals("Official Final", cursor.getString(1))
        assertEquals(1, cursor.getInt(2))
        assertEquals("2026/2027", cursor.getString(3))
        assertTrue(cursor.isNull(4))
        assertEquals(1304, cursor.getInt(5))
        cursor.close()
        db.close()
    }

    /**
     * v10 adds lecture_notes.attachmentPath, which stores the relative path of an
     * attachment copied into files/attachments/.
     *
     * Guards two failure modes: the column must exist after upgrading (otherwise
     * Room's schema validation rejects the open), and pre-existing notes must
     * survive with attachmentPath NULL — a legacy row keeps its name-only
     * attachmentName and must not be dropped.
     */
    @Test
    fun testMigration9To10AddsAttachmentPathAndPreservesExistingNotes() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase("test_migration_9_10.db")

        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name("test_migration_9_10.db")
            .callback(object : SupportSQLiteOpenHelper.Callback(9) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    // Version 9 schema: no attachmentPath column.
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS lecture_notes (
                            naturalKey TEXT NOT NULL PRIMARY KEY,
                            content TEXT NOT NULL,
                            updatedAt INTEGER NOT NULL,
                            colourTag TEXT,
                            alarmMinutes INTEGER,
                            title TEXT,
                            tag TEXT,
                            attachedClass TEXT,
                            attachmentName TEXT,
                            isPinned INTEGER NOT NULL
                        )
                    """.trimIndent())

                    // A pre-upgrade note carrying a name-only attachment.
                    db.execSQL("""
                        INSERT INTO lecture_notes (
                            naturalKey, content, updatedAt, title, attachmentName, isPinned
                        ) VALUES (
                            'CSC1101|Legacy|Monday|08:00', 'legacy body', 1700000000000,
                            'Legacy Note', 'old_slides.pdf', 1
                        )
                    """.trimIndent())
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val db = helper.writableDatabase

        AppDatabase.MIGRATION_9_10.migrate(db)

        // The column must exist, be nullable TEXT, and default to NULL.
        val columns = mutableMapOf<String, Pair<String, Boolean>>()
        db.query("PRAGMA table_info(`lecture_notes`)").use { cursor ->
            while (cursor.moveToNext()) {
                columns[cursor.getString(1)] = cursor.getString(2) to (cursor.getInt(3) == 1)
            }
        }
        assertTrue("attachmentPath column missing after migration", columns.containsKey("attachmentPath"))
        assertEquals("TEXT" to false, columns["attachmentPath"])

        // The pre-existing row survives with a NULL path.
        db.query(
            "SELECT naturalKey, content, attachmentName, attachmentPath FROM lecture_notes WHERE naturalKey = ?",
            arrayOf("CSC1101|Legacy|Monday|08:00")
        ).use { cursor ->
            assertTrue("legacy note was dropped by the migration", cursor.moveToFirst())
            assertEquals("legacy body", cursor.getString(1))
            assertEquals("old_slides.pdf", cursor.getString(2))
            assertTrue("legacy row must have NULL attachmentPath", cursor.isNull(3))
        }

        // And the column is writable for new attachments.
        db.execSQL(
            "UPDATE lecture_notes SET attachmentPath = ? WHERE naturalKey = ?",
            arrayOf("attachments/1700000000000_new_slides.pdf", "CSC1101|Legacy|Monday|08:00")
        )
        db.query(
            "SELECT attachmentPath FROM lecture_notes WHERE naturalKey = ?",
            arrayOf("CSC1101|Legacy|Monday|08:00")
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("attachments/1700000000000_new_slides.pdf", cursor.getString(0))
        }

        db.close()
    }
}
