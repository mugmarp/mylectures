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

        Log.d("ClassAlarmReceiver", "Class reminder fired for $courseCode in $room at $startTime")

        NotificationHelper.showClassReminderNotification(
            context = context,
            courseCode = courseCode,
            courseTitle = courseTitle,
            room = room,
            startTime = startTime,
            lecturer = lecturer
        )
    }
}
