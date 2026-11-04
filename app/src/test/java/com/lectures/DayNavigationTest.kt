package com.lectures

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.lectures.core.database.AppDatabase
import com.lectures.core.util.TimeUtil
import com.lectures.features.calendar.CalendarViewModel
import com.lectures.features.timetable.data.ETagStore
import com.lectures.features.timetable.data.TimetableRepository
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
@Config(sdk = [36])
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

    @Test
    fun testNavigateToDateCrossMonthAndYear() {
        // Navigate across year and month boundary (e.g. jumping to Semester 2 start date)
        calendarViewModel.navigateToDate(2027, java.util.Calendar.JANUARY, 18)

        val state = calendarViewModel.uiState.value
        assertEquals(2027, state.currentYear)
        assertEquals(java.util.Calendar.JANUARY, state.currentMonth)
        assertEquals(18, state.selectedDay)
        assertEquals(2, state.academicContext.semesterNumber)
        assertEquals("Monday", state.selectedDayOfWeek)
        assertTrue(state.academicContext.isInstructionalDay)
    }

    @Test
    fun testAcademicCalendarEngineMilestoneLookup() {
        val milestones = com.lectures.features.calendar.AcademicCalendarEngine.getMilestonesForAcademicYear()
        assertTrue(milestones.isNotEmpty())

        // Check Mid-Semester Assessment date: Oct 20, 2026
        val midsemContext = com.lectures.features.calendar.AcademicCalendarEngine.getAcademicContextForDate(
            2026, java.util.Calendar.OCTOBER, 20
        )
        assertEquals(com.lectures.features.calendar.AcademicPhase.MID_SEM_TESTS, midsemContext.phase)
        assertNotNull(midsemContext.primaryMilestone)
        assertEquals("sem1_midsem", midsemContext.primaryMilestone?.id)

        // Check Christmas / Recess date: Dec 28, 2026
        val recessContext = com.lectures.features.calendar.AcademicCalendarEngine.getAcademicContextForDate(
            2026, java.util.Calendar.DECEMBER, 28
        )
        assertEquals(com.lectures.features.calendar.AcademicPhase.RECESS, recessContext.phase)
        assertTrue(!recessContext.isInstructionalDay)
    }
}
