package com.lectures

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.lectures.core.alarm.*
import com.lectures.core.notification.NotificationHelper
import com.lectures.core.util.NotificationPermissionHelper
import com.lectures.features.timetable.domain.Assignment
import com.lectures.features.timetable.domain.LectureNote
import com.lectures.features.timetable.domain.TimetableEntry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AlarmDeliveryTest {
    private lateinit var app: TimetableApplication
    private lateinit var manager: AlarmManager

    @Before
    fun setUp() = runBlocking {
        app = ApplicationProvider.getApplicationContext()
        manager = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        app.etagStore.setNotificationsEnabled(true)
        clearAlarms()
        (app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancelAll()
    }

    @After
    fun tearDown() = runBlocking {
        clearAlarms()
        app.etagStore.setNotificationsEnabled(true)
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
    }

    @Test
    fun classKeysWithCollidingHashCodesKeepIndependentAlarms() {
        assertEquals("Aa".hashCode(), "BB".hashCode())
        val scheduler = AlarmScheduler(app)
        assertTrue(scheduler.scheduleClassAlarm(entry("Aa"), 15))
        assertTrue(scheduler.scheduleClassAlarm(entry("BB"), 15))
        assertEquals(2, alarms().size)
        scheduler.cancelClassAlarm("Aa")
        assertEquals("BB", shadowOf(alarms().single().operation).savedIntent.getStringExtra("NATURAL_KEY"))
    }

    @Test
    fun classCancellationRemovesRealAndSnoozedAlarmsButKeepsExplicitTest() {
        val scheduler = AlarmScheduler(app)
        assertTrue(scheduler.scheduleClassAlarm(entry("class"), 15))
        assertTrue(scheduler.triggerTestAlarm(entry("class"), 5))
        scheduler.scheduleSnoozeAlarm("CSC", "Lecture", "Hall", "09:00", naturalKey = "class")
        assertEquals(3, alarms().size)
        scheduler.cancelClassAlarm("class")
        assertEquals(1, alarms().size)
        assertTrue(shadowOf(alarms().single().operation).savedIntent.getBooleanExtra("IS_TEST", false))
    }

    @Test
    fun replacingLegacyClassAlarmDoesNotLeaveDuplicateDelivery() {
        val key = "legacy-class"
        legacyAlarm(key.hashCode(), Intent(app, ClassAlarmReceiver::class.java))
        assertTrue(AlarmScheduler(app).scheduleClassAlarm(entry(key), 20))
        assertEquals(1, alarms().size)
        assertNotNull(shadowOf(alarms().single().operation).savedIntent.data)
    }

    @Test
    fun taskTestAndRealAlarmsRemainIndependentEvenWithRequestCodeCollisions() {
        val scheduler = TaskAlarmScheduler(app)
        val first = task(7L)
        assertTrue(scheduler.scheduleTaskReminder(first))
        assertTrue(scheduler.scheduleTaskReminder(task(7L + Int.MAX_VALUE)))
        scheduler.scheduleTestReminder(first)
        assertEquals(3, alarms().size)
        scheduler.cancelTaskReminder(first.id)
        assertEquals(2, alarms().size)
        assertTrue(alarms().any { shadowOf(it.operation).savedIntent.getBooleanExtra("IS_TEST", false) })
    }

    @Test
    fun replacingLegacyTaskAndDigestAlarmsRemovesOldTokens() {
        legacyAlarm(22, Intent(app, TaskReminderReceiver::class.java).apply {
            action = TaskReminderReceiver.ACTION_TRIGGER_TASK_REMINDER
        })
        assertTrue(TaskAlarmScheduler(app).scheduleTaskReminder(task(22)))
        assertEquals(1, alarms().size)
        clearAlarms()
        legacyAlarm(ReminderScheduler.DAILY_DIGEST_REQUEST_CODE, Intent(app, DailyReminderReceiver::class.java).apply {
            action = DailyReminderReceiver.ACTION_TRIGGER_DAILY_DIGEST
        })
        assertTrue(ReminderScheduler(app).scheduleDailyReminder())
        assertEquals(1, alarms().size)
        assertNotNull(shadowOf(alarms().single().operation).savedIntent.data)
    }

    @Test
    fun disablingOrCompletingTaskCancelsItsExistingReminder() {
        val scheduler = TaskAlarmScheduler(app)
        val task = task(25)
        assertTrue(scheduler.scheduleTaskReminder(task))
        assertFalse(scheduler.scheduleTaskReminder(task.copy(reminderMinutes = null)))
        assertTrue(alarms().isEmpty())
        assertTrue(scheduler.scheduleTaskReminder(task))
        assertFalse(scheduler.scheduleTaskReminder(task.copy(completed = true)))
        assertTrue(alarms().isEmpty())
        assertTrue(scheduler.scheduleTaskReminder(task))
        assertFalse(scheduler.scheduleTaskReminder(task.copy(dueDate = "1999-01-01 12:00")))
        assertTrue(alarms().isEmpty())
    }

    @Test
    fun enrolledSchedulingPreservesCustomLeadTime() {
        val note = LectureNote("custom", "Review", 1L, alarmMinutes = 45)
        AlarmScheduler(app).scheduleAllEnrolledClassAlarms(listOf(entry("custom")), 15, customAlarms = listOf(note))
        assertEquals(45, shadowOf(alarms().single().operation).savedIntent.getIntExtra("MINUTES_BEFORE", -1))
    }

    @Test
    fun allSchedulersFallBackWhenExactPermissionIsUnavailable() {
        ShadowAlarmManager.setCanScheduleExactAlarms(false)
        assertTrue(AlarmScheduler(app).scheduleClassAlarm(entry("fallback"), 15))
        assertTrue(AlarmScheduler(app).triggerTestAlarm(entry("fallback"), 5))
        assertTrue(TaskAlarmScheduler(app).scheduleTaskReminder(task(30)))
        assertTrue(ReminderScheduler(app).scheduleDailyReminder())
        assertEquals(4, alarms().size)
    }

    @Test
    fun quietModePreventsRegularSchedulingAndBootRestoration() = runBlocking {
        app.etagStore.setNotificationsEnabled(false)
        assertFalse(AlarmScheduler(app).scheduleClassAlarm(entry("quiet"), 15))
        assertFalse(TaskAlarmScheduler(app).scheduleTaskReminder(task(40)))
        assertFalse(ReminderScheduler(app).scheduleDailyReminder())
        BootReceiver().onReceive(app, Intent(Intent.ACTION_BOOT_COMPLETED))
        assertTrue(alarms().isEmpty())
    }

    @Test
    fun quietDigestCancelsLegacyScheduleWithoutRearmingOrPosting() = runBlocking {
        legacyAlarm(ReminderScheduler.DAILY_DIGEST_REQUEST_CODE, Intent(app, DailyReminderReceiver::class.java).apply {
            action = DailyReminderReceiver.ACTION_TRIGGER_DAILY_DIGEST
        })
        app.etagStore.setNotificationsEnabled(false)
        DailyReminderReceiver().onReceive(app, Intent(app, DailyReminderReceiver::class.java))
        assertTrue(alarms().isEmpty())
        val notifications = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        assertTrue(shadowOf(notifications).allNotifications.isEmpty())
    }

    @Test
    fun quietTaskDeliveryAllowsOnlyExplicitTests() = runBlocking {
        app.etagStore.setNotificationsEnabled(false)
        val intent = Intent(app, TaskReminderReceiver::class.java)
            .putExtra(TaskReminderReceiver.EXTRA_TASK_ID, 50L)
        TaskReminderReceiver().onReceive(app, intent)
        val notifications = shadowOf(app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
        assertTrue(notifications.allNotifications.isEmpty())
        TaskReminderReceiver().onReceive(app, intent.putExtra("IS_TEST", true))
        assertEquals(1, notifications.allNotifications.size)
    }

    @Test
    fun notificationPermissionChoicesPersistAcrossContextAccess() {
        app.getSharedPreferences("mustime_notification_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        assertFalse(NotificationPermissionHelper.hasPromptedNotificationPermission(app))
        assertFalse(NotificationPermissionHelper.isTaskBannerDismissed(app))
        NotificationPermissionHelper.setPromptedNotificationPermission(app)
        NotificationPermissionHelper.setTaskBannerDismissed(app)
        assertTrue(NotificationPermissionHelper.hasPromptedNotificationPermission(app.applicationContext))
        assertTrue(NotificationPermissionHelper.isTaskBannerDismissed(app.applicationContext))
    }

    @Test
    fun taskNotificationActionsUseIndependentIntentIdentities() {
        NotificationHelper.showTaskReminderNotification(app, 60L, "Lab", "CSC", "Tomorrow", "High")
        val manager = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = shadowOf(manager).allNotifications.single()
        val actions = notification.actions.map { shadowOf(it.actionIntent).savedIntent.data }
        assertEquals(actions.size, actions.toSet().size)
        assertFalse(actions.contains(shadowOf(notification.contentIntent).savedIntent.data))
    }

    @Test
    fun dailyDigestReceiverIsRegisteredInTheManifest() {
        val name = android.content.ComponentName(app, DailyReminderReceiver::class.java)
        @Suppress("DEPRECATION")
        val info = app.packageManager.getReceiverInfo(name, 0)
        assertFalse(info.exported)
    }

    private fun entry(key: String) = TimetableEntry(
        natural_key = key, program_group = "BCS I", day = "Monday", time_slot = "09:00 - 11:00",
        start_time = "09:00", end_time = "11:00", course_code = "CSC", course_title = "Lecture",
        session_type = "Lecture", lecturer = "Lecturer", room = "Hall"
    )

    private fun task(id: Long) = Assignment(
        id = id, title = "Lab", courseCode = "CSC", dueDate = "Tomorrow • 18:00",
        reminderMinutes = 30, priority = "High", completed = false
    )

    private fun alarms() = shadowOf(manager).scheduledAlarms

    private fun clearAlarms() {
        alarms().toList().forEach { manager.cancel(it.operation); it.operation.cancel() }
    }

    private fun legacyAlarm(code: Int, intent: Intent) {
        val token = PendingIntent.getBroadcast(app, code, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        manager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 60_000L, token)
    }
}
