package com.lectures.core.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
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
        private val DAILY_DIGEST_URI = Uri.parse("lectures://alarm/daily_digest")
    }

    /**
     * Schedules a daily morning briefing of today's classes and venues.
     */
    fun scheduleDailyReminder(hour: Int = DEFAULT_DIGEST_HOUR, minute: Int = DEFAULT_DIGEST_MINUTE): Boolean {
        if (!com.lectures.features.timetable.data.ETagStore(context).isNotificationsEnabled()) {
            cancelDailyReminder()
            return false
        }
        val manager = alarmManager
        if (manager == null) {
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
            data = DAILY_DIGEST_URI
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            DAILY_DIGEST_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val scheduled = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (manager.canScheduleExactAlarms()) {
                    manager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerMillis,
                        pendingIntent
                    )
                } else {
                    manager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerMillis,
                        pendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                manager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            } else {
                manager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            }
            Log.d(TAG, "Scheduled daily class digest for ${cal.time}")
            true
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException scheduling daily digest, falling back to inexact: ${e.message}")
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                } else {
                    manager.set(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                }
                true
            } catch (fallbackEx: Exception) {
                Log.e(TAG, "Fallback daily digest scheduling failed: ${fallbackEx.message}")
                false
            }
        }
        if (scheduled) cancelAlarmToken(context, manager, DAILY_DIGEST_REQUEST_CODE, Intent(context, DailyReminderReceiver::class.java).apply {
            action = DailyReminderReceiver.ACTION_TRIGGER_DAILY_DIGEST
        })
        return scheduled
    }

    fun triggerTestDailyDigest(): Boolean {
        val intent = Intent(context, DailyReminderReceiver::class.java).apply {
            action = DailyReminderReceiver.ACTION_TRIGGER_DAILY_DIGEST
            data = Uri.parse("lectures://alarm/daily_digest_test")
            putExtra("IS_TEST", true)
        }
        context.sendBroadcast(intent)
        return true
    }

    fun cancelDailyReminder() {
        val manager = alarmManager ?: return
        cancelAlarmToken(context, manager, DAILY_DIGEST_REQUEST_CODE, Intent(context, DailyReminderReceiver::class.java).apply {
            action = DailyReminderReceiver.ACTION_TRIGGER_DAILY_DIGEST
        })
        val intent = Intent(context, DailyReminderReceiver::class.java).apply {
            action = DailyReminderReceiver.ACTION_TRIGGER_DAILY_DIGEST
            data = DAILY_DIGEST_URI
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            DAILY_DIGEST_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            manager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Cancelled daily reminder alarm")
        }
    }
}
