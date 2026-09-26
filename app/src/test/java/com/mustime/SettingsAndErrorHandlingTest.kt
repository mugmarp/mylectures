package com.mustime

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.mustime.core.alarm.TaskAlarmScheduler
import com.mustime.core.database.AppDatabase
import com.mustime.features.settings.SettingsViewModel
import com.mustime.features.timetable.data.DataLoader
import com.mustime.features.timetable.data.ETagStore
import com.mustime.features.timetable.data.TimetableRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsAndErrorHandlingTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var etagStore: ETagStore
    private lateinit var repository: TimetableRepository
    private lateinit var dataLoader: DataLoader
    private lateinit var alarmScheduler: TaskAlarmScheduler
    private lateinit var settingsViewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext<Context>().applicationContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        etagStore = ETagStore(context)
        dataLoader = DataLoader(context, db)
        alarmScheduler = TaskAlarmScheduler(context)
        repository = TimetableRepository(
            dao = db.timetableDao(),
            eventDao = db.customEventDao(),
            assignmentDao = db.assignmentDao(),
            etagStore = etagStore
        )
        settingsViewModel = SettingsViewModel(repository, dataLoader, alarmScheduler, testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
    }

    @Test
    fun testDarkModeSettingPersistence() = runTest(testDispatcher) {
        settingsViewModel.setDarkMode(true)
        testScheduler.advanceUntilIdle()
        val isDark = repository.themeDarkPref.first()
        assertTrue("Dark mode should be persisted as true", isDark)

        settingsViewModel.setDarkMode(false)
        testScheduler.advanceUntilIdle()
        val isLight = repository.themeDarkPref.first()
        assertFalse("Dark mode should be persisted as false", isLight)
    }

    @Test
    fun testAccentColorSettingPersistence() = runTest(testDispatcher) {
        settingsViewModel.setSelectedAccent(2)
        testScheduler.advanceUntilIdle()
        val accent = repository.themeAccentPref.first()
        assertEquals("Accent index should be persisted as 2", 2, accent)
    }

    @Test
    fun testNotificationsToggle() = runTest(testDispatcher) {
        settingsViewModel.setNotificationsEnabled(false)
        testScheduler.advanceUntilIdle()
        val enabled = repository.notificationsEnabledPref.first()
        assertFalse("Notifications should be saved as disabled", enabled)

        settingsViewModel.setNotificationsEnabled(true)
        testScheduler.advanceUntilIdle()
        val reEnabled = repository.notificationsEnabledPref.first()
        assertTrue("Notifications should be saved as enabled", reEnabled)
    }

    @Test
    fun testSyncTimetableSuccessFeedback() = runTest(testDispatcher) {
        settingsViewModel.syncTimetable()

        for (i in 0 until 50) {
            testScheduler.advanceUntilIdle()
            if (settingsViewModel.uiState.value.successMessage != null || settingsViewModel.uiState.value.errorMessage != null) {
                break
            }
            Thread.sleep(100)
        }

        val state = settingsViewModel.uiState.value
        assertNotNull("Sync should provide success feedback", state.successMessage)
        assertTrue(state.successMessage!!.contains("synchronized", ignoreCase = true))
        assertNull("There should be no error message on successful sync", state.errorMessage)
    }

    @Test
    fun testResetAllNotes() = runTest(testDispatcher) {
        var completed = false
        settingsViewModel.resetAllNotes {
            completed = true
        }

        for (i in 0 until 50) {
            testScheduler.advanceUntilIdle()
            if (completed) {
                break
            }
            Thread.sleep(100)
        }

        assertTrue("Completion callback should be invoked", completed)
        val state = settingsViewModel.uiState.value
        assertNotNull(state.successMessage)
    }
}
