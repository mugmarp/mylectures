package com.lectures.core.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import com.lectures.features.timetable.domain.TimetableEntry
import java.util.Calendar

class AlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
    private val TAG = "AlarmScheduler"

    companion object {
        const val ACTION_TRIGGER_CLASS_ALARM = "com.lectures.ACTION_TRIGGER_CLASS_ALARM"
        const val ACTION_TRIGGER_TEST_CLASS_ALARM = "com.lectures.ACTION_TRIGGER_TEST_CLASS_ALARM"
        const val ACTION_TRIGGER_SNOOZE_CLASS_ALARM = "com.lectures.ACTION_TRIGGER_SNOOZE_CLASS_ALARM"

        fun classAlarmUri(naturalKey: String): Uri =
            Uri.parse("lectures://alarm/class/${Uri.encode(naturalKey)}")

        fun testClassAlarmUri(naturalKey: String): Uri =
            Uri.parse("lectures://alarm/class_test/${Uri.encode(naturalKey)}")

        fun snoozeClassAlarmUri(key: String): Uri =
            Uri.parse("lectures://alarm/class_snooze/${Uri.encode(key)}")

        /**
         * Computes the next future trigger timestamp in epoch millis for a weekly class occurrence.
         * Advances to the target day-of-week FIRST before subtracting [minutesBefore], ensuring
         * early-morning classes whose lead time crosses midnight (e.g. Monday 00:15 - 30m = Sunday 23:45)
         * advance strictly into the future instead of firing in the past.
         */
        fun calculateNextTriggerMillis(
            targetDow: Int,
            hour: Int,
            minute: Int,
            minutesBefore: Int,
            nowMillis: Long = System.currentTimeMillis()
        ): Long {
            val nowCal = Calendar.getInstance().apply { timeInMillis = nowMillis }
            val currentDow = nowCal.get(Calendar.DAY_OF_WEEK)
            val daysDiff = (targetDow - currentDow + 7) % 7

            val triggerCal = Calendar.getInstance().apply {
                timeInMillis = nowMillis
                add(Calendar.DAY_OF_YEAR, daysDiff)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                add(Calendar.MINUTE, -minutesBefore.coerceAtLeast(0))
            }

            while (triggerCal.timeInMillis <= nowMillis) {
                triggerCal.add(Calendar.DAY_OF_YEAR, 7)
            }
            return triggerCal.timeInMillis
        }
    }

    private fun parseStartTime(rawTime: String): Pair<Int, Int>? {
        val clean = rawTime.trim().uppercase(java.util.Locale.ROOT)
        val isPm = clean.endsWith("PM")
        val isAm = clean.endsWith("AM")
        val stripped = clean.removeSuffix("PM").removeSuffix("AM").trim()
        val parts = stripped.split(":")
        if (parts.size != 2) return null
        var hour = parts[0].trim().toIntOrNull() ?: return null
        val minute = parts[1].trim().toIntOrNull() ?: return null
        if ((isPm || isAm) && hour !in 1..12) return null
        if (isPm && hour < 12) hour += 12
        if (isAm && hour == 12) hour = 0
        if (hour !in 0..23 || minute !in 0..59) return null
        return Pair(hour, minute)
    }

    fun scheduleClassAlarm(entry: TimetableEntry, minutesBefore: Int, isAlarm: Boolean = true): Boolean {
        if (!com.lectures.features.timetable.data.ETagStore(context).isNotificationsEnabled()) {
            cancelClassAlarm(entry.naturalKey)
            return false
        }
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager service not available")
            return false
        }

        val targetDow = when (entry.dayOfWeek.trim().lowercase(java.util.Locale.ROOT).take(3)) {
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
            cancelClassAlarm(entry.naturalKey)
            Log.e(TAG, "Cannot schedule alarm: unrecognized dayOfWeek '${entry.dayOfWeek}' for ${entry.courseCode}")
            return false
        }

        val time = parseStartTime(entry.startTime)
        if (time == null) {
            cancelClassAlarm(entry.naturalKey)
            Log.e(TAG, "Cannot schedule alarm: invalid startTime '${entry.startTime}' for ${entry.courseCode}")
            return false
        }
        val (hour, minute) = time

        val triggerMillis = calculateNextTriggerMillis(
            targetDow = targetDow,
            hour = hour,
            minute = minute,
            minutesBefore = minutesBefore
        )
        val intent = Intent(context, ClassAlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_CLASS_ALARM
            data = classAlarmUri(entry.naturalKey)
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

        val scheduled = scheduleRtcWakeupWithFallback(
            triggerMillis = triggerMillis,
            pendingIntent = pendingIntent,
            label = "class alarm for ${entry.courseCode}"
        )
        if (scheduled) cancelAlarmToken(context, alarmManager, entry.naturalKey.hashCode(), Intent(context, ClassAlarmReceiver::class.java))
        return scheduled
    }

    private fun scheduleRtcWakeupWithFallback(
        triggerMillis: Long,
        pendingIntent: PendingIntent,
        label: String
    ): Boolean {
        val manager = alarmManager ?: return false
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (manager.canScheduleExactAlarms()) {
                    manager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerMillis,
                        pendingIntent
                    )
                    Log.d(TAG, "Scheduled exact $label at $triggerMillis")
                } else {
                    manager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerMillis,
                        pendingIntent
                    )
                    Log.d(TAG, "Scheduled inexact $label at $triggerMillis")
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                manager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
                Log.d(TAG, "Scheduled M exact $label at $triggerMillis")
            } else {
                manager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            }
            true
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException scheduling exact $label, falling back to inexact: ${e.message}")
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                } else {
                    manager.set(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                }
                true
            } catch (fallbackEx: Exception) {
                Log.e(TAG, "Fallback inexact scheduling failed for $label: ${fallbackEx.message}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed scheduling $label: ${e.message}")
            false
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
            action = ACTION_TRIGGER_TEST_CLASS_ALARM
            data = testClassAlarmUri(entry.naturalKey)
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

        return scheduleRtcWakeupWithFallback(
            triggerMillis = triggerMillis,
            pendingIntent = pendingIntent,
            label = "test alarm for ${entry.courseCode}"
        )
    }

    fun cancelClassAlarm(naturalKey: String) {
        val manager = alarmManager ?: return
        cancelAlarmToken(context, manager, naturalKey.hashCode(), Intent(context, ClassAlarmReceiver::class.java))
        cancelAlarmToken(context, manager, naturalKey.hashCode(), Intent(context, ClassAlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_CLASS_ALARM
            data = classAlarmUri(naturalKey)
        })
        cancelAlarmToken(context, manager, ("snooze_$naturalKey").hashCode(), Intent(context, ClassAlarmReceiver::class.java))
        cancelAlarmToken(context, manager, ("snooze_$naturalKey").hashCode(), Intent(context, ClassAlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_SNOOZE_CLASS_ALARM
            data = snoozeClassAlarmUri(naturalKey)
        })
    }

    /**
     * Automatically arms recurring class reminders/alarms for all lectures
     * in the student's enrolled academic programme.
     */
    fun scheduleAllEnrolledClassAlarms(
        classes: List<TimetableEntry>,
        leadMinutes: Int,
        isAlarm: Boolean = true,
        customAlarms: List<com.lectures.features.timetable.domain.LectureNote> = emptyList()
    ): Int {
        if (alarmManager == null || classes.isEmpty()) return 0
        val uniqueClasses = classes.distinctBy {
            if (it.naturalKey.isNotBlank()) it.naturalKey else "${it.courseCode}|${it.dayOfWeek}|${it.startTime}"
        }
        val overrides = customAlarms.filter { !it.isDeleted && it.alarmMinutes != null }
            .associate { it.naturalKey to it.alarmMinutes!! }
        var scheduledCount = 0
        for (entry in uniqueClasses) {
            val ok = scheduleClassAlarm(entry, overrides[entry.naturalKey] ?: leadMinutes, isAlarm)
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
        if (alarmManager == null || !com.lectures.features.timetable.data.ETagStore(context).isNotificationsEnabled()) return
        val triggerMillis = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)
        val snoozeKey = naturalKey ?: "$courseCode|$room|$startTime|$dayOfWeek"
        val intent = Intent(context, ClassAlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_SNOOZE_CLASS_ALARM
            data = snoozeClassAlarmUri(snoozeKey)
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
        val requestCode = ("snooze_$snoozeKey").hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        scheduleRtcWakeupWithFallback(
            triggerMillis = triggerMillis,
            pendingIntent = pendingIntent,
            label = "snooze alarm for $courseCode"
        )
    }
}
