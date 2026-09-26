package com.mustime.core.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.mustime.core.notification.NotificationHelper

class TaskReminderReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TRIGGER_TASK_REMINDER = "com.mustime.ACTION_TRIGGER_TASK_REMINDER"
        const val EXTRA_TASK_ID = "EXTRA_TASK_ID"
        const val EXTRA_TASK_TITLE = "EXTRA_TASK_TITLE"
        const val EXTRA_TASK_COURSE = "EXTRA_TASK_COURSE"
        const val EXTRA_TASK_DUE = "EXTRA_TASK_DUE"
        const val EXTRA_TASK_PRIORITY = "EXTRA_TASK_PRIORITY"

        private const val TAG = "TaskReminderReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        val title = intent.getStringExtra(EXTRA_TASK_TITLE) ?: "Academic Task"
        val course = intent.getStringExtra(EXTRA_TASK_COURSE) ?: "Coursework"
        val due = intent.getStringExtra(EXTRA_TASK_DUE) ?: "Due Soon"
        val priority = intent.getStringExtra(EXTRA_TASK_PRIORITY) ?: "Medium"

        Log.d(TAG, "AlarmManager triggered reminder for Task #$taskId: $title ($course)")

        if (taskId != -1L) {
            NotificationHelper.showTaskReminderNotification(
                context = context,
                taskId = taskId,
                title = title,
                courseCode = course,
                dueDate = due,
                priority = priority
            )
        }
    }
}
