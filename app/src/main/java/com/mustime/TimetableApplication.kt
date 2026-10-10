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

        // Initialize Android NotificationChannels for academic task reminders and class alarms
        com.mustime.core.notification.NotificationHelper.createNotificationChannels(this)
        
        database = Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "mustime_db"
        )
        .addMigrations(
            AppDatabase.MIGRATION_2_3,
            AppDatabase.MIGRATION_3_4,
            AppDatabase.MIGRATION_4_5,
            AppDatabase.MIGRATION_5_6,
            AppDatabase.MIGRATION_6_7
        )
        // Strictly protect all production coursework migrations (v2-v7).
        // Only allow destructive reset for pre-release prototype version 1.
        .fallbackToDestructiveMigrationFrom(1)
        .build()
        
        etagStore = ETagStore(this)
        onboardingDataStore = OnboardingDataStore(this)
        dataLoader = DataLoader(this, database)
        
        repository = TimetableRepository(
            dao = database.timetableDao(),
            eventDao = database.customEventDao(),
            assignmentDao = database.assignmentDao(),
            etagStore = etagStore,
            onboardingDataStore = onboardingDataStore
        )

        syncRepository = SyncRepository(database)
        feedbackManager = com.mustime.features.feedback.FeedbackManager(this, etagStore)
        analyticsManager = com.mustime.core.analytics.AnalyticsManager(this)
        
        // Initial load and telemetry heartbeat
        applicationScope.launch {
            dataLoader.loadInitialDataIfNeeded()
            val prog = etagStore.getSavedProgramme() ?: ""
            analyticsManager.recordAppLaunch(prog, this)
        }
    }
}
