package com.mustime.core.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.mustime.core.notification.NotificationHelper
import com.mustime.features.timetable.domain.TimetableEntry

class ClassAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val courseCode = intent.getStringExtra("COURSE_CODE") ?: "Lecture"
        val courseTitle = intent.getStringExtra("COURSE_TITLE") ?: "Class"
        val room = intent.getStringExtra("ROOM") ?: "Classroom"
        val startTime = intent.getStringExtra("START_TIME") ?: "Soon"
        val lecturer = intent.getStringExtra("LECTURER") ?: ""
        val isAlarm = intent.getBooleanExtra("IS_ALARM", false)
        val minutesBefore = intent.getIntExtra("MINUTES_BEFORE", 30)
        val isRecurring = intent.getBooleanExtra("IS_RECURRING", true)
        val isTest = intent.getBooleanExtra("IS_TEST", false)
        val naturalKey = intent.getStringExtra("NATURAL_KEY")
        val dayOfWeek = intent.getStringExtra("DAY_OF_WEEK")

        Log.d("ClassAlarmReceiver", "Class alert fired (isAlarm=$isAlarm, isRecurring=$isRecurring) for $courseCode in $room at $startTime")

        if (isAlarm) {
            NotificationHelper.showClassAlarmNotification(
                context = context,
                courseCode = courseCode,
                courseTitle = courseTitle,
                room = room,
                startTime = startTime
            )
        } else {
            NotificationHelper.showClassReminderNotification(
                context = context,
                courseCode = courseCode,
                courseTitle = courseTitle,
                room = room,
                startTime = startTime,
                minutesBefore = minutesBefore,
                lecturer = lecturer
            )
        }

        // Reschedule recurring alarm for next week (+7 days)
        if (isAlarm && isRecurring && !isTest && !naturalKey.isNullOrBlank() && !dayOfWeek.isNullOrBlank()) {
            try {
                val syntheticEntry = TimetableEntry(
                    natural_key = naturalKey,
                    program_group = "",
                    day = dayOfWeek,
                    time_slot = startTime,
                    start_time = startTime,
                    end_time = null,
                    course_code = courseCode,
                    course_title = courseTitle,
                    session_type = null,
                    lecturer = lecturer,
                    room = room
                )
                AlarmScheduler(context).scheduleClassAlarm(syntheticEntry, minutesBefore)
                Log.d("ClassAlarmReceiver", "Successfully re-armed weekly class alarm for $courseCode on next $dayOfWeek")
            } catch (e: Exception) {
                Log.e("ClassAlarmReceiver", "Failed to reschedule weekly class alarm: ${e.message}")
            }
        }
    }
}
