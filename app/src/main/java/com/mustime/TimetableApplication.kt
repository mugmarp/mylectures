package com.mustime

import android.app.Application
import androidx.room.Room
import com.mustime.core.database.AppDatabase
import com.mustime.core.network.FirebaseFactory
import com.mustime.core.sync.SyncRepository
import com.mustime.features.onboarding.OnboardingDataStore
import com.mustime.features.timetable.data.DataLoader
import com.mustime.features.timetable.data.ETagStore
import com.mustime.features.timetable.data.TimetableRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TimetableApplication : Application() {
    lateinit var database: AppDatabase
    lateinit var etagStore: ETagStore
    lateinit var onboardingDataStore: OnboardingDataStore
    lateinit var repository: TimetableRepository
    lateinit var syncRepository: SyncRepository
    lateinit var dataLoader: DataLoader
    lateinit var feedbackManager: com.mustime.features.feedback.FeedbackManager
    lateinit var analyticsManager: com.mustime.core.analytics.AnalyticsManager
    
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
        com.mustime.core.notification.NotificationHelper.createNotificationChannels(this)
        // Apply saved sound profile so notification channels reflect the user's choice
        com.mustime.core.notification.NotificationHelper.applySoundProfile(
            this, com.mustime.features.timetable.data.ETagStore(this).getAlarmSound()
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
            AppDatabase.MIGRATION_9_10
        )
        // Strictly protect all production coursework migrations (v2-v7).
        // Only allow destructive reset for pre-release prototype version 1.
        .fallbackToDestructiveMigrationFrom(1)
        .build()
        
        etagStore = ETagStore(this)
        onboardingDataStore = OnboardingDataStore(this)
        dataLoader = DataLoader(this, database)
        
        syncRepository = SyncRepository(database)
        repository = TimetableRepository(
            dao = database.timetableDao(),
            eventDao = database.customEventDao(),
            assignmentDao = database.assignmentDao(),
            etagStore = etagStore,
            onboardingDataStore = onboardingDataStore,
            syncRepository = syncRepository
        )

        feedbackManager = com.mustime.features.feedback.FeedbackManager(this, etagStore)
        analyticsManager = com.mustime.core.analytics.AnalyticsManager(this)
        
        schedulePeriodicTimetableSync()

        // Initial load, sync, and telemetry heartbeat
        applicationScope.launch {
            dataLoader.loadInitialDataIfNeeded()
            syncRepository.startTimetableSync(this)
            feedbackManager.flushPendingSubmissions()
            val prog = etagStore.getSavedProgramme() ?: ""
            analyticsManager.recordAppLaunch(prog, this)
            if (etagStore.isOnboardingCompleted()) {
                com.mustime.core.alarm.ReminderScheduler(this@TimetableApplication).scheduleDailyReminder()
            }
        }
    }

    private fun schedulePeriodicTimetableSync() {
        try {
            val constraints = androidx.work.Constraints.Builder()
                .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                .build()

            val syncRequest = androidx.work.PeriodicWorkRequestBuilder<com.mustime.core.sync.TimetableSyncWorker>(
                12, java.util.concurrent.TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()

            androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                com.mustime.core.sync.TimetableSyncWorker.WORK_NAME,
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
            android.util.Log.d("TimetableApplication", "Periodic timetable sync worker enqueued (12h interval)")
        } catch (e: Exception) {
            android.util.Log.e("TimetableApplication", "Failed to schedule TimetableSyncWorker: ${e.message}")
        }
    }
}
