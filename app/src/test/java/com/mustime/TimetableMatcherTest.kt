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

    @Test
    fun testTrackGroupMatchingAndInheritance() {
        // Shared track entry: biological + chemistry/maths tracks attend together
        val sharedEntry = TimetableEntry(
            natural_key = "BS BIOLOGICAL I|CHE1101|Monday|11:00",
            program_group = "BS BIOLOGICAL I",
            day = "Monday",
            time_slot = "11:00 - 13:00",
            start_time = "11:00",
            end_time = "13:00",
            course_code = "CHE1101",
            course_title = "Basic Inorganic Chemistry",
            session_type = "Lecture",
            lecturer = "Dr. Chemistry",
            room = "Lab 1",
            shared_with = listOf("BS CHEM MATHS I")
        )

        // General parent group BS I matches
        assertTrue(TimetableMatcher.entryMatchesGroup(sharedEntry, "BS I"))
        // Both specific tracks match
        assertTrue(TimetableMatcher.entryMatchesGroup(sharedEntry, "BS BIOLOGICAL I"))
        assertTrue(TimetableMatcher.entryMatchesGroup(sharedEntry, "BS CHEM MATHS I"))
        // Distinct physical track does not match
        org.junit.Assert.assertFalse(TimetableMatcher.entryMatchesGroup(sharedEntry, "BS PHYSICAL I"))
    }

    @Test
    fun testGetGroupCountCalculationFromEntries() {
        val e1 = TimetableEntry(
            natural_key = "DLT BIOLOGY II|BIO2101|Monday|08:00",
            program_group = "DLT BIOLOGY II",
            day = "Monday",
            time_slot = "08:00 - 10:00",
            start_time = "08:00",
            end_time = "10:00",
            course_code = "BIO2101",
            course_title = "Plant Physiology",
            session_type = "Lecture",
            lecturer = "Staff",
            room = "SCI 1",
            shared_with = emptyList()
        )
        val e2 = TimetableEntry(
            natural_key = "DLT CHEMISTRY II|CHE2101|Tuesday|08:00",
            program_group = "DLT CHEMISTRY II",
            day = "Tuesday",
            time_slot = "08:00 - 10:00",
            start_time = "08:00",
            end_time = "10:00",
            course_code = "CHE2101",
            course_title = "Organic Chem",
            session_type = "Lecture",
            lecturer = "Staff",
            room = "SCI 2",
            shared_with = emptyList()
        )
        val entries = listOf(e1, e2)

        // Parent group DLT II matches both
        assertEquals(2, TimetableMatcher.getGroupCount("DLT II", entries))
        // Specific track matches only its session
        assertEquals(1, TimetableMatcher.getGroupCount("DLT BIOLOGY II", entries))
        assertEquals(1, TimetableMatcher.getGroupCount("DLT CHEMISTRY II", entries))
        assertEquals(0, TimetableMatcher.getGroupCount("DLT PHYSICS II", entries))
    }

    @Test
    fun testPrecomputedGroupCountsIntegrity() {
        // Verify key precomputed counts
        assertEquals(17, TimetableMatcher.PRECOMPUTED_GROUP_COUNTS["MBR I"])
        assertEquals(12, TimetableMatcher.PRECOMPUTED_GROUP_COUNTS["BGWH III"])
        assertEquals(15, TimetableMatcher.PRECOMPUTED_GROUP_COUNTS["BS III"])
        assertEquals(18, TimetableMatcher.PRECOMPUTED_GROUP_COUNTS["DLT II"])
        assertEquals(17, TimetableMatcher.PRECOMPUTED_GROUP_COUNTS["BBA II"])

        // Verify BBA TAXATION II was removed
        org.junit.Assert.assertNull(TimetableMatcher.PRECOMPUTED_GROUP_COUNTS["BBA TAXATION II"])
    }

    @Test
    fun testProgrammeTracksAndGroupsGeneration() {
        val dlt = com.mustime.features.onboarding.Programme(
            code = "DLT",
            name = "Diploma in Science Laboratory Technology",
            years = 2,
            tracks = mapOf("II" to listOf("BIOLOGY", "CHEMISTRY", "PHYSICS"))
        )
        val dltGroups = dlt.allGroups
        assertEquals(listOf("DLT I", "DLT II", "DLT BIOLOGY II", "DLT CHEMISTRY II", "DLT PHYSICS II"), dltGroups)

        val bgwh = com.mustime.features.onboarding.Programme(
            code = "BGWH",
            name = "BSc in Gender and Applied Women Health",
            years = 3,
            customYears = listOf("III")
        )
        val bgwhGroups = bgwh.allGroups
        assertEquals(listOf("BGWH III"), bgwhGroups)
    }
}
