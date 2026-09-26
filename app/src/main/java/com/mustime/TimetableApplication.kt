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
            AppDatabase.MIGRATION_5_6
        )
        .fallbackToDestructiveMigration()
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
        
        // Initial load
        applicationScope.launch {
            dataLoader.loadInitialDataIfNeeded()
            
            // Authenticate anonymously and start sync if Firebase is available
            try {
                val auth = FirebaseFactory.auth
                if (auth.currentUser == null) {
                    auth.signInAnonymously()
                        .addOnSuccessListener { authResult ->
                            authResult.user?.uid?.let { uid ->
                                syncRepository.startAllSync(uid, applicationScope)
                            }
                        }
                        .addOnFailureListener { exception ->
                            android.util.Log.i("TimetableApplication", "Firebase anonymous auth unavailable (offline or credentials pending in Firebase Console): ${exception.message}")
                        }
                } else {
                    syncRepository.startAllSync(auth.currentUser!!.uid, applicationScope)
                }
            } catch (e: Throwable) {
                // Firebase not initialized in local test / offline environment
                android.util.Log.i("TimetableApplication", "Firebase not available: ${e.message}")
            }
        }
    }
}
