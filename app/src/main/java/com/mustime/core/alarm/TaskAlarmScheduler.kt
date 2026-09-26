package com.mustime.core.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.mustime.features.timetable.domain.Assignment

class TaskAlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
    private val TAG = "TaskAlarmScheduler"

    /**
     * Schedules an AlarmManager alarm for an upcoming academic task reminder.
     * Returns true if scheduled, false if skipped (e.g. completed, in past, or invalid time).
     */
    fun scheduleTaskReminder(task: Assignment): Boolean {
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager service not available")
            return false
        }

        if (task.completed) {
            cancelTaskReminder(task.id)
            return false
        }

        val triggerAtMillis = TaskDateTimeParser.calculateTriggerTimeMillis(task.dueDate, task.reminderMinutes)
        if (triggerAtMillis == null || triggerAtMillis <= System.currentTimeMillis()) {
            Log.d(TAG, "Task ${task.id} due date has already passed or cannot be resolved: ${task.dueDate}")
            return false
        }

        val pendingIntent = createReminderPendingIntent(task)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                    Log.d(TAG, "Scheduled EXACT idle alarm for task ${task.id} at $triggerAtMillis")
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                    Log.d(TAG, "Scheduled INEXACT idle alarm (exact permission not granted) for task ${task.id}")
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
                Log.d(TAG, "Scheduled EXACT idle alarm for task ${task.id} at $triggerAtMillis")
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
            return true
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException scheduling exact alarm, falling back to setAndAllowWhileIdle: ${e.message}")
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
                return true
            } catch (fallbackEx: Exception) {
                Log.e(TAG, "Fallback alarm scheduling also failed: ${fallbackEx.message}")
                return false
            }
        }
    }

    /**
     * Schedules a quick test reminder alarm (e.g. in 5 seconds) to allow instant testing
     * and verification of AlarmManager and NotificationChannel triggering.
     */
    fun scheduleTestReminder(task: Assignment, delaySeconds: Int = 5) {
        if (alarmManager == null) return

        val triggerAtMillis = System.currentTimeMillis() + (delaySeconds.coerceAtLeast(1) * 1000L)
        val pendingIntent = createReminderPendingIntent(task)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
            Log.d(TAG, "Scheduled test reminder alarm in ${delaySeconds}s for task ${task.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule test alarm: ${e.message}")
        }
    }

    /**
     * Cancels any scheduled AlarmManager alarm for the given task.
     */
    fun cancelTaskReminder(taskId: Long) {
        if (alarmManager == null) return

        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            action = TaskReminderReceiver.ACTION_TRIGGER_TASK_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (taskId % Int.MAX_VALUE).toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Cancelled alarm for task $taskId")
        }
    }

    /**
     * Cancels alarms for all provided tasks.
     */
    fun cancelAllTaskReminders(tasks: List<Assignment>) {
        tasks.forEach { cancelTaskReminder(it.id) }
    }

    private fun createReminderPendingIntent(task: Assignment): PendingIntent {
        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            action = TaskReminderReceiver.ACTION_TRIGGER_TASK_REMINDER
            putExtra(TaskReminderReceiver.EXTRA_TASK_ID, task.id)
            putExtra(TaskReminderReceiver.EXTRA_TASK_TITLE, task.title)
            putExtra(TaskReminderReceiver.EXTRA_TASK_COURSE, task.courseCode)
            putExtra(TaskReminderReceiver.EXTRA_TASK_DUE, task.dueDate)
            putExtra(TaskReminderReceiver.EXTRA_TASK_PRIORITY, task.priority)
        }

        return PendingIntent.getBroadcast(
            context,
            (task.id % Int.MAX_VALUE).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
