package com.mustime.core.alarm

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.mustime.TimetableApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TaskActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_MARK_TASK_DONE = "com.mustime.ACTION_MARK_TASK_DONE"
        const val EXTRA_TASK_ID = "EXTRA_TASK_ID"
        private const val TAG = "TaskActionReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_MARK_TASK_DONE) {
            val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
            Log.d(TAG, "Marking Task #$taskId as completed from notification action")

            if (taskId != -1L) {
                // Dismiss notification
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                val notificationId = (taskId % Int.MAX_VALUE).toInt()
                notificationManager?.cancel(notificationId)

                // Update database
                val app = context.applicationContext as? TimetableApplication
                val repository = app?.repository
                if (repository != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            repository.updateAssignmentCompletion(taskId, true)
                            TaskAlarmScheduler(context).cancelTaskReminder(taskId)
                            Log.d(TAG, "Task #$taskId marked completed and alarms cancelled")
                        } catch (e: Exception) {
                            Log.e(TAG, "Error updating task completion: ${e.message}")
                        }
                    }
                }
            }
        }
    }
}
