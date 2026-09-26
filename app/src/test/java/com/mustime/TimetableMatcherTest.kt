package com.mustime

import com.mustime.features.timetable.domain.TimetableEntry
import com.mustime.features.timetable.domain.TimetableMatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimetableMatcherTest {

    @Test
    fun testParseGroup() {
        assertEquals("MBR I", TimetableMatcher.parseGroup("MBR I"))
        assertEquals("BCS I", TimetableMatcher.parseGroup("BCS I"))
        assertEquals("MLS I", TimetableMatcher.parseGroup("MLC I")) // MLC alias
        assertEquals("BNS II", TimetableMatcher.parseGroup("BNC II")) // BNC alias
        assertEquals("CVE I", TimetableMatcher.parseGroup("CIV I")) // CIV alias
        assertEquals("PEEM I", TimetableMatcher.parseGroup("PEM I")) // PEM alias
        assertEquals("BSAF I", TimetableMatcher.parseGroup("BAF I")) // BAF alias
    }

    @Test
    fun testEntryMatchesGroupDirect() {
        val entry = TimetableEntry(
            natural_key = "MBR I|ANA1101|Monday|08:00",
            program_group = "MBR I",
            day = "Monday",
            time_slot = "08:00 - 10:00",
            start_time = "08:00",
            end_time = "10:00",
            course_code = "ANA1101",
            course_title = "Gross Anatomy",
            session_type = "Lecture",
            lecturer = "Dr. Tumusiime",
            room = "Anatomy Lab",
            shared_with = listOf("PHA I")
        )

        assertTrue(TimetableMatcher.entryMatchesGroup(entry, "MBR I"))
        assertTrue(TimetableMatcher.entryMatchesGroup(entry, "PHA I"))
    }

    @Test
    fun testEntryMatchesGroupWithAlias() {
        val entry = TimetableEntry(
            natural_key = "MLC I|MLS1101|Tuesday|09:00",
            program_group = "MLC I",
            day = "Tuesday",
            time_slot = "09:00 - 11:00",
            start_time = "09:00",
            end_time = "11:00",
            course_code = "MLS1101",
            course_title = "Clinical Microbiology",
            session_type = "Lecture",
            lecturer = "Dr. Kato",
            room = "MLS Lab",
            shared_with = emptyList()
        )

        assertTrue(TimetableMatcher.entryMatchesGroup(entry, "MLS I"))
    }
}
