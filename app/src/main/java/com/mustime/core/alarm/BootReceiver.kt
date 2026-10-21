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
            NotificationHelper.applySoundProfile(
                context, com.mustime.features.timetable.data.ETagStore(context).getAlarmSound()
            )

            val app = context.applicationContext as? TimetableApplication ?: return
            val repository = app.repository

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val taskScheduler = TaskAlarmScheduler(context)
                    val classScheduler = AlarmScheduler(context)
                    val reminderScheduler = ReminderScheduler(context)

                    // 1. Re-arm Daily Digest
                    reminderScheduler.scheduleDailyReminder()

                    // 2. Re-arm Academic Task Reminders
                    val allAssignments = repository.getAssignments().firstOrNull() ?: emptyList()
                    var taskCount = 0
                    allAssignments.forEach { task ->
                        if (!task.completed && task.reminderMinutes != null) {
                            val scheduled = taskScheduler.scheduleTaskReminder(task)
                            if (scheduled) taskCount++
                        }
                    }

                    // 3. Re-arm Custom Class Alarms
                    val notesWithAlarms = repository.getNotesWithAlarms()
                    var classCount = 0
                    notesWithAlarms.forEach { note ->
                        val mins = note.alarmMinutes ?: return@forEach
                        val entry = repository.getTimetableEntryByNaturalKey(note.naturalKey)
                        val ok = if (entry != null) {
                            classScheduler.scheduleClassAlarm(entry, mins)
                        } else {
                            classScheduler.scheduleClassAlarmFromNote(note, mins)
                        }
                        if (ok) classCount++
                    }

                    Log.d(TAG, "Boot reschedule complete: $taskCount tasks, $classCount class alarms, and daily digest scheduled.")
                } catch (e: Exception) {
                    Log.e(TAG, "Error rescheduling alarms on boot: ${e.message}", e)
                }
            }
        }
    }
}
