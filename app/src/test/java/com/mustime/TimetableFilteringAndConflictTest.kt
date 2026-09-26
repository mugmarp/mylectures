package com.mustime

import com.mustime.features.timetable.domain.ConflictDetail
import com.mustime.features.timetable.domain.CustomEvent
import com.mustime.features.timetable.domain.TimetableConflictDetector
import com.mustime.features.timetable.domain.TimetableEntry
import com.mustime.features.timetable.domain.TimetableMatcher
import org.junit.Assert.*
import org.junit.Test

class TimetableFilteringAndConflictTest {

    private fun createEntry(
        key: String,
        group: String,
        day: String,
        startTime: String,
        endTime: String,
        courseCode: String,
        courseTitle: String = "Test Course",
        sharedWith: List<String> = emptyList()
    ): TimetableEntry {
        return TimetableEntry(
            natural_key = key,
            program_group = group,
            day = day,
            time_slot = "$startTime - $endTime",
            start_time = startTime,
            end_time = endTime,
            course_code = courseCode,
            course_title = courseTitle,
            session_type = "Lecture",
            lecturer = "Dr. Lecturer",
            room = "Hall A",
            shared_with = sharedWith
        )
    }

    // =========================================================================
    // 1. TIMETABLE GROUP FILTERING TESTS
    // =========================================================================

    @Test
    fun testOfficialFacultyGroupParsing() {
        assertEquals("BCS I", TimetableMatcher.parseGroup("BCS I"))
        assertEquals("BIT II", TimetableMatcher.parseGroup("BIT II"))
        assertEquals("BSE III", TimetableMatcher.parseGroup("BSE III"))
        assertEquals("MBR I", TimetableMatcher.parseGroup("MBR I"))
        assertEquals("PHA II", TimetableMatcher.parseGroup("PHA II"))
        assertEquals("BBA I", TimetableMatcher.parseGroup("BBA I"))
    }

    @Test
    fun testHistoricalAndAlternativeAliasesParsing() {
        // MLC -> MLS (Medical Laboratory Sciences)
        assertEquals("MLS I", TimetableMatcher.parseGroup("MLC I"))
        // BNC -> BNS (Nursing Science)
        assertEquals("BNS II", TimetableMatcher.parseGroup("BNC II"))
        // CIV -> CVE (Civil Engineering)
        assertEquals("CVE I", TimetableMatcher.parseGroup("CIV I"))
        // PEM -> PEEM (Petroleum Engineering)
        assertEquals("PEEM II", TimetableMatcher.parseGroup("PEM II"))
        // BAF -> BSAF (Accounting & Finance)
        assertEquals("BSAF III", TimetableMatcher.parseGroup("BAF III"))
    }

    @Test
    fun testSharedWithCourseFiltering() {
        val entry = createEntry(
            key = "MBR101|Gross Anatomy|Mon|08:00",
            group = "MBR I",
            day = "Monday",
            startTime = "08:00",
            endTime = "10:00",
            courseCode = "ANA1101",
            sharedWith = listOf("PHA I", "BNS I")
        )

        // Matches main group
        assertTrue("Entry should match primary group MBR I", TimetableMatcher.entryMatchesGroup(entry, "MBR I"))
        // Matches shared groups
        assertTrue("Entry should match shared group PHA I", TimetableMatcher.entryMatchesGroup(entry, "PHA I"))
        assertTrue("Entry should match shared group BNS I", TimetableMatcher.entryMatchesGroup(entry, "BNS I"))
        // Does not match unrelated group
        assertFalse("Entry should not match BCS I", TimetableMatcher.entryMatchesGroup(entry, "BCS I"))
    }

    @Test
    fun testDayFilteringAndSorting() {
        val entryMon1 = createEntry("k1", "BCS I", "Monday", "11:00", "13:00", "CS102")
        val entryMon2 = createEntry("k2", "BCS I", "Monday", "08:00", "10:00", "CS101")
        val entryTue = createEntry("k3", "BCS I", "Tuesday", "09:00", "11:00", "CS103")

        val all = listOf(entryMon1, entryMon2, entryTue)

        // Filter Monday
        val mondayClasses = TimetableMatcher.filterTimetable(all, "BCS I", "Monday")
        assertEquals(2, mondayClasses.size)
        // Must be sorted chronologically by start time
        assertEquals("08:00", mondayClasses[0].startTime)
        assertEquals("11:00", mondayClasses[1].startTime)

        // Filter Tuesday
        val tuesdayClasses = TimetableMatcher.filterTimetable(all, "BCS I", "Tuesday")
        assertEquals(1, tuesdayClasses.size)
        assertEquals("CS103", tuesdayClasses[0].courseCode)

        // Filter Wednesday (empty)
        val wedClasses = TimetableMatcher.filterTimetable(all, "BCS I", "Wednesday")
        assertTrue(wedClasses.isEmpty())
    }

    // =========================================================================
    // 2. TIMETABLE CONFLICT DETECTION TESTS
    // =========================================================================

    @Test
    fun testDirectLectureConflictDetection() {
        // Class A: 08:00 - 10:00 on Monday
        val classA = createEntry("A", "BCS I", "Monday", "08:00", "10:00", "CSC1101", "Calculus I")
        // Class B: 09:00 - 11:00 on Monday (1-hour overlap)
        val classB = createEntry("B", "BCS I", "Monday", "09:00", "11:00", "CSC1102", "Data Structures")

        val conflicts = TimetableConflictDetector.findConflicts(listOf(classA, classB))
        assertEquals(1, conflicts.size)

        val conflict = conflicts[0]
        assertEquals("Monday", conflict.day)
        assertEquals("09:00", conflict.overlapStart)
        assertEquals("10:00", conflict.overlapEnd)
        assertEquals(60, conflict.overlapMinutes)

        assertTrue(TimetableConflictDetector.hasConflict(classA, listOf(classB)))
        assertTrue(TimetableConflictDetector.hasConflict(classB, listOf(classA)))
    }

    @Test
    fun testBackToBackLecturesDoNotConflict() {
        // Class A: 08:00 - 10:00
        val classA = createEntry("A", "BCS I", "Monday", "08:00", "10:00", "CSC1101")
        // Class B: 10:00 - 12:00 (Starts exactly when A ends)
        val classB = createEntry("B", "BCS I", "Monday", "10:00", "12:00", "CSC1102")

        val conflicts = TimetableConflictDetector.findConflicts(listOf(classA, classB))
        assertTrue("Back-to-back classes must not produce conflicts", conflicts.isEmpty())
        assertFalse(TimetableConflictDetector.hasConflict(classA, listOf(classB)))
    }

    @Test
    fun testSameTimeOnDifferentDaysDoNotConflict() {
        // Class Monday 09:00 - 11:00
        val mondayClass = createEntry("Mon", "BCS I", "Monday", "09:00", "11:00", "CSC1101")
        // Class Tuesday 09:00 - 11:00
        val tuesdayClass = createEntry("Tue", "BCS I", "Tuesday", "09:00", "11:00", "CSC1102")

        val conflicts = TimetableConflictDetector.findConflicts(listOf(mondayClass, tuesdayClass))
        assertTrue("Classes on different days must not conflict", conflicts.isEmpty())
    }

    @Test
    fun testFullyEnclosedLectureConflict() {
        // Outer 3-hour Lab: 09:00 - 12:00
        val lab = createEntry("Lab", "BCS I", "Friday", "09:00", "12:00", "CSC1103", "Hardware Lab")
        // Inner Tutorial: 10:00 - 11:00
        val tutorial = createEntry("Tut", "BCS I", "Friday", "10:00", "11:00", "CSC1104", "Discrete Math")

        val conflicts = TimetableConflictDetector.findConflicts(listOf(lab, tutorial))
        assertEquals(1, conflicts.size)
        assertEquals("10:00", conflicts[0].overlapStart)
        assertEquals("11:00", conflicts[0].overlapEnd)
        assertEquals(60, conflicts[0].overlapMinutes)
    }

    @Test
    fun testConflictWithCustomStudentEvent() {
        val lecture = createEntry("Lec", "BCS I", "Wednesday", "14:00", "16:00", "CSC2101", "Database Systems")

        val overlappingEvent = CustomEvent(
            id = 1L,
            title = "Guild Committee Meeting",
            dayOfWeek = "Wednesday",
            startTime = "15:00",
            endTime = "17:00"
        )

        val nonOverlappingEvent = CustomEvent(
            id = 2L,
            title = "Evening Football Training",
            dayOfWeek = "Wednesday",
            startTime = "17:00",
            endTime = "18:30"
        )

        val eventOtherDay = CustomEvent(
            id = 3L,
            title = "Study Session",
            dayOfWeek = "Thursday",
            startTime = "14:00",
            endTime = "16:00"
        )

        assertTrue("Lecture should conflict with 15:00-17:00 meeting", TimetableConflictDetector.hasConflictWithEvent(lecture, overlappingEvent))
        assertFalse("Lecture should not conflict with 17:00 training", TimetableConflictDetector.hasConflictWithEvent(lecture, nonOverlappingEvent))
        assertFalse("Lecture should not conflict with event on another day", TimetableConflictDetector.hasConflictWithEvent(lecture, eventOtherDay))
    }

    @Test
    fun testDeduplicateReplicatedSharedLectures() {
        // Same course session replicated across 3 different cohorts in dataset
        val rep1 = createEntry(
            key = "AB_MED|GRAND|Tuesday|14:00",
            group = "AB- MED/PCH",
            day = "Tuesday",
            startTime = "14:00",
            endTime = "16:00",
            courseCode = "GRAND",
            courseTitle = "Grand Rounds",
            sharedWith = listOf("MBR III", "MLS III", "PHA III")
        )
        val rep2 = createEntry(
            key = "MBR_III|GRAND|Tuesday|14:00",
            group = "MBR III",
            day = "Tuesday",
            startTime = "14:00",
            endTime = "16:00",
            courseCode = "GRAND",
            courseTitle = "Grand Rounds",
            sharedWith = listOf("MLS III", "PHA III")
        )
        val rep3 = createEntry(
            key = "PHA_III|GRAND|Tuesday|14:00",
            group = "PHA III",
            day = "Tuesday",
            startTime = "14:00",
            endTime = "16:00",
            courseCode = "GRAND",
            courseTitle = "Grand Rounds",
            sharedWith = listOf("MBR III", "MLS III")
        )

        val rawList = listOf(rep1, rep2, rep3)

        // For student enrolled in MBR III:
        val deduped = TimetableMatcher.filterTimetable(rawList, "MBR III", "Tuesday")

        assertEquals("Replicated sessions must be merged into exactly 1 session", 1, deduped.size)
        val singleSession = deduped.first()
        assertEquals("GRAND", singleSession.courseCode)
        assertEquals("14:00", singleSession.startTime)
        assertEquals("16:00", singleSession.endTime)
        // Ensure student's enrolled group was preferred as primary
        assertEquals("MBR III", singleSession.program_group)
    }
}
