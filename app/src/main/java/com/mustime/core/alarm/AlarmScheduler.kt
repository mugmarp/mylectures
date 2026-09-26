package com.mustime.core.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.mustime.features.timetable.domain.TimetableEntry
import java.util.Calendar

class AlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
    private val TAG = "AlarmScheduler"

    fun scheduleClassAlarm(entry: TimetableEntry, minutesBefore: Int): Boolean {
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
            else -> Calendar.MONDAY
        }

        val timeParts = entry.startTime.split(":")
        val hour = timeParts.getOrNull(0)?.trim()?.toIntOrNull() ?: 8
        val minute = timeParts.getOrNull(1)?.trim()?.toIntOrNull() ?: 0

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MINUTE, -minutesBefore)
        }

        val currentDow = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        var daysDiff = (targetDow - currentDow + 7) % 7
        if (daysDiff == 0 && cal.timeInMillis <= System.currentTimeMillis()) {
            daysDiff = 7
        }
        cal.add(Calendar.DAY_OF_YEAR, daysDiff)

        val triggerMillis = cal.timeInMillis
        val intent = Intent(context, ClassAlarmReceiver::class.java).apply {
            putExtra("COURSE_CODE", entry.courseCode)
            putExtra("COURSE_TITLE", entry.courseTitle)
            putExtra("ROOM", entry.room ?: "TBD")
            putExtra("START_TIME", entry.startTime)
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

    fun triggerTestAlarm(entry: TimetableEntry, delaySeconds: Int = 3): Boolean {
        if (alarmManager == null) return false
        val triggerMillis = System.currentTimeMillis() + (delaySeconds * 1000L)
        val intent = Intent(context, ClassAlarmReceiver::class.java).apply {
            putExtra("COURSE_CODE", entry.courseCode)
            putExtra("COURSE_TITLE", entry.courseTitle)
            putExtra("ROOM", entry.room ?: "TBD")
            putExtra("START_TIME", entry.startTime)
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
}
