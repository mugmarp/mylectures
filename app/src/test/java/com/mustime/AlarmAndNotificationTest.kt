package com.mustime

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.mustime.core.alarm.TaskAlarmScheduler
import com.mustime.core.alarm.TaskDateTimeParser
import com.mustime.core.notification.NotificationHelper
import com.mustime.features.timetable.domain.Assignment
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AlarmAndNotificationTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun testNotificationChannelsCreatedWithHighImportance() {
        NotificationHelper.createNotificationChannels(context)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val taskChannel = notificationManager.getNotificationChannel(NotificationHelper.CHANNEL_ACADEMIC_TASKS)
        val classChannel = notificationManager.getNotificationChannel(NotificationHelper.CHANNEL_CLASSES)

        assertNotNull("Academic task reminder channel must exist", taskChannel)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, taskChannel.importance)
        assertTrue(taskChannel.name.toString().contains("Academic", ignoreCase = true))

        assertNotNull("Class reminder channel must exist", classChannel)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, classChannel.importance)
    }

    @Test
    fun testTaskDateTimeParserDueCalculation() {
        val now = Calendar.getInstance()
        val dueMillis = TaskDateTimeParser.calculateDueMillis("Tomorrow • 15:00")
        assertNotNull(dueMillis)
        assertTrue("Due time for tomorrow should be in the future", dueMillis!! > now.timeInMillis)

        val cal = Calendar.getInstance().apply { timeInMillis = dueMillis }
        assertEquals(15, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, cal.get(Calendar.MINUTE))
    }

    @Test
    fun testTaskDateTimeParserTriggerOffset() {
        val dueDateStr = "Tomorrow • 14:00"
        val dueMillis = TaskDateTimeParser.calculateDueMillis(dueDateStr)
        assertNotNull(dueMillis)

        val reminderMinutes = 30
        val triggerMillis = TaskDateTimeParser.calculateTriggerTimeMillis(dueDateStr, reminderMinutes)
        assertNotNull(triggerMillis)

        // Trigger should be exactly 30 minutes before due time
        val expectedDifference = 30 * 60 * 1000L
        assertEquals(expectedDifference, dueMillis!! - triggerMillis!!)
    }

    @Test
    fun testTaskAlarmSchedulerScheduleAndCancel() {
        val scheduler = TaskAlarmScheduler(context)
        val task = Assignment(
            id = 42L,
            title = "Pathology Lab Report",
            courseCode = "PAT2201",
            dueDate = "Tomorrow • 18:00",
            reminderMinutes = 60,
            priority = "High",
            completed = false
        )

        val scheduled = scheduler.scheduleTaskReminder(task)
        assertTrue("Task should be scheduled successfully", scheduled)

        // Verify with shadow alarm manager
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        val shadowAlarmManager = shadowOf(alarmManager)
        val nextAlarm = shadowAlarmManager.nextScheduledAlarm
        assertNotNull("An alarm should be scheduled in AlarmManager", nextAlarm)

        // Cancel alarm
        scheduler.cancelTaskReminder(task.id)
    }

    @Test
    fun testNotificationDisplay() {
        NotificationHelper.showTaskReminderNotification(
            context = context,
            taskId = 101L,
            title = "Biochemistry Presentation",
            courseCode = "BCH2101",
            dueDate = "Tomorrow • 10:00",
            priority = "High"
        )

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val shadowNotificationManager = shadowOf(notificationManager)
        val notifications = shadowNotificationManager.allNotifications

        assertTrue("Notification should be posted", notifications.isNotEmpty())
        val notification = notifications.first()
        assertEquals(NotificationHelper.CHANNEL_ACADEMIC_TASKS, notification.channelId)
    }
}
