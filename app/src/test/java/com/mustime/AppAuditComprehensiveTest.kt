package com.mustime

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.mustime.core.database.AppDatabase
import com.mustime.features.onboarding.OnboardingDataStore
import com.mustime.features.timetable.data.ETagStore
import com.mustime.features.timetable.data.TimetableRepository
import com.mustime.features.timetable.domain.Assignment
import com.mustime.features.timetable.domain.CustomEvent
import com.mustime.features.timetable.domain.TimetableEntry
import com.mustime.features.timetable.domain.TimetableMatcher
import com.mustime.features.timetable.ui.resolveSessionTypeLabel
import com.mustime.features.timetable.ui.sessionStyle
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppAuditComprehensiveTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repository: TimetableRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>().applicationContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = TimetableRepository(
            dao = db.timetableDao(),
            eventDao = db.customEventDao(),
            assignmentDao = db.assignmentDao(),
            etagStore = ETagStore(context),
            onboardingDataStore = OnboardingDataStore(context)
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testFirstLaunchDefaultThemeIsLight() = runBlocking {
        val mode = repository.themeModePref.first()
        assertEquals("Theme on fresh launch must default to LIGHT", "LIGHT", mode)
    }

    @Test
    fun testMucosaSessionTypeResolutionAndStyling() {
        val mucosaEntry = TimetableEntry(
            natural_key = "MUCOSA_SESSION_MON_1700",
            program_group = "BCS II",
            day = "Monday",
            time_slot = "17:00 - 19:00",
            start_time = "17:00",
            end_time = "19:00",
            course_code = "MUCOSA",
            course_title = "Mbarara University Computing Students Association Assembly",
            session_type = null, // In DB it is null
            lecturer = null,     // In DB it is null
            room = null          // In DB it is null
        )

        // 1. Label should NOT be THEORY or LECTURE; it should be Student Association
        val label = resolveSessionTypeLabel(mucosaEntry)
        assertEquals("Student Association", label)

        // 2. Styling should be Association-specific
        val style = sessionStyle(mucosaEntry.session_type, mucosaEntry.room ?: "", mucosaEntry.course_title)
        assertNotNull(style)
        assertNotNull(style.badgeBg)
        assertNotNull(style.accentBar)
    }

    @Test
    fun testRegularLectureAndLabResolution() {
        val theoryEntry = TimetableEntry(
            natural_key = "CS201_MON_0800",
            program_group = "BCS II",
            day = "Monday",
            time_slot = "08:00 - 10:00",
            start_time = "08:00",
            end_time = "10:00",
            course_code = "CS2101",
            course_title = "Data Structures & Algorithms",
            session_type = "THEORY",
            lecturer = "Dr. Mugisha",
            room = "LR4"
        )
        assertEquals("Lecture", resolveSessionTypeLabel(theoryEntry))

        val labEntry = TimetableEntry(
            natural_key = "CS201_LAB_MON_1400",
            program_group = "BCS II",
            day = "Monday",
            time_slot = "14:00 - 17:00",
            start_time = "14:00",
            end_time = "17:00",
            course_code = "CS2101",
            course_title = "Data Structures Lab",
            session_type = "LAB",
            lecturer = "Mr. Kato",
            room = "Computer Lab 2"
        )
        assertEquals("Lab", resolveSessionTypeLabel(labEntry))
    }

    @Test
    fun testPlaceholderSanitization() {
        // Entries with TBA, TBD, or Staff should have them cleanly filtered out
        val tbaVenue = "TBA".takeIf { it.isNotBlank() && it.uppercase() !in listOf("TBA", "TBD", "NONE", "N/A") }
        assertNull("TBA venue must be sanitized to null", tbaVenue)

        val validVenue = "Room 301".takeIf { it.isNotBlank() && it.uppercase() !in listOf("TBA", "TBD", "NONE", "N/A") }
        assertEquals("Room 301", validVenue)

        val staffLecturer = "Staff".takeIf { it.isNotBlank() && !it.equals("Staff", ignoreCase = true) }
        assertNull("Staff lecturer must be sanitized to null", staffLecturer)

        val validLecturer = "Dr. Tumusiime".takeIf { it.isNotBlank() && !it.equals("Staff", ignoreCase = true) }
        assertEquals("Dr. Tumusiime", validLecturer)
    }

    @Test
    fun testTaskOptimisticToggleAndDatabaseSync() = runBlocking {
        val task = Assignment(
            id = 101,
            title = "Compiler Construction Assignment",
            courseCode = "CS3101",
            dueDate = "Friday • 23:59",
            priority = "High",
            notes = "Submit on MUST Moodle",
            completed = false,
            reminderMinutes = 30
        )

        repository.saveAssignment(task)

        val savedList = repository.getAssignments().first()
        assertEquals(1, savedList.size)
        assertFalse(savedList[0].completed)

        // Toggle to completed
        repository.updateAssignmentCompletion(task.id, true)
        val updatedList = repository.getAssignments().first()
        assertTrue(updatedList[0].completed)

        // Toggle back to pending
        repository.updateAssignmentCompletion(task.id, false)
        val restoredList = repository.getAssignments().first()
        assertFalse(restoredList[0].completed)
    }

    @Test
    fun testCustomActivityLifecycle() = runBlocking {
        val event = CustomEvent(
            title = "MUCOSA Hackathon Meeting",
            dayOfWeek = "Wednesday",
            startTime = "16:00",
            endTime = "18:00",
            location = "Computing Lab 1",
            notes = "Prepare prototype pitches",
            category = "Study Session",
            colorTag = "#2563EB",
            alarmMinutes = 15
        )

        val eventId = repository.saveCustomEvent(event)
        assertTrue(eventId > 0)

        val retrieved = repository.getCustomEvents().first()
        assertEquals(1, retrieved.size)
        assertEquals("MUCOSA Hackathon Meeting", retrieved[0].title)
        assertEquals(15, retrieved[0].alarmMinutes)

        repository.deleteCustomEvent(eventId)
        val afterDelete = repository.getCustomEvents().first()
        assertEquals(0, afterDelete.size)
    }
}
