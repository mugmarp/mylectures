package com.mustime

import com.mustime.features.timetable.domain.TimetableStage
import com.mustime.features.timetable.domain.TimetableVersionResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimetableVersionResolverTest {

    @Test
    fun testResolvesFinalTimetableAccurately() {
        val meta1 = TimetableVersionResolver.resolve("FINAL")
        assertTrue(meta1.isFinal)
        assertEquals(TimetableStage.FINAL, meta1.stage)
        assertEquals("Final Timetable", meta1.versionLabel)
        assertEquals("Official Final", meta1.status)
        assertEquals("FINAL", meta1.displayBadge)
        assertNull(meta1.draftNumber)

        val meta2 = TimetableVersionResolver.resolve("Final Timetable", isExplicitFinal = true)
        assertTrue(meta2.isFinal)
        assertEquals("Final Timetable · Official Release", meta2.displaySummary)

        val meta3 = TimetableVersionResolver.resolve("2026/2027 Final Teaching Schedule")
        assertTrue(meta3.isFinal)
    }

    @Test
    fun testResolvesDynamicDraftNumbersAccurately() {
        // Dynamic draft 1
        val draft1 = TimetableVersionResolver.resolve("Draft 1")
        assertFalse(draft1.isFinal)
        assertEquals(TimetableStage.DRAFT, draft1.stage)
        assertEquals(1, draft1.draftNumber)
        assertEquals("Draft 1", draft1.versionLabel)
        assertEquals("Active Draft", draft1.status)
        assertEquals("DRAFT 1", draft1.displayBadge)
        assertEquals("Draft 1 · Synced (Active Draft)", draft1.displaySummary)

        // Dynamic draft 2
        val draft2 = TimetableVersionResolver.resolve("Draft 2")
        assertFalse(draft2.isFinal)
        assertEquals(2, draft2.draftNumber)

        // Dynamic draft 3
        val draft3 = TimetableVersionResolver.resolve("Draft 3 (Week 2 Updates)")
        assertFalse(draft3.isFinal)
        assertEquals(3, draft3.draftNumber)
        assertEquals("DRAFT 3", draft3.displayBadge)

        // Dynamic draft 4 or higher (unlimited/dynamic draft ceiling)
        val draft4 = TimetableVersionResolver.resolve("Draft 4")
        assertFalse(draft4.isFinal)
        assertEquals(4, draft4.draftNumber)
        assertEquals("DRAFT 4", draft4.displayBadge)

        val draft12 = TimetableVersionResolver.resolve("Draft 12")
        assertFalse(draft12.isFinal)
        assertEquals(12, draft12.draftNumber)
    }

    @Test
    fun testResolvesRomanNumeralDrafts() {
        val draftII = TimetableVersionResolver.resolve("Draft II")
        assertFalse(draftII.isFinal)
        assertEquals(2, draftII.draftNumber)

        val draftIII = TimetableVersionResolver.resolve("Draft III")
        assertFalse(draftIII.isFinal)
        assertEquals(3, draftIII.draftNumber)
    }
}
