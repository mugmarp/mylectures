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
    const val CHANNEL_DAILY_DIGEST = "daily_class_digest"

    private const val TAG = "NotificationHelper"

    /**
     * Applies the user's selected sound profile to all notification channels.
     * "System Default" uses the device's default notification sound;
     * named profiles map to bundled raw audio resources.
     */
    fun applySoundProfile(context: Context, soundName: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val soundUri = when (soundName.lowercase()) {
            "chime" -> android.net.Uri.parse("android.resource://${context.packageName}/raw/sound_chime")
            "gentle bell" -> android.net.Uri.parse("android.resource://${context.packageName}/raw/sound_gentle_bell")
            "standard" -> android.net.Uri.parse("android.resource://${context.packageName}/raw/sound_standard")
            else -> android.media.RingtoneManager.getActualDefaultRingtoneUri(
                context, android.media.RingtoneManager.TYPE_NOTIFICATION
            )
        }
        if (soundUri == null) return
        val notifAudioAttrs = android.media.AudioAttributes.Builder()
            .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val alarmAudioAttrs = android.media.AudioAttributes.Builder()
            .setUsage(android.media.AudioAttributes.USAGE_ALARM)
            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        notificationManager.getNotificationChannel(CHANNEL_ACADEMIC_TASKS)?.apply { setSound(soundUri, notifAudioAttrs) }
        notificationManager.getNotificationChannel(CHANNEL_CLASSES)?.apply { setSound(soundUri, notifAudioAttrs) }
        notificationManager.getNotificationChannel(CHANNEL_DAILY_DIGEST)?.apply { setSound(soundUri, notifAudioAttrs) }
        notificationManager.getNotificationChannel(CHANNEL_CLASS_ALARMS)?.apply { setSound(soundUri, alarmAudioAttrs) }
    }

    /**
     * Plays a preview of the requested sound profile.
     */
    fun previewSound(context: Context, soundName: String) {
        try {
            val soundUri = when (soundName.lowercase()) {
                "chime" -> android.net.Uri.parse("android.resource://${context.packageName}/raw/sound_chime")
                "gentle bell" -> android.net.Uri.parse("android.resource://${context.packageName}/raw/sound_gentle_bell")
                "standard" -> android.net.Uri.parse("android.resource://${context.packageName}/raw/sound_standard")
                else -> android.media.RingtoneManager.getActualDefaultRingtoneUri(
                    context, android.media.RingtoneManager.TYPE_NOTIFICATION
                )
            } ?: return
            val ringtone = android.media.RingtoneManager.getRingtone(context, soundUri)
            ringtone?.play()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to preview sound: $soundName", e)
        }
    }

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

            // Channel 3: Class Alarms (Urgent Alarm Stream)
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
                val alarmAttrs = android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                val alarmSound = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
                    ?: android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
                setSound(alarmSound, alarmAttrs)
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            // Channel 4: Daily Class Schedule Digest
            val digestChannel = NotificationChannel(
                CHANNEL_DAILY_DIGEST,
                "Daily Schedule Digest",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Morning overview of your scheduled classes and venues for the day."
                enableLights(true)
                lightColor = Color.parseColor("#3B82F6")
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            notificationManager.createNotificationChannels(listOf(taskChannel, classChannel, alarmChannel, digestChannel))
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

    /**
     * Builds and displays a daily morning digest summarizing the student's scheduled lectures for today.
     */
    fun showDailyDigestNotification(
        context: Context,
        dayOfWeek: String,
        classes: List<com.mustime.features.timetable.domain.TimetableEntry>
    ) {
        createNotificationChannels(context)

        val notificationId = 88002

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

        val title = "Today's Schedule • $dayOfWeek"
        val snippet: String
        val bigText: String

        if (classes.isEmpty()) {
            snippet = "No lectures scheduled for today. Great day for coursework!"
            bigText = buildString {
                append("Good morning!\n\n")
                append("You have no scheduled lectures or practical labs today ($dayOfWeek).\n\n")
                append("Use this time to review lecture notes, work on assignments, or prepare ahead.")
            }
        } else {
            val first = classes.first()
            val venue = if (!first.room.isNullOrBlank()) " in ${first.room}" else ""
            snippet = "${classes.size} class${if (classes.size > 1) "es" else ""} today. First: ${first.courseCode} at ${first.startTime}$venue"
            bigText = buildString {
                append("Good morning! You have ${classes.size} class${if (classes.size > 1) "es" else ""} scheduled for today:\n\n")
                classes.forEachIndexed { index, entry ->
                    val roomText = if (!entry.room.isNullOrBlank()) " @ ${entry.room}" else ""
                    val timeRange = if (entry.endTime.isNotBlank()) "${entry.startTime} – ${entry.endTime}" else entry.startTime
                    append("${index + 1}. ${entry.courseCode} ($timeRange)$roomText\n")
                    if (entry.courseTitle.isNotBlank() && entry.courseTitle != entry.courseCode) {
                        append("   ${entry.courseTitle}\n")
                    }
                }
                append("\nHave a productive academic day!")
            }
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_DAILY_DIGEST)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle(title)
            .setContentText(snippet)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .setColor(Color.parseColor("#3B82F6"))

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
            Log.d(TAG, "Daily digest notification posted for $dayOfWeek with ${classes.size} classes")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while posting daily digest: ${e.message}")
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
