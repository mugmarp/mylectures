package com.mustime

import com.mustime.features.tasks.TaskMetricsCalculator
import com.mustime.features.timetable.domain.Assignment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.*

class TaskDashboardMetricsTest {

    @Test
    fun testEmptyTaskListMetrics() {
        val metrics = TaskMetricsCalculator.calculateWeeklyMetrics(emptyList())
        assertEquals(0, metrics.totalCount)
        assertEquals(0, metrics.completedCount)
        assertEquals(0, metrics.pendingCount)
        assertEquals(0, metrics.completionPercentage)
        assertEquals(7, metrics.days.size)
        assertEquals(0, metrics.highPriorityPending)
    }

    @Test
    fun testCompletedVsPendingMetricsCalculation() {
        val testCal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 8, 10, 0, 0) // Tuesday
        }

        val tasks = listOf(
            Assignment(
                id = 1,
                title = "Anatomy Dissection Report",
                courseCode = "ANA1101",
                dueDate = "Tuesday, 14:00",
                priority = "High",
                notes = "",
                completed = true
            ),
            Assignment(
                id = 2,
                title = "Biochemistry Lab Quiz",
                courseCode = "BCH1101",
                dueDate = "Wednesday, 09:00",
                priority = "High",
                notes = "",
                completed = false
            ),
            Assignment(
                id = 3,
                title = "Physiology Article Review",
                courseCode = "PHS1101",
                dueDate = "Friday, 17:00",
                priority = "Medium",
                notes = "",
                completed = false
            ),
            Assignment(
                id = 4,
                title = "Community Health Summary",
                courseCode = "COM1101",
                dueDate = "Thursday, 12:00",
                priority = "Low",
                notes = "",
                completed = true
            )
        )

        val metrics = TaskMetricsCalculator.calculateWeeklyMetrics(
            assignments = tasks,
            scopeThisWeekOnly = true,
            calendar = testCal
        )

        assertEquals("Total tasks should match", 4, metrics.totalCount)
        assertEquals("Completed tasks should match", 2, metrics.completedCount)
        assertEquals("Pending tasks should match", 2, metrics.pendingCount)
        assertEquals("Completion percentage should be 50%", 50, metrics.completionPercentage)
        assertEquals("High priority pending tasks should be 1", 1, metrics.highPriorityPending)

        // Verify day distribution
        val tuesday = metrics.days.find { it.dayLabel == "Tue" }
        val wednesday = metrics.days.find { it.dayLabel == "Wed" }
        val thursday = metrics.days.find { it.dayLabel == "Thu" }
        val friday = metrics.days.find { it.dayLabel == "Fri" }

        assertTrue(tuesday != null)
        assertEquals("Tuesday completed count", 1, tuesday!!.completedCount)
        assertEquals("Tuesday pending count", 0, tuesday.pendingCount)

        assertTrue(wednesday != null)
        assertEquals("Wednesday completed count", 0, wednesday!!.completedCount)
        assertEquals("Wednesday pending count", 1, wednesday.pendingCount)

        assertTrue(thursday != null)
        assertEquals("Thursday completed count", 1, thursday!!.completedCount)
        assertEquals("Thursday pending count", 0, thursday.pendingCount)

        assertTrue(friday != null)
        assertEquals("Friday completed count", 0, friday!!.completedCount)
        assertEquals("Friday pending count", 1, friday.pendingCount)
    }

    @Test
    fun testAllCompletedPercentage() {
        val tasks = listOf(
            Assignment(
                id = 1,
                title = "Task 1",
                courseCode = "MTH1101",
                dueDate = "Monday, 10:00",
                priority = "High",
                notes = "",
                completed = true
            ),
            Assignment(
                id = 2,
                title = "Task 2",
                courseCode = "MTH1101",
                dueDate = "Monday, 14:00",
                priority = "Low",
                notes = "",
                completed = true
            )
        )

        val metrics = TaskMetricsCalculator.calculateWeeklyMetrics(tasks)
        assertEquals(2, metrics.totalCount)
        assertEquals(2, metrics.completedCount)
        assertEquals(0, metrics.pendingCount)
        assertEquals(100, metrics.completionPercentage)
    }
}
