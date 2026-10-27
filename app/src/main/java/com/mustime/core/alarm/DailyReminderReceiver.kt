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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DailyReminderReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TRIGGER_DAILY_DIGEST = "com.mustime.ACTION_TRIGGER_DAILY_DIGEST"
        private const val TAG = "DailyReminderReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val isTest = intent.getBooleanExtra("IS_TEST", false)
        Log.d(TAG, "Daily reminder broadcast received (isTest=$isTest)")

        // Re-arm for tomorrow morning if this was an actual alarm trigger
        if (!isTest) {
            ReminderScheduler(context).scheduleDailyReminder()
        }

        val app = context.applicationContext as? TimetableApplication ?: return
        val repository = app.repository
        val etagStore = app.etagStore

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val notificationsEnabled = etagStore.notificationsEnabledPref.firstOrNull() ?: true
                if (!notificationsEnabled && !isTest) {
                    Log.d(TAG, "Notifications disabled; skipping daily digest")
                    return@launch
                }

                // Determine today's day of week
                val cal = Calendar.getInstance()
                val dayFormat = SimpleDateFormat("EEEE", Locale.US)
                val dayOfWeek = dayFormat.format(cal.time)

                val programme = etagStore.getSavedProgramme()?.trim() ?: repository.getInitialProgramme()?.trim() ?: ""
                val classes = if (programme.isNotEmpty()) {
                    repository.getLocalSchedule(programme).firstOrNull()?.filter {
                        it.dayOfWeek.trim().equals(dayOfWeek, ignoreCase = true)
                    }?.sortedBy { it.startTime } ?: emptyList()
                } else {
                    repository.getScheduleForDay(dayOfWeek).firstOrNull()?.sortedBy { it.startTime } ?: emptyList()
                }

                NotificationHelper.showDailyDigestNotification(
                    context = context,
                    dayOfWeek = dayOfWeek,
                    classes = classes
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error generating daily schedule digest: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
