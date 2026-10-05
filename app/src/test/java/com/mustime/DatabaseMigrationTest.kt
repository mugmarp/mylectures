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
}
