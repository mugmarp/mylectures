package com.mustime

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.mustime.core.database.AppDatabase
import com.mustime.core.util.TimeUtil
import com.mustime.features.calendar.CalendarViewModel
import com.mustime.features.timetable.data.ETagStore
import com.mustime.features.timetable.data.TimetableRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DayNavigationTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repository: TimetableRepository
    private lateinit var calendarViewModel: CalendarViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext<Context>().applicationContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val etagStore = ETagStore(context)
        repository = TimetableRepository(
            dao = db.timetableDao(),
            eventDao = db.customEventDao(),
            assignmentDao = db.assignmentDao(),
            etagStore = etagStore
        )
        calendarViewModel = CalendarViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
    }

    @Test
    fun testTimeUtilDaysListContainsSevenDays() {
        val days = TimeUtil.DAYS
        assertEquals(7, days.size)
        assertEquals("Monday", days[0])
        assertEquals("Sunday", days[6])
    }

    @Test
    fun testCalendarDayNavigationNextAndPrevious() {
        calendarViewModel.selectDay(15)
        assertEquals(15, calendarViewModel.uiState.value.selectedDay)

        calendarViewModel.nextDay()
        assertEquals(16, calendarViewModel.uiState.value.selectedDay)

        calendarViewModel.previousDay()
        assertEquals(15, calendarViewModel.uiState.value.selectedDay)
    }

    @Test
    fun testCalendarMonthBoundaryNavigation() {
        calendarViewModel.selectDay(1)
        calendarViewModel.previousDay()
        // Should navigate to last day of previous month
        assertTrue(calendarViewModel.uiState.value.selectedDay >= 28)

        val daysInCurrent = calendarViewModel.uiState.value.daysInMonth
        calendarViewModel.selectDay(daysInCurrent)
        calendarViewModel.nextDay()
        // Should navigate to day 1 of next month
        assertEquals(1, calendarViewModel.uiState.value.selectedDay)
    }
}
