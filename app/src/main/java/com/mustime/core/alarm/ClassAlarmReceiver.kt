package com.mustime.core.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.mustime.core.notification.NotificationHelper

class ClassAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val courseCode = intent.getStringExtra("COURSE_CODE") ?: "Lecture"
        val courseTitle = intent.getStringExtra("COURSE_TITLE") ?: "Class"
        val room = intent.getStringExtra("ROOM") ?: "Classroom"
        val startTime = intent.getStringExtra("START_TIME") ?: "Soon"
        val lecturer = intent.getStringExtra("LECTURER") ?: ""
        val isAlarm = intent.getBooleanExtra("IS_ALARM", false)
        val minutesBefore = intent.getIntExtra("MINUTES_BEFORE", 30)

        Log.d("ClassAlarmReceiver", "Class notification fired (isAlarm=$isAlarm) for $courseCode in $room at $startTime")

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
    }
}
