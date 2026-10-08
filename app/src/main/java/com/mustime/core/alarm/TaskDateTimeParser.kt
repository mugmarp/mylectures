package com.mustime.core.alarm

import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

object TaskDateTimeParser {

    private val TIME_REGEX = Pattern.compile("(\\b[01]?[0-9]|2[0-3]):([0-5][0-9])")
    private val DATE_FORMATS = listOf(
        SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()),
        SimpleDateFormat("EEE, MMM d yyyy", Locale.getDefault()),
        SimpleDateFormat("MMM d, yyyy", Locale.getDefault()),
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()),
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()),
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()),
        SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()),
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
                    val pattern = format.toPattern()
                    if (!pattern.contains("HH") && !pattern.contains("hh")) {
                        // Date only: set to 23:59:59 (end of day) so the task remains active and valid all day long
                        cal.set(Calendar.HOUR_OF_DAY, 23)
                        cal.set(Calendar.MINUTE, 59)
                        cal.set(Calendar.SECOND, 59)
                        cal.set(Calendar.MILLISECOND, 999)
                    }
                    return cal.timeInMillis
                }
            } catch (_: Exception) {}
        }

        // 2. Extract Time component (HH:mm)
        val timeMatcher = TIME_REGEX.matcher(trimmed)
        val hasExplicitTime = timeMatcher.find()
        var hour = if (hasExplicitTime) timeMatcher.group(1)?.toIntOrNull() ?: 17 else 23
        var minute = if (hasExplicitTime) timeMatcher.group(2)?.toIntOrNull() ?: 0 else 59

        val lower = trimmed.lowercase(Locale.ROOT)
        val targetCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, if (hasExplicitTime) 0 else 59)
            set(Calendar.MILLISECOND, 0)
        }

        // 3. Match relative keywords
        when {
            lower.contains("today") -> {
                // targetCal is already today at hour:minute
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
        val now = System.currentTimeMillis()
        if (dueMillis <= now) return null // Task is completely in the past

        val trimmed = dueDateStr.trim()
        val timeMatcher = TIME_REGEX.matcher(trimmed)
        val hasExplicitTime = timeMatcher.find()

        val triggerMillis = if (!hasExplicitTime) {
            // For date-only academic tasks (due end of day), ground alarms at student-friendly academic hours (09:00 AM)
            val cal = Calendar.getInstance().apply {
                timeInMillis = dueMillis
                set(Calendar.HOUR_OF_DAY, 9)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            when {
                reminderMinutes == null -> return null
                reminderMinutes == 0 -> cal.timeInMillis // 09:00 AM on due day
                reminderMinutes >= 1440 -> {
                    val daysBefore = reminderMinutes / 1440
                    cal.add(Calendar.DAY_OF_YEAR, -daysBefore)
                    cal.timeInMillis
                }
                reminderMinutes >= 60 -> {
                    // Lead hours before end of academic day (17:00)
                    cal.set(Calendar.HOUR_OF_DAY, 17)
                    cal.timeInMillis - (reminderMinutes * 60 * 1000L)
                }
                else -> {
                    // Minutes before 09:00 AM
                    cal.timeInMillis - (reminderMinutes * 60 * 1000L)
                }
            }
        } else {
            val offsetMillis = ((reminderMinutes ?: 30).coerceAtLeast(0)) * 60 * 1000L
            dueMillis - offsetMillis
        }

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
        if (reminderMinutes == null) return "No alarm reminder set"
        val trigger = calculateTriggerTimeMillis(dueDateStr, reminderMinutes)
            ?: return "Reminder disabled or date passed"
        val sdf = SimpleDateFormat("EEE, MMM d • HH:mm", Locale.getDefault())
        val leadText = when {
            reminderMinutes == 0 -> "on due date"
            reminderMinutes >= 1440 -> "${reminderMinutes / 1440}d before"
            reminderMinutes >= 60 -> "${reminderMinutes / 60}h before"
            else -> "${reminderMinutes}m before"
        }
        return "Alarm scheduled: ${sdf.format(Date(trigger))} ($leadText)"
    }

    /**
     * Cleans legacy timestamps (e.g. "Friday, 17:00" -> "Friday", "Oct 12 • 23:59" -> "Oct 12")
     * so academic tasks are purely day-based without accidental legacy time attachments.
     */
    fun normalizeDueDate(rawDueDate: String?): String {
        if (rawDueDate.isNullOrBlank()) return "Tomorrow"
        val trimmed = rawDueDate.trim()
        val parts = trimmed.split("•", "-")
        val datePart = parts.firstOrNull()?.trim() ?: trimmed
        val cleanDay = datePart.replace(Regex(",\\s*(\\b[01]?[0-9]|2[0-3]):[0-5][0-9]"), "").trim()
        return cleanDay.ifBlank { "Tomorrow" }
    }
}
