package com.lectures

import android.app.Application
import androidx.room.Room
import com.lectures.core.database.AppDatabase
import com.lectures.core.network.FirebaseFactory
import com.lectures.core.sync.SyncRepository
import com.lectures.features.onboarding.OnboardingDataStore
import com.lectures.features.timetable.data.DataLoader
import com.lectures.features.timetable.data.ETagStore
import com.lectures.features.timetable.data.TimetableRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class TimetableApplication : Application() {
    lateinit var database: AppDatabase
    lateinit var etagStore: ETagStore
    lateinit var onboardingDataStore: OnboardingDataStore
    lateinit var repository: TimetableRepository
    lateinit var syncRepository: SyncRepository
    lateinit var dataLoader: DataLoader
    lateinit var feedbackManager: com.lectures.features.feedback.FeedbackManager
    lateinit var analyticsManager: com.lectures.core.analytics.AnalyticsManager
    
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        try {
            com.google.firebase.FirebaseApp.initializeApp(this)
        } catch (_: Exception) { }

        // Install App Check so every Firestore request carries a valid attestation token.
        // Play Integrity is the provider; the debug provider is used automatically in
        // debug builds when FIREBASE_APPCHECK_DEBUG_TOKEN is configured.
        try {
            com.google.firebase.appcheck.FirebaseAppCheck.getInstance()
                .installAppCheckProviderFactory(
                    com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory.getInstance()
                )
        } catch (_: Exception) { }

        // Initialize Android NotificationChannels for academic task reminders and class alarms
        com.lectures.core.notification.NotificationHelper.createNotificationChannels(this)
        // Apply saved sound profile so notification channels reflect the user's choice
        com.lectures.core.notification.NotificationHelper.applySoundProfile(
            this, com.lectures.features.timetable.data.ETagStore(this).getAlarmSound()
        )
        
        database = Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "mustime_db"
        )
        // First install: copy the prepackaged asset (1304 rows already present)
        // instead of parsing JSON and inserting 1304 rows on the device.
        // Only used when mustime_db does not yet exist; existing installs go
        // through the migrations below as before.
        .createFromAsset(AppDatabase.PREPOPULATED_DB_NAME)
        .addMigrations(
            AppDatabase.MIGRATION_2_3,
            AppDatabase.MIGRATION_3_4,
            AppDatabase.MIGRATION_4_5,
            AppDatabase.MIGRATION_5_6,
            AppDatabase.MIGRATION_6_7,
            AppDatabase.MIGRATION_7_8,
            AppDatabase.MIGRATION_8_9,
            AppDatabase.MIGRATION_9_10,
            AppDatabase.MIGRATION_10_11,
            AppDatabase.MIGRATION_11_12
        )
        // Strictly protect all production coursework migrations (v2-v7).
        // Only allow destructive reset for pre-release prototype version 1.
        .fallbackToDestructiveMigrationFrom(1)
        .build()
        
        etagStore = ETagStore(this)
        onboardingDataStore = OnboardingDataStore(this)
        dataLoader = DataLoader(this, database)
        
        syncRepository = SyncRepository(database, beforeUserDataPurge = { tasks, notes ->
            val taskScheduler = com.lectures.core.alarm.TaskAlarmScheduler(this)
            tasks.forEach { taskScheduler.cancelTaskReminder(it.id) }
            val classScheduler = com.lectures.core.alarm.AlarmScheduler(this)
            notes.forEach { classScheduler.cancelClassAlarm(it.naturalKey) }
            androidx.core.app.NotificationManagerCompat.from(this).cancelAll()
            com.lectures.core.notification.InAppNotificationStore.getInstance(this).clearAll()
        })
        repository = TimetableRepository(
            dao = database.timetableDao(),
            eventDao = database.customEventDao(),
            assignmentDao = database.assignmentDao(),
            etagStore = etagStore,
            onboardingDataStore = onboardingDataStore,
            syncRepository = syncRepository
        )

        feedbackManager = com.lectures.features.feedback.FeedbackManager(this, etagStore)
        analyticsManager = com.lectures.core.analytics.AnalyticsManager(this)
        
        schedulePeriodicTimetableSync()

        // Initial load, sync, and telemetry heartbeat
        applicationScope.launch {
            dataLoader.loadInitialDataIfNeeded()
            syncRepository.startTimetableSync(applicationScope)
            feedbackManager.flushPendingSubmissions()
            val prog = etagStore.getSavedProgramme() ?: ""
            analyticsManager.recordAppLaunch(prog, this)
            if (repository.checkIsOnboardingCompleted() && etagStore.isNotificationsEnabled()) {
                com.lectures.core.alarm.ReminderScheduler(this@TimetableApplication).scheduleDailyReminder()
                if (prog.isNotEmpty() && etagStore.isNotificationsEnabled()) {
                    val enrolledClasses = repository.getLocalSchedule(prog).firstOrNull() ?: emptyList()
                    if (enrolledClasses.isNotEmpty()) {
                        val leadMinutes = etagStore.getClassAlarmLeadMinutes()
                        com.lectures.core.alarm.AlarmScheduler(this@TimetableApplication).scheduleAllEnrolledClassAlarms(enrolledClasses, leadMinutes, customAlarms = repository.getNotesWithAlarms())
                    }
                }
            }
        }
        applicationScope.launch {
            com.lectures.core.auth.AuthRepository.getInstance().currentUser.collect { user ->
                if (user == null) syncRepository.stopUserSync()
                else syncRepository.startAllSync(user.uid, applicationScope)
            }
        }
    }

    private fun schedulePeriodicTimetableSync() {
        try {
            val constraints = androidx.work.Constraints.Builder()
                .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                .build()

            val syncRequest = androidx.work.PeriodicWorkRequestBuilder<com.lectures.core.sync.TimetableSyncWorker>(
                12, java.util.concurrent.TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()

            androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                com.lectures.core.sync.TimetableSyncWorker.WORK_NAME,
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
            android.util.Log.d("TimetableApplication", "Periodic timetable sync worker enqueued (12h interval)")
        } catch (e: Exception) {
            android.util.Log.e("TimetableApplication", "Failed to schedule TimetableSyncWorker: ${e.message}")
        }
    }
}
