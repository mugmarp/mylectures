package com.mustime

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.mustime.core.alarm.AlarmScheduler
import com.mustime.core.alarm.ClassAlarmReceiver
import com.mustime.core.notification.NotificationHelper
import com.mustime.features.timetable.domain.TimetableEntry
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager
import org.robolectric.shadows.ShadowNotificationManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ClassAlarmAndNotificationTriggerTest {

    private lateinit var context: Context
    private lateinit var alarmScheduler: AlarmScheduler
    private lateinit var notificationManager: NotificationManager
    private lateinit var shadowNotificationManager: ShadowNotificationManager
    private lateinit var alarmManager: AlarmManager
    private lateinit var shadowAlarmManager: ShadowAlarmManager

    private val sampleEntry = TimetableEntry(
        natural_key = "BCS I|CSC1201|Monday|09:00|Hall 3",
        program_group = "BCS I",
        day = "Monday",
        time_slot = "09:00 - 11:00",
        start_time = "09:00",
        end_time = "11:00",
        course_code = "CSC1201",
        course_title = "Data Structures & Algorithms",
        session_type = "Lecture",
        lecturer = "Dr. Paul",
        room = "Engineering Hall 3",
        shared_with = emptyList()
    )

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        alarmScheduler = AlarmScheduler(context)
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        shadowNotificationManager = shadowOf(notificationManager)
        alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        shadowAlarmManager = shadowOf(alarmManager)

        // Ensure notification channels are registered before tests
        NotificationHelper.createNotificationChannels(context)
    }

    // =========================================================================
    // 1. CLASS ALARM SCHEDULING TESTS
    // =========================================================================

    @Test
    fun testScheduleClassAlarmRegistersInAlarmManager() {
        val minutesBefore = 15
        val scheduled = alarmScheduler.scheduleClassAlarm(sampleEntry, minutesBefore)
        assertTrue("AlarmScheduler should report success", scheduled)

        val scheduledAlarms = shadowAlarmManager.scheduledAlarms
        assertFalse("At least one alarm must be scheduled in AlarmManager", scheduledAlarms.isEmpty())

        val nextAlarm = scheduledAlarms.first()
        // Trigger time must be in the future
        assertTrue("Trigger time must be in the future", nextAlarm.triggerAtTime > System.currentTimeMillis())
        assertEquals(AlarmManager.RTC_WAKEUP, nextAlarm.type)
    }

    @Test
    fun testScheduleClassAlarmIntentExtras() {
        alarmScheduler.scheduleClassAlarm(sampleEntry, 10)

        val scheduledAlarms = shadowAlarmManager.scheduledAlarms
        assertFalse(scheduledAlarms.isEmpty())

        val pendingIntent = scheduledAlarms.first().operation
        val shadowPendingIntent = shadowOf(pendingIntent)
        val intent = shadowPendingIntent.savedIntent

        assertNotNull("PendingIntent must contain a valid Intent", intent)
        assertEquals("CSC1201", intent.getStringExtra("COURSE_CODE"))
        assertEquals("Data Structures & Algorithms", intent.getStringExtra("COURSE_TITLE"))
        assertEquals("Engineering Hall 3", intent.getStringExtra("ROOM"))
        assertEquals("09:00", intent.getStringExtra("START_TIME"))
    }

    @Test
    fun testCancelClassAlarmRemovesFromAlarmManager() {
        // Schedule an alarm
        alarmScheduler.scheduleClassAlarm(sampleEntry, 10)
        assertFalse("Alarm should exist after scheduling", shadowAlarmManager.scheduledAlarms.isEmpty())

        // Cancel it
        alarmScheduler.cancelClassAlarm(sampleEntry.naturalKey)

        // Verify alarm is removed from AlarmManager
        assertTrue("Scheduled alarms should be empty after cancellation", shadowAlarmManager.scheduledAlarms.isEmpty())
    }

    @Test
    fun testTriggerTestAlarmQuickChime() {
        val triggered = alarmScheduler.triggerTestAlarm(sampleEntry, delaySeconds = 3)
        assertTrue("Test alarm must return true", triggered)

        val scheduledAlarms = shadowAlarmManager.scheduledAlarms
        assertFalse(scheduledAlarms.isEmpty())

        val testAlarm = scheduledAlarms.last()
        val expectedTime = System.currentTimeMillis() + 3000L
        // Allow a small delta of +/- 500ms
        val delta = Math.abs(testAlarm.triggerAtTime - expectedTime)
        assertTrue("Test alarm should trigger ~3s in the future", delta < 1000L)
    }

    // =========================================================================
    // 2. BROADCAST RECEIVER & NOTIFICATION TRIGGER TESTS
    // =========================================================================

    @Test
    fun testClassAlarmReceiverTriggersNotification() {
        val receiver = ClassAlarmReceiver()
        val broadcastIntent = Intent(context, ClassAlarmReceiver::class.java).apply {
            putExtra("COURSE_CODE", "CSC1201")
            putExtra("COURSE_TITLE", "Data Structures & Algorithms")
            putExtra("ROOM", "Hall 3")
            putExtra("START_TIME", "09:00")
        }

        receiver.onReceive(context, broadcastIntent)

        val notifications = shadowNotificationManager.allNotifications
        assertFalse("A notification must be posted upon broadcast reception", notifications.isEmpty())

        val postedNotification = notifications.last()
        assertEquals(NotificationHelper.CHANNEL_CLASSES, postedNotification.channelId)

        val shadowNotification = shadowOf(postedNotification)
        assertEquals("Upcoming class", shadowNotification.contentTitle.toString())
        assertTrue(shadowNotification.contentText.toString().contains("starts in 30 minutes"))
    }

    @Test
    fun testNotificationChannelsConfiguration() {
        val classChannel = notificationManager.getNotificationChannel(NotificationHelper.CHANNEL_CLASSES)
        assertNotNull("Class reminder notification channel must exist", classChannel)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, classChannel.importance)
        assertTrue(classChannel.shouldVibrate())
        assertTrue(classChannel.shouldShowLights())

        val taskChannel = notificationManager.getNotificationChannel(NotificationHelper.CHANNEL_ACADEMIC_TASKS)
        assertNotNull("Academic task reminder notification channel must exist", taskChannel)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, taskChannel.importance)
    }
}
