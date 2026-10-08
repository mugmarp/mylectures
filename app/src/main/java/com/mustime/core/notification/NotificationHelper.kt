package com.mustime.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.mustime.MainActivity
import com.mustime.R
import com.mustime.core.alarm.TaskActionReceiver

object NotificationHelper {

    const val CHANNEL_ACADEMIC_TASKS = "academic_task_reminders"
    const val CHANNEL_CLASSES = "class_reminders"
    const val CHANNEL_CLASS_ALARMS = "class_alarms"

    private const val TAG = "NotificationHelper"

    /**
     * Creates and registers Notification Channels with Android OS (API 26+).
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            // Channel 1: Academic Tasks & Coursework Deadlines
            val taskChannel = NotificationChannel(
                CHANNEL_ACADEMIC_TASKS,
                "Academic Task & Deadline Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies you of upcoming assignments, coursework deadlines, tests, and academic tasks."
                enableLights(true)
                lightColor = Color.parseColor("#2563EB")
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 350, 200, 350)
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            // Channel 2: Class & Lecture Reminders
            val classChannel = NotificationChannel(
                CHANNEL_CLASSES,
                "Upcoming Class Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders before scheduled lectures, practical labs, and tutorials."
                enableLights(true)
                lightColor = Color.parseColor("#10B981")
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 150, 250)
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            // Channel 3: Class Alarms
            val alarmChannel = NotificationChannel(
                CHANNEL_CLASS_ALARMS,
                "Class Alerts & Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Immediate alerts and sound alarms for upcoming lectures."
                enableLights(true)
                lightColor = Color.parseColor("#EF4444")
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            notificationManager.createNotificationChannels(listOf(taskChannel, classChannel, alarmChannel))
            Log.d(TAG, "Notification channels registered successfully")
        }
    }

    /**
     * Builds and displays a rich notification for an upcoming academic task reminder.
     */
    fun showTaskReminderNotification(
        context: Context,
        taskId: Long,
        title: String,
        courseCode: String,
        dueDate: String,
        priority: String
    ) {
        createNotificationChannels(context)

        val notificationId = (taskId % Int.MAX_VALUE).toInt()

        // 1. Content Intent: Launch app into Tasks tab
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_NAV_TAB", 3) // Tasks tab
            putExtra("EXTRA_TASK_ID", taskId)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 2. Action Intent: Mark as Done directly from notification shade
        val markDoneIntent = Intent(context, TaskActionReceiver::class.java).apply {
            action = TaskActionReceiver.ACTION_MARK_TASK_DONE
            putExtra(TaskActionReceiver.EXTRA_TASK_ID, taskId)
        }
        val markDonePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 100_000,
            markDoneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val priorityText = priority.replaceFirstChar { it.uppercase() }
        val notificationTitle = "Due Soon: $title"
        val notificationSnippet = "$courseCode • Due: $dueDate ($priorityText Priority)"
        val bigText = buildString {
            append("📌 Academic Task Reminder\n\n")
            append("Course: $courseCode\n")
            append("Task: $title\n")
            append("Due: $dueDate\n")
            append("Priority: $priorityText\n\n")
            append("Be sure to review and submit your coursework on time!")
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ACADEMIC_TASKS)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle(notificationTitle)
            .setContentText(notificationSnippet)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .setColor(Color.parseColor("#2563EB"))
            .addAction(R.drawable.ic_stat_notification, "Mark Done", markDonePendingIntent)
            .addAction(R.drawable.ic_stat_notification, "View Tasks", contentPendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
            Log.d(TAG, "Notification posted for task $taskId: $title")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while posting notification (POST_NOTIFICATIONS missing): ${e.message}")
        }
    }

    /**
     * Builds and displays a notification for an upcoming lecture/class reminder.
     * Format requested:
     * Title: Upcoming class
     * Content: Object Oriented Programming - SWE2101
     *          starts in 30 minutes in SFL01 (Computer Lab).
     */
    fun showClassReminderNotification(
        context: Context,
        courseCode: String,
        courseTitle: String,
        room: String,
        startTime: String,
        minutesBefore: Int = 30,
        lecturer: String = ""
    ) {
        createNotificationChannels(context)

        val notificationId = ("reminder_$courseCode$room$startTime").hashCode()

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_NAV_TAB", 0) // Timetable tab
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "Upcoming class"
        val headerSubject = if (courseTitle.isNotBlank() && courseTitle != courseCode) {
            "$courseTitle - $courseCode"
        } else {
            courseCode
        }

        val leadTimeText = if (minutesBefore > 0) "starts in $minutesBefore minutes" else "starts now"
        val locationText = if (room.isNotBlank()) " in $room" else ""
        val subtitle = "$leadTimeText$locationText."
        val lecturerDetails = if (lecturer.isNotBlank()) "\nLecturer: $lecturer" else ""
        val detailedText = "$headerSubject\n$subtitle$lecturerDetails"

        val builder = NotificationCompat.Builder(context, CHANNEL_CLASSES)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(detailedText)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .setColor(Color.parseColor("#10B981"))

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while posting class notification: ${e.message}")
        }
    }

    /**
     * Builds and displays an alarm alert for a scheduled class alarm.
     * Format requested:
     * Title: Class Alert
     * Content: Object Oriented Programming - SWE2101
     *          Starts at 10:00 AM
     *          In SFL01 (Computer Lab)
     */
    fun showClassAlarmNotification(
        context: Context,
        courseCode: String,
        courseTitle: String,
        room: String,
        startTime: String
    ) {
        createNotificationChannels(context)

        val notificationId = ("alarm_$courseCode$room$startTime").hashCode()

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_NAV_TAB", 0) // Timetable tab
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "Class Alert"
        val headerSubject = if (courseTitle.isNotBlank() && courseTitle != courseCode) {
            "$courseTitle - $courseCode"
        } else {
            courseCode
        }

        val startsAtText = "Starts at $startTime"
        val venueText = if (room.isNotBlank()) "In $room" else ""
        val detailedText = "$headerSubject\n$startsAtText\n$venueText".trimEnd()

        val builder = NotificationCompat.Builder(context, CHANNEL_CLASS_ALARMS)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle(title)
            .setContentText("$headerSubject • $startsAtText")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(detailedText)
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .setColor(Color.parseColor("#EF4444"))

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while posting class alarm notification: ${e.message}")
        }
    }

    fun cancelNotification(context: Context, notificationId: Int) {
        try {
            NotificationManagerCompat.from(context).cancel(notificationId)
        } catch (e: Exception) {
            Log.e(TAG, "Error canceling notification: ${e.message}")
        }
    }
}
