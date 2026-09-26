package com.mustime.core.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.mustime.TimetableApplication
import com.mustime.core.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    private val TAG = "BootReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            Log.d(TAG, "Boot or package update detected. Rescheduling alarms and initializing channels.")

            NotificationHelper.createNotificationChannels(context)

            val app = context.applicationContext as? TimetableApplication ?: return
            val repository = app.repository

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val allAssignments = repository.getAssignments().firstOrNull() ?: emptyList()
                    val scheduler = TaskAlarmScheduler(context)

                    var count = 0
                    allAssignments.forEach { task ->
                        if (!task.completed && task.reminderMinutes != null) {
                            val scheduled = scheduler.scheduleTaskReminder(task)
                            if (scheduled) count++
                        }
                    }
                    Log.d(TAG, "Rescheduled $count academic task reminder alarms after reboot.")
                } catch (e: Exception) {
                    Log.e(TAG, "Error rescheduling task reminders on boot: ${e.message}")
                }
            }
        }
    }
}
