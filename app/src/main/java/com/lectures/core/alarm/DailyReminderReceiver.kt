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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DailyReminderReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TRIGGER_DAILY_DIGEST = "com.lectures.ACTION_TRIGGER_DAILY_DIGEST"
        private const val TAG = "DailyReminderReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val isTest = intent.getBooleanExtra("IS_TEST", false)
        Log.d(TAG, "Daily reminder broadcast received (isTest=$isTest)")

        val app = context.applicationContext as? TimetableApplication ?: return
        val repository = app.repository
        val etagStore = app.etagStore

        if (!etagStore.isNotificationsEnabled() && !isTest) {
            ReminderScheduler(context).cancelDailyReminder()
            return
        }
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                withTimeout(8_500L) {
                    val notificationsEnabled = etagStore.notificationsEnabledPref.firstOrNull()
                        ?: etagStore.isNotificationsEnabled()
                    if (!notificationsEnabled && !isTest) {
                        Log.d(TAG, "Notifications disabled; skipping daily digest and cancelling future schedule")
                        ReminderScheduler(context).cancelDailyReminder()
                        return@withTimeout
                    }

                    // Re-arm for tomorrow morning only when notifications are enabled and this was an actual alarm trigger
                    if (!isTest) {
                        ReminderScheduler(context).scheduleDailyReminder()
                    }

                    // Determine today's day of week
                    val cal = Calendar.getInstance()
                    val dayFormat = SimpleDateFormat("EEEE", Locale.US)
                    val dayOfWeek = dayFormat.format(cal.time)

                    val programme = etagStore.getSavedProgramme()?.trim()
                        ?: repository.getInitialProgramme()?.trim()
                        ?: ""
                    val classes = if (programme.isNotEmpty()) {
                        repository.getLocalSchedule(programme).firstOrNull()?.filter {
                            it.dayOfWeek.trim().equals(dayOfWeek, ignoreCase = true) ||
                                it.dayOfWeek.trim().startsWith(dayOfWeek.take(3), ignoreCase = true)
                        }?.sortedBy { it.startTime } ?: emptyList()
                    } else {
                        repository.getScheduleForDay(dayOfWeek).firstOrNull()?.sortedBy { it.startTime } ?: emptyList()
                    }

                    NotificationHelper.showDailyDigestNotification(
                        context = context,
                        dayOfWeek = dayOfWeek,
                        classes = classes
                    )

                    // Self-healing daily safety net: Ensure today's and this week's enrolled class alarms are armed
                    if (programme.isNotEmpty() && !isTest) {
                        val allCohortClasses = repository.getLocalSchedule(programme).firstOrNull() ?: emptyList()
                        if (allCohortClasses.isNotEmpty()) {
                            val leadMinutes = etagStore.getClassAlarmLeadMinutes()
                            AlarmScheduler(context).scheduleAllEnrolledClassAlarms(allCohortClasses, leadMinutes, customAlarms = repository.getNotesWithAlarms())
                            Log.d(TAG, "Daily morning check armed enrolled class alarms with lead=$leadMinutes min")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error generating daily schedule digest: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
