package com.lectures.core.alarm

import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

object TaskDateTimeParser {

    private val TIME_REGEX = Pattern.compile("(?<![0-9])([01]?[0-9]|2[0-3]):([0-5][0-9])(?![0-9])")
    private val DATE_PATTERNS = listOf(
        "EEE, MMM d, yyyy",
        "EEE, MMM d yyyy",
        "EEE, MMM d, yyyy HH:mm",
        "EEE, MMM d yyyy HH:mm",
        "MMM d, yyyy",
        "yyyy-MM-dd",
        "dd/MM/yyyy",
        "yyyy-MM-dd HH:mm",
        "dd/MM/yyyy HH:mm",
        "MMM d, yyyy HH:mm",
        "MMM d HH:mm",
        "EEE, MMM d HH:mm",
        "MMM d"
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
     * Safely attempts to parse [dateStr] using a specified pattern across multiple locales.
     * SimpleDateFormat is not thread-safe, so fresh instances are created per parse.
     */
    private fun tryParsePattern(dateStr: String, pattern: String): Date? {
        val locales = listOf(Locale.US, Locale.getDefault()).distinct()
        for (locale in locales) {
            try {
                val sdf = SimpleDateFormat(pattern, locale).apply { isLenient = false }
                val position = ParsePosition(0)
                val parsed = sdf.parse(dateStr, position)
                if (parsed != null && position.index == dateStr.length) return parsed
            } catch (_: Exception) {}
        }
        return null
    }

    /**
     * Extracts an explicit time pattern (HH:mm) from a due date string if present.
     */
    fun extractTime(dueDateStr: String?): String? {
        if (dueDateStr.isNullOrBlank()) return null
        val matcher = TIME_REGEX.matcher(dueDateStr)
        return if (matcher.find()) matcher.group(0) else null
    }

    /**
     * Calculates the epoch millisecond when the task is due.
     */
    fun calculateDueMillis(dueDateStr: String): Long? {
        val trimmed = dueDateStr.trim().replace("•", " ").replace(Regex("\\s+"), " ")
        if (trimmed.isBlank()) return null

        val now = Calendar.getInstance()

        // 1. Check if it matches any standard date format directly
        for (pattern in DATE_PATTERNS) {
            val parsed = tryParsePattern(trimmed, pattern)
            if (parsed != null) {
                val cal = Calendar.getInstance().apply { time = parsed }
                // Only yearless formats inherit the current year; explicit past years stay past.
                if (!pattern.contains("y")) {
                    cal.set(Calendar.YEAR, now.get(Calendar.YEAR))
                }
                if (!pattern.contains("HH") && !pattern.contains("hh")) {
                    // Date only: set to 23:59:59 (end of day) so the task remains active and valid all day long
                    cal.set(Calendar.HOUR_OF_DAY, 23)
                    cal.set(Calendar.MINUTE, 59)
                    cal.set(Calendar.SECOND, 59)
                    cal.set(Calendar.MILLISECOND, 999)
                }
                return cal.timeInMillis
            }
        }

        // An invalid explicit calendar date must not become an unrelated relative reminder.
        if (Regex("^\\d{4}-\\d{1,2}-\\d{1,2}|^\\d{1,2}/\\d{1,2}/\\d{4}").containsMatchIn(trimmed)) return null

        val relativeDate = Regex(
            "^(?:today|tomorrow|sun(?:day)?|mon(?:day)?|tue(?:sday)?|wed(?:nesday)?|thu(?:rsday)?|fri(?:day)?|sat(?:urday)?)(?:[ ,\\-]+(?:[01]?[0-9]|2[0-3]):[0-5][0-9])?$",
            RegexOption.IGNORE_CASE
        )
        if (!relativeDate.matches(trimmed) && !TIME_REGEX.matcher(trimmed).matches()) return null

        // 2. Extract Time component (HH:mm)
        val timeMatcher = TIME_REGEX.matcher(trimmed)
        val hasExplicitTime = timeMatcher.find()
        var hour = if (hasExplicitTime) timeMatcher.group(1)?.toIntOrNull() ?: 17 else 23
        var minute = if (hasExplicitTime) timeMatcher.group(2)?.toIntOrNull() ?: 0 else 59
        if (trimmed.contains(':') && !hasExplicitTime) return null

        val lower = trimmed.lowercase(Locale.ROOT)
        val targetCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, if (hasExplicitTime) 0 else 59)
            set(Calendar.MILLISECOND, 0)
        }

        // 3. Match relative keywords
        when {
            Regex("\\btoday\\b").containsMatchIn(lower) -> {
                // targetCal is already today at hour:minute
            }
            Regex("\\btomorrow\\b").containsMatchIn(lower) -> {
                targetCal.add(Calendar.DAY_OF_YEAR, 1)
            }
            else -> {
                // Check for day of week (e.g. "Friday • 17:00")
                var foundDay = false
                for ((dayName, dayConstant) in DAYS_OF_WEEK) {
                    if (Regex("\\b${dayName.take(3)}(?:${dayName.drop(3)})?\\b").containsMatchIn(lower)) {
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
                    if (!hasExplicitTime || timeMatcher.group(0) != trimmed) return null
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
        if (reminderMinutes == null || reminderMinutes < 0) return null
        val dueMillis = calculateDueMillis(dueDateStr) ?: return null
        val now = System.currentTimeMillis()
        if (dueMillis <= now) return null // Task is completely in the past

        val trimmed = dueDateStr.trim().replace("•", " ").replace(Regex("\\s+"), " ")
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
        val datePart = trimmed.substringBefore("•").trim()
        val cleanDay = datePart.replace(Regex("[, \\t-]+(?:[01]?[0-9]|2[0-3]):[0-5][0-9]$"), "").trim()
        return cleanDay.ifBlank { "Tomorrow" }
    }
}
