package com.lectures.core.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.lectures.TimetableApplication
import com.lectures.core.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class BootReceiver : BroadcastReceiver() {

    private val TAG = "BootReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            Log.d(TAG, "Boot or package update detected. Rescheduling alarms and initializing channels.")

            val app = context.applicationContext as? TimetableApplication ?: return
            NotificationHelper.createNotificationChannels(context)
            NotificationHelper.applySoundProfile(
                context, app.etagStore.getAlarmSound()
            )

            val repository = app.repository
            val etagStore = app.etagStore

            if (!etagStore.isNotificationsEnabled()) return
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    withTimeout(8_500L) {
                        if (!etagStore.isNotificationsEnabled()) {
                            Log.d(TAG, "Quiet Mode / notifications disabled; skipping all boot alarm rescheduling.")
                            return@withTimeout
                        }

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

                        // 4. Re-arm Enrolled Programme Class Alarms
                        val savedProgramme = etagStore.getSavedProgramme()?.trim()
                            ?: repository.getInitialProgramme()?.trim()
                            ?: ""
                        var enrolledClassCount = 0
                        if (savedProgramme.isNotEmpty()) {
                            val enrolledClasses = repository.getLocalSchedule(savedProgramme).firstOrNull() ?: emptyList()
                            val leadMinutes = etagStore.getClassAlarmLeadMinutes()
                            enrolledClassCount = classScheduler.scheduleAllEnrolledClassAlarms(enrolledClasses, leadMinutes, customAlarms = repository.getNotesWithAlarms())
                        }

                        Log.d(
                            TAG,
                            "Boot reschedule complete: $taskCount tasks, $classCount custom notes, $enrolledClassCount enrolled classes, and daily digest scheduled."
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error rescheduling alarms on boot: ${e.message}", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
