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

    @Test
    fun testClassAlarmDismissActionRemovesNotification() {
        val receiver = ClassAlarmReceiver()
        val notifId = 44556

        val builder = android.app.Notification.Builder(context, NotificationHelper.CHANNEL_CLASS_ALARMS)
            .setContentTitle("Test Alarm")
            .setContentText("Dismiss me")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
        notificationManager.notify(notifId, builder.build())
        assertFalse("Notification should exist before dismiss", shadowNotificationManager.allNotifications.isEmpty())

        val dismissIntent = Intent(context, ClassAlarmReceiver::class.java).apply {
            action = ClassAlarmReceiver.ACTION_DISMISS_CLASS_ALARM
            putExtra(ClassAlarmReceiver.EXTRA_NOTIFICATION_ID, notifId)
        }
        receiver.onReceive(context, dismissIntent)

        assertTrue("Notification should be cancelled after dismiss action", shadowNotificationManager.allNotifications.isEmpty())
    }

    @Test
    fun testClassAlarmSnoozeActionSchedulesTenMinutesAndPreservesMetadata() {
        val receiver = ClassAlarmReceiver()
        val notifId = 99881

        val builder = android.app.Notification.Builder(context, NotificationHelper.CHANNEL_CLASS_ALARMS)
            .setContentTitle("Class Alert")
            .setContentText("Snooze me")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
        notificationManager.notify(notifId, builder.build())

        val snoozeIntent = Intent(context, ClassAlarmReceiver::class.java).apply {
            action = ClassAlarmReceiver.ACTION_SNOOZE_CLASS_ALARM
            putExtra(ClassAlarmReceiver.EXTRA_NOTIFICATION_ID, notifId)
            putExtra("COURSE_CODE", "SWE2101")
            putExtra("COURSE_TITLE", "Object Oriented Programming")
            putExtra("ROOM", "SFL01")
            putExtra("START_TIME", "10:00 AM")
            putExtra("MINUTES_BEFORE", 15) // Custom user lead time
            putExtra("LECTURER", "Dr. Andrew")
            putExtra("NATURAL_KEY", "SWE2101|OOP|Monday|10:00|SFL01")
            putExtra("DAY_OF_WEEK", "Monday")
        }

        receiver.onReceive(context, snoozeIntent)

        // 1. Notification was dismissed
        assertTrue("Original notification should be dismissed upon snooze", shadowNotificationManager.allNotifications.isEmpty())

        // 2. Alarm was scheduled in AlarmManager
        val scheduledAlarms = shadowAlarmManager.scheduledAlarms
        assertFalse("A snooze alarm must be scheduled in AlarmManager", scheduledAlarms.isEmpty())

        val snoozeAlarm = scheduledAlarms.last()
        val expectedMinTime = System.currentTimeMillis() + (9 * 60 * 1000L)
        val expectedMaxTime = System.currentTimeMillis() + (11 * 60 * 1000L)
        assertTrue("Snooze trigger should be ~10 minutes in future", snoozeAlarm.triggerAtTime in expectedMinTime..expectedMaxTime)

        // 3. Metadata preserved in pending intent
        val pendingIntent = snoozeAlarm.operation
        val shadowPendingIntent = shadowOf(pendingIntent)
        val scheduledIntent = shadowPendingIntent.savedIntent

        assertNotNull("Snooze intent must exist", scheduledIntent)
        assertEquals("SWE2101", scheduledIntent.getStringExtra("COURSE_CODE"))
        assertEquals("Object Oriented Programming", scheduledIntent.getStringExtra("COURSE_TITLE"))
        assertEquals("SFL01", scheduledIntent.getStringExtra("ROOM"))
        assertEquals("10:00 AM", scheduledIntent.getStringExtra("START_TIME"))
        assertEquals(15, scheduledIntent.getIntExtra("MINUTES_BEFORE", -1))
        assertEquals("Dr. Andrew", scheduledIntent.getStringExtra("LECTURER"))
        assertEquals("SWE2101|OOP|Monday|10:00|SFL01", scheduledIntent.getStringExtra("NATURAL_KEY"))
        assertEquals("Monday", scheduledIntent.getStringExtra("DAY_OF_WEEK"))
        assertFalse("Snooze alarm must not be recurring", scheduledIntent.getBooleanExtra("IS_RECURRING", true))
        assertTrue("Snooze alarm must remain an alarm", scheduledIntent.getBooleanExtra("IS_ALARM", false))
    }
}
