package com.mustime

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.mustime.core.database.AppDatabase
import com.mustime.features.onboarding.OnboardingDataStore
import com.mustime.features.timetable.data.ETagStore
import com.mustime.features.timetable.data.TimetableRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OnboardingDataStoreTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: Context
    private lateinit var onboardingDataStore: OnboardingDataStore
    private lateinit var repository: TimetableRepository
    private lateinit var db: AppDatabase
    private lateinit var etagStore: ETagStore

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext<Context>().applicationContext
        onboardingDataStore = OnboardingDataStore(context)
        etagStore = ETagStore(context)
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = TimetableRepository(
            dao = db.timetableDao(),
            eventDao = db.customEventDao(),
            assignmentDao = db.assignmentDao(),
            etagStore = etagStore,
            onboardingDataStore = onboardingDataStore
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
    }

    @Test
    fun testFirstLaunchReturnsFalse() = runTest(testDispatcher) {
        // Reset state to simulate clean install
        onboardingDataStore.setOnboardingCompleted(false)
        val isCompleted = onboardingDataStore.isOnboardingCompleted.first()
        assertFalse("On first launch, onboarding completion state must be false so welcome screen shows", isCompleted)
    }

    @Test
    fun testCompletingOnboardingPersistsTrue() = runTest(testDispatcher) {
        onboardingDataStore.setOnboardingCompleted(true)
        val isCompleted = onboardingDataStore.isOnboardingCompleted.first()
        assertTrue("After completing onboarding, DataStore must persist true", isCompleted)
    }

    @Test
    fun testRepositoryDelegatesToDataStore() = runTest(testDispatcher) {
        repository.setOnboardingCompleted(false)
        assertFalse("Repository onboardingCompletedPref must emit false initially", repository.onboardingCompletedPref.first())

        repository.setOnboardingCompleted(true)
        assertTrue("Repository onboardingCompletedPref must emit true after completion", repository.onboardingCompletedPref.first())
        assertTrue("checkIsOnboardingCompleted must return true", repository.checkIsOnboardingCompleted())
    }

    @Test
    fun testResettingOnboardingRestoresFirstLaunchState() = runTest(testDispatcher) {
        repository.setOnboardingCompleted(true)
        assertTrue(repository.onboardingCompletedPref.first())

        onboardingDataStore.resetOnboarding()
        assertFalse("Resetting onboarding must restore false state to display welcome screen again", repository.onboardingCompletedPref.first())
    }

    @Test
    fun testProgrammePersistenceDoesNotDefaultToMbrI() = runTest(testDispatcher) {
        // User selects a specific non-MBR programme like BCS II
        val selectedProg = "BCS II"
        repository.setProgrammePref(selectedProg)

        val retrievedFromRepo = repository.programmePref.first()
        org.junit.Assert.assertEquals(selectedProg, retrievedFromRepo)
        org.junit.Assert.assertEquals(selectedProg, repository.getInitialProgramme())

        // Ensure ETagStore and DataStore both reflect BCS II and not MBR I
        val dataStoreProg = onboardingDataStore.programmeFlow.first()
        org.junit.Assert.assertEquals(selectedProg, dataStoreProg)
    }
}
