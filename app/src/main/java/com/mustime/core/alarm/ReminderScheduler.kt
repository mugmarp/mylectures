package com.mustime.core.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.Calendar

class ReminderScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
    private val TAG = "ReminderScheduler"

    companion object {
        const val DAILY_DIGEST_REQUEST_CODE = 88001
        const val DEFAULT_DIGEST_HOUR = 7 // 07:30 AM
        const val DEFAULT_DIGEST_MINUTE = 30
    }

    /**
     * Schedules a daily morning briefing of today's classes and venues.
     */
    fun scheduleDailyReminder(hour: Int = DEFAULT_DIGEST_HOUR, minute: Int = DEFAULT_DIGEST_MINUTE): Boolean {
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager not available")
            return false
        }

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If today's briefing time has already passed, schedule for tomorrow morning
        if (cal.timeInMillis <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        val triggerMillis = cal.timeInMillis
        val intent = Intent(context, DailyReminderReceiver::class.java).apply {
            action = DailyReminderReceiver.ACTION_TRIGGER_DAILY_DIGEST
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            DAILY_DIGEST_REQUEST_CODE,
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
            Log.d(TAG, "Scheduled daily class digest for ${cal.time}")
            return true
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException scheduling daily digest: ${e.message}")
            return false
        }
    }

    fun triggerTestDailyDigest(): Boolean {
        val intent = Intent(context, DailyReminderReceiver::class.java).apply {
            action = DailyReminderReceiver.ACTION_TRIGGER_DAILY_DIGEST
            putExtra("IS_TEST", true)
        }
        context.sendBroadcast(intent)
        return true
    }

    fun cancelDailyReminder() {
        if (alarmManager == null) return
        val intent = Intent(context, DailyReminderReceiver::class.java).apply {
            action = DailyReminderReceiver.ACTION_TRIGGER_DAILY_DIGEST
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            DAILY_DIGEST_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Cancelled daily reminder alarm")
        }
    }
}
