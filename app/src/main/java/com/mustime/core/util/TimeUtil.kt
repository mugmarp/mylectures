package com.mustime.core.util

import java.util.Calendar

data class SessionProgress(
    val isOngoing: Boolean,
    val progress: Float, // 0.0f to 1.0f
    val elapsedMinutes: Int,
    val remainingMinutes: Int,
    val totalMinutes: Int
)

object TimeUtil {
    val DAYS = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

    /**
     * MUST 2026/2027 Academic Year Semester I commenced around late August / early September.
     * Calculates current active semester week (e.g. "Week 6 • Semester 1").
     */
    fun currentSemesterWeekLabel(): String {
        val now = Calendar.getInstance()
        val year = now.get(Calendar.YEAR)
        val month = now.get(Calendar.MONTH) // 0-based: Jan = 0, Aug = 7, Sep = 8

        // Semester 1 reference start date: Last Monday of August / First Monday of September
        val semStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (month >= Calendar.AUGUST) {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, Calendar.AUGUST)
                set(Calendar.DAY_OF_MONTH, 24) // Late August start
            } else {
                // Semester 2: Starts mid-January / February
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, Calendar.JANUARY)
                set(Calendar.DAY_OF_MONTH, 19)
            }
        }

        val diffMillis = now.timeInMillis - semStart.timeInMillis
        val diffDays = (diffMillis / (1000 * 60 * 60 * 24)).toInt()
        val weekNum = (diffDays / 7) + 1
        val semesterNumber = if (month >= Calendar.AUGUST || month <= Calendar.JANUARY) 1 else 2

        val clampedWeek = when {
            weekNum in 1..17 -> "Week $weekNum"
            weekNum > 17 -> "Recess / Exams"
            else -> "Week 1"
        }
        return "$clampedWeek • Sem $semesterNumber"
    }

    fun todayName(): String {
        val calendar = Calendar.getInstance()
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "Monday"
            Calendar.TUESDAY -> "Tuesday"
            Calendar.WEDNESDAY -> "Wednesday"
            Calendar.THURSDAY -> "Thursday"
            Calendar.FRIDAY -> "Friday"
            Calendar.SATURDAY -> "Saturday"
            Calendar.SUNDAY -> "Sunday"
            else -> "Monday"
        }
    }

    fun toMinutes(timeString: String): Int {
        if (timeString.isBlank() || !timeString.contains(":")) return 0
        val parts = timeString.split(":")
        if (parts.size != 2) return 0
        val hours = parts[0].trim().toIntOrNull() ?: 0
        val minutes = parts[1].trim().toIntOrNull() ?: 0
        return hours * 60 + minutes
    }

    fun rangesOverlap(start1: Int, end1: Int, start2: Int, end2: Int): Boolean {
        return start1 < end2 && start2 < end1
    }

    fun calculateProgress(startTime: String, endTime: String?, dayOfWeek: String): SessionProgress {
        if (endTime.isNullOrBlank()) {
            return SessionProgress(false, 0f, 0, 0, 0)
        }

        val today = todayName()
        val matchesDay = dayOfWeek.equals(today, ignoreCase = true) ||
                dayOfWeek.startsWith(today.take(3), ignoreCase = true) ||
                today.startsWith(dayOfWeek.take(3), ignoreCase = true)
        if (!matchesDay) {
            return SessionProgress(false, 0f, 0, 0, 0)
        }

        val cal = Calendar.getInstance()
        val now = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val start = toMinutes(startTime)
        val end = toMinutes(endTime)

        if (end <= start) {
            return SessionProgress(false, 0f, 0, 0, 0)
        }

        if (now in start until end) {
            val elapsed = now - start
            val total = end - start
            val remaining = end - now
            val fraction = (elapsed.toFloat() / total.toFloat()).coerceIn(0f, 1f)
            return SessionProgress(
                isOngoing = true,
                progress = fraction,
                elapsedMinutes = elapsed,
                remainingMinutes = remaining,
                totalMinutes = total
            )
        }

        return SessionProgress(false, 0f, 0, 0, 0)
    }
}
