package com.lectures

import com.lectures.core.alarm.AlarmScheduler
import com.lectures.core.alarm.TaskDateTimeParser
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.Callable
import java.util.concurrent.Executors

class AlarmDateCalculationTest {
    @Test
    fun earlyMondayClassCanTriggerOnSundayAndMissedLeadMovesToNextWeek() {
        val before = at(2026, Calendar.OCTOBER, 4, 23, 30)
        val trigger = AlarmScheduler.calculateNextTriggerMillis(Calendar.MONDAY, 0, 15, 30, before)
        assertEquals(at(2026, Calendar.OCTOBER, 4, 23, 45), trigger)
        val after = at(2026, Calendar.OCTOBER, 4, 23, 50)
        assertEquals(at(2026, Calendar.OCTOBER, 11, 23, 45),
            AlarmScheduler.calculateNextTriggerMillis(Calendar.MONDAY, 0, 15, 30, after))
    }

    @Test
    fun upcomingClassWithinOneMinuteIsNotSkippedForAWeek() {
        val now = at(2026, Calendar.OCTOBER, 5, 8, 59) + 30_000L
        assertEquals(now + 30_000L,
            AlarmScheduler.calculateNextTriggerMillis(Calendar.MONDAY, 9, 0, 0, now))
    }

    @Test
    fun fullDatesRetainTheirExplicitTimeAndPastYear() {
        val expected = at(2050, Calendar.NOVEMBER, 14, 14, 30)
        assertEquals(expected, TaskDateTimeParser.calculateDueMillis("2050-11-14 14:30"))
        assertEquals(expected, TaskDateTimeParser.calculateDueMillis("Nov 14, 2050 • 14:30"))
        val old = TaskDateTimeParser.calculateDueMillis("1999-01-01 12:00")!!
        assertEquals(1999, Calendar.getInstance().apply { timeInMillis = old }.get(Calendar.YEAR))
    }

    @Test
    fun invalidDatesAndDisabledRemindersDoNotProduceAlarmTimes() {
        assertNull(TaskDateTimeParser.calculateDueMillis("2050-02-30 14:00"))
        assertNull(TaskDateTimeParser.calculateDueMillis("Monday, Feb 30, 2050 14:00"))
        assertNull(TaskDateTimeParser.calculateDueMillis("Tomorrow 25:00"))
        assertNull(TaskDateTimeParser.calculateDueMillis("unknown deadline"))
        assertNull(TaskDateTimeParser.calculateTriggerTimeMillis("2050-11-14 14:30", null))
        assertNull(TaskDateTimeParser.calculateTriggerTimeMillis("2050-11-14 14:30", -1))
    }

    @Test
    fun dateOnlyDeadlinesStayAtEndOfDayAndNormalizationKeepsIsoDates() {
        val due = TaskDateTimeParser.calculateDueMillis("2050-11-14")!!
        val calendar = Calendar.getInstance().apply { timeInMillis = due }
        assertEquals(23, calendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, calendar.get(Calendar.MINUTE))
        assertEquals("2050-11-14", TaskDateTimeParser.normalizeDueDate("2050-11-14 14:30"))
        assertEquals("2050-11-14", TaskDateTimeParser.normalizeDueDate("2050-11-14 • 14:30"))
        assertEquals("Friday", TaskDateTimeParser.normalizeDueDate("Friday, 17:00"))
    }

    @Test
    fun concurrentDateParsingDoesNotShareMutableFormatters() {
        val executor = Executors.newFixedThreadPool(4)
        try {
            val expected = at(2050, Calendar.NOVEMBER, 14, 14, 30)
            val results = executor.invokeAll((1..40).map {
                Callable { TaskDateTimeParser.calculateDueMillis("2050-11-14 14:30") }
            })
            results.forEach { assertEquals(expected, it.get()) }
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun englishSavedDatesParseAfterDeviceLocaleChanges() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.FRANCE)
            assertEquals(at(2050, Calendar.NOVEMBER, 14, 14, 30),
                TaskDateTimeParser.calculateDueMillis("Nov 14, 2050 14:30"))
        } finally {
            Locale.setDefault(original)
        }
    }

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int) = Calendar.getInstance().apply {
        clear()
        set(year, month, day, hour, minute, 0)
    }.timeInMillis
}
