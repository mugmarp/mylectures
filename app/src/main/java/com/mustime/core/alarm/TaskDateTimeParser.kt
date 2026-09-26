package com.mustime.core.alarm

import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

object TaskDateTimeParser {

    private val TIME_REGEX = Pattern.compile("(\\b[01]?[0-9]|2[0-3]):([0-5][0-9])")
    private val DATE_FORMATS = listOf(
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()),
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()),
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()),
        SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()),
        SimpleDateFormat("MMM d, yyyy", Locale.getDefault()),
        SimpleDateFormat("MMM d HH:mm", Locale.getDefault()),
        SimpleDateFormat("EEE, MMM d HH:mm", Locale.getDefault())
    )

    private val DAYS_OF_WEEK = mapOf(
        "sunday" to Calendar.SUNDAY,
        "monday" to Calendar.MONDAY,
        "tuesday" to Calendar.TUESDAY,
        "wednesday" to Calendar.WEDNESDAY,
        "thursday" to Calendar.THURSDAY,
        "friday" to Calendar.FRIDAY,
        "saturday" to Calendar.SATURDAY
    )

    /**
     * Calculates the epoch millisecond when the task is due.
     */
    fun calculateDueMillis(dueDateStr: String): Long? {
        val trimmed = dueDateStr.trim()
        if (trimmed.isBlank()) return null

        val now = Calendar.getInstance()

        // 1. Check if it matches any standard date format directly
        for (format in DATE_FORMATS) {
            try {
                format.isLenient = false
                val parsed = format.parse(trimmed)
                if (parsed != null) {
                    val cal = Calendar.getInstance().apply { time = parsed }
                    // If no year was in format, set to current year
                    if (cal.get(Calendar.YEAR) < 2000) {
                        cal.set(Calendar.YEAR, now.get(Calendar.YEAR))
                    }
                    return cal.timeInMillis
                }
            } catch (_: Exception) {}
        }

        // 2. Extract Time component (HH:mm)
        val timeMatcher = TIME_REGEX.matcher(trimmed)
        var hour = 17 // Default 5:00 PM if no time specified
        var minute = 0
        if (timeMatcher.find()) {
            hour = timeMatcher.group(1)?.toIntOrNull() ?: 17
            minute = timeMatcher.group(2)?.toIntOrNull() ?: 0
        }

        val lower = trimmed.lowercase(Locale.ROOT)
        val targetCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // 3. Match relative keywords
        when {
            lower.contains("today") -> {
                // targetCal is already today at hour:minute
                // If the time already passed today, don't move to yesterday
            }
            lower.contains("tomorrow") -> {
                targetCal.add(Calendar.DAY_OF_YEAR, 1)
            }
            else -> {
                // Check for day of week (e.g. "Friday • 17:00")
                var foundDay = false
                for ((dayName, dayConstant) in DAYS_OF_WEEK) {
                    if (lower.contains(dayName)) {
                        val currentDay = now.get(Calendar.DAY_OF_WEEK)
                        var daysToAdd = (dayConstant - currentDay + 7) % 7
                        if (daysToAdd == 0 && targetCal.before(now)) {
                            daysToAdd = 7
                        }
                        targetCal.add(Calendar.DAY_OF_YEAR, daysToAdd)
                        foundDay = true
                        break
                    }
                }

                if (!foundDay) {
                    // Fallback: If no day recognized but time was given, assume tomorrow if today passed
                    if (targetCal.before(now)) {
                        targetCal.add(Calendar.DAY_OF_YEAR, 1)
                    }
                }
            }
        }

        return targetCal.timeInMillis
    }

    /**
     * Calculates the exact trigger timestamp in epoch millis for AlarmManager,
     * taking into account the task due time and lead reminder minutes.
     */
    fun calculateTriggerTimeMillis(dueDateStr: String, reminderMinutes: Int?): Long? {
        val dueMillis = calculateDueMillis(dueDateStr) ?: return null
        val offsetMillis = ((reminderMinutes ?: 30).coerceAtLeast(0)) * 60 * 1000L
        val triggerMillis = dueMillis - offsetMillis

        val now = System.currentTimeMillis()
        // If trigger time is in the past, but the task is still due in the future:
        // Schedule in 5 seconds so the user is immediately reminded that deadline is approaching!
        return if (triggerMillis <= now) {
            if (dueMillis > now) {
                now + 5_000L
            } else {
                null // Task is completely in the past
            }
        } else {
            triggerMillis
        }
    }

    /**
     * Formats an epoch millisecond into a human-readable reminder schedule time.
     */
    fun formatSchedulePreview(dueDateStr: String, reminderMinutes: Int?): String {
        val trigger = calculateTriggerTimeMillis(dueDateStr, reminderMinutes)
            ?: return "No active reminder"
        val sdf = SimpleDateFormat("EEE, MMM d • HH:mm", Locale.getDefault())
        val leadText = if (reminderMinutes == null || reminderMinutes == 0) {
            "at due time"
        } else {
            "${reminderMinutes}m before"
        }
        return "Alarm scheduled: ${sdf.format(Date(trigger))} ($leadText)"
    }
}
