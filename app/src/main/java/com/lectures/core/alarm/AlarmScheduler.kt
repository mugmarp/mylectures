package com.lectures.core.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.lectures.features.timetable.domain.TimetableEntry
import java.util.Calendar

class AlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
    private val TAG = "AlarmScheduler"

    private fun parseStartTime(rawTime: String): Pair<Int, Int>? {
        val clean = rawTime.trim().uppercase()
        val isPm = clean.endsWith("PM")
        val isAm = clean.endsWith("AM")
        val stripped = clean.removeSuffix("PM").removeSuffix("AM").trim()
        val parts = stripped.split(":")
        if (parts.size != 2) return null
        var hour = parts[0].trim().toIntOrNull() ?: return null
        val minute = parts[1].trim().toIntOrNull() ?: return null
        if (isPm && hour < 12) hour += 12
        if (isAm && hour == 12) hour = 0
        if (hour !in 0..23 || minute !in 0..59) return null
        return Pair(hour, minute)
    }

    fun scheduleClassAlarm(entry: TimetableEntry, minutesBefore: Int, isAlarm: Boolean = true): Boolean {
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager service not available")
            return false
        }

        val targetDow = when (entry.dayOfWeek.trim().lowercase().take(3)) {
            "mon" -> Calendar.MONDAY
            "tue" -> Calendar.TUESDAY
            "wed" -> Calendar.WEDNESDAY
            "thu" -> Calendar.THURSDAY
            "fri" -> Calendar.FRIDAY
            "sat" -> Calendar.SATURDAY
            "sun" -> Calendar.SUNDAY
            else -> null
        }
        if (targetDow == null) {
            Log.e(TAG, "Cannot schedule alarm: unrecognized dayOfWeek '${entry.dayOfWeek}' for ${entry.courseCode}")
            return false
        }

        val time = parseStartTime(entry.startTime)
        if (time == null) {
            Log.e(TAG, "Cannot schedule alarm: invalid startTime '${entry.startTime}' for ${entry.courseCode}")
            return false
        }
        val (hour, minute) = time

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MINUTE, -minutesBefore)
        }

        val currentDow = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        var daysDiff = (targetDow - currentDow + 7) % 7
        if (daysDiff == 0 && cal.timeInMillis <= System.currentTimeMillis() + 60_000L) {
            daysDiff = 7
        }
        cal.add(Calendar.DAY_OF_YEAR, daysDiff)

        val triggerMillis = cal.timeInMillis
        val intent = Intent(context, ClassAlarmReceiver::class.java).apply {
            putExtra("NATURAL_KEY", entry.naturalKey)
            putExtra("DAY_OF_WEEK", entry.dayOfWeek)
            putExtra("COURSE_CODE", entry.courseCode)
            putExtra("COURSE_TITLE", entry.courseTitle)
            putExtra("ROOM", entry.room ?: "TBD")
            putExtra("START_TIME", entry.startTime)
            putExtra("LECTURER", entry.lecturer ?: "")
            putExtra("MINUTES_BEFORE", minutesBefore)
            putExtra("IS_ALARM", isAlarm)
            putExtra("IS_RECURRING", true)
            putExtra("IS_TEST", false)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            entry.naturalKey.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerMillis,
                        pendingIntent
                    )
                    Log.d(TAG, "Scheduled exact alarm for ${entry.courseCode} at ${cal.time}")
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerMillis,
                        pendingIntent
                    )
                    Log.d(TAG, "Scheduled inexact alarm for ${entry.courseCode} at ${cal.time}")
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
                Log.d(TAG, "Scheduled M exact alarm for ${entry.courseCode} at ${cal.time}")
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            }
            return true
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while scheduling class alarm: ${e.message}")
            return false
        }
    }

    /**
     * Schedules a class alarm directly from a saved LectureNote metadata.
     */
    fun scheduleClassAlarmFromNote(note: com.lectures.features.timetable.domain.LectureNote, minutesBefore: Int): Boolean {
        val parts = note.naturalKey.split("|")
        val code = parts.getOrNull(0)?.trim() ?: (note.title?.take(10) ?: "Lecture")
        val title = note.title ?: parts.getOrNull(1)?.trim() ?: code
        val day = parts.getOrNull(2)?.trim() ?: "Monday"
        val time = parts.getOrNull(3)?.trim() ?: "09:00"
        val room = parts.getOrNull(4)?.trim() ?: (note.attachedClass ?: "Lecture Room")

        val syntheticEntry = TimetableEntry(
            natural_key = note.naturalKey,
            program_group = "",
            day = day,
            time_slot = time,
            start_time = time,
            end_time = null,
            course_code = code,
            course_title = title,
            session_type = null,
            lecturer = null,
            room = room
        )
        return scheduleClassAlarm(syntheticEntry, minutesBefore)
    }

    fun triggerTestAlarm(entry: TimetableEntry, delaySeconds: Int = 3): Boolean {
        if (alarmManager == null) return false
        val triggerMillis = System.currentTimeMillis() + (delaySeconds * 1000L)
        val intent = Intent(context, ClassAlarmReceiver::class.java).apply {
            putExtra("NATURAL_KEY", entry.naturalKey)
            putExtra("DAY_OF_WEEK", entry.dayOfWeek)
            putExtra("COURSE_CODE", entry.courseCode)
            putExtra("COURSE_TITLE", entry.courseTitle)
            putExtra("ROOM", entry.room ?: "TBD")
            putExtra("START_TIME", entry.startTime)
            putExtra("LECTURER", entry.lecturer ?: "")
            putExtra("MINUTES_BEFORE", 0)
            putExtra("IS_ALARM", true)
            putExtra("IS_RECURRING", false)
            putExtra("IS_TEST", true)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            entry.naturalKey.hashCode() + 1000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerMillis,
                        pendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            }
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed test alarm: ${e.message}")
            return false
        }
    }

    fun cancelClassAlarm(naturalKey: String) {
        if (alarmManager == null) return
        val intent = Intent(context, ClassAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            naturalKey.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
        Log.d(TAG, "Cancelled class alarm for $naturalKey")
    }

    /**
     * Automatically arms recurring class reminders/alarms for all lectures
     * in the student's enrolled academic programme.
     */
    fun scheduleAllEnrolledClassAlarms(classes: List<TimetableEntry>, leadMinutes: Int, isAlarm: Boolean = true): Int {
        if (alarmManager == null || classes.isEmpty()) return 0
        val uniqueClasses = classes.distinctBy {
            if (it.naturalKey.isNotBlank()) it.naturalKey else "${it.courseCode}|${it.dayOfWeek}|${it.startTime}"
        }
        var scheduledCount = 0
        for (entry in uniqueClasses) {
            val ok = scheduleClassAlarm(entry, leadMinutes, isAlarm)
            if (ok) scheduledCount++
        }
        Log.d(TAG, "Scheduled $scheduledCount enrolled class alarms (lead = ${leadMinutes}m) out of ${uniqueClasses.size} unique lectures")
        return scheduledCount
    }

    /**
     * Cancels alarms for all given timetable entries (e.g. on programme switch or quiet mode).
     */
    fun cancelAllEnrolledClassAlarms(classes: List<TimetableEntry>) {
        if (alarmManager == null || classes.isEmpty()) return
        classes.distinctBy {
            if (it.naturalKey.isNotBlank()) it.naturalKey else "${it.courseCode}|${it.dayOfWeek}|${it.startTime}"
        }.forEach {
            cancelClassAlarm(it.naturalKey)
        }
        Log.d(TAG, "Cancelled alarms for ${classes.size} enrolled class entries")
    }

    fun scheduleSnoozeAlarm(
        courseCode: String,
        courseTitle: String,
        room: String,
        startTime: String,
        snoozeMinutes: Int = 10,
        minutesBefore: Int = 30,
        lecturer: String = "",
        naturalKey: String? = null,
        dayOfWeek: String? = null
    ) {
        if (alarmManager == null) return
        val triggerMillis = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)
        val intent = Intent(context, ClassAlarmReceiver::class.java).apply {
            putExtra("COURSE_CODE", courseCode)
            putExtra("COURSE_TITLE", courseTitle)
            putExtra("ROOM", room)
            putExtra("START_TIME", startTime)
            putExtra("MINUTES_BEFORE", minutesBefore)
            putExtra("LECTURER", lecturer)
            naturalKey?.let { putExtra("NATURAL_KEY", it) }
            dayOfWeek?.let { putExtra("DAY_OF_WEEK", it) }
            putExtra("IS_ALARM", true)
            putExtra("IS_RECURRING", false)
            putExtra("IS_TEST", false)
            putExtra("IS_SNOOZED", true)
        }
        val requestCode = ("snooze_" + (naturalKey ?: "$courseCode$room$startTime$dayOfWeek")).hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                    Log.d(TAG, "Scheduled EXACT snooze alarm for $courseCode in $snoozeMinutes minutes")
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                    Log.d(TAG, "Scheduled INEXACT snooze alarm for $courseCode in $snoozeMinutes minutes (exact permission missing)")
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException scheduling exact snooze alarm, falling back to setAndAllowWhileIdle: ${e.message}")
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                }
            } catch (fallbackEx: Exception) {
                Log.e(TAG, "Fallback snooze alarm failed: ${fallbackEx.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule snooze alarm: ${e.message}")
        }
    }
}
