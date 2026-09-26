package com.mustime

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.mustime.core.alarm.TaskDateTimeParser
import com.mustime.core.database.AppDatabase
import com.mustime.core.util.TimeUtil
import com.mustime.features.onboarding.OnboardingDataStore
import com.mustime.features.timetable.data.ETagStore
import com.mustime.features.timetable.data.TimetableRepository
import com.mustime.features.timetable.domain.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.util.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TimetableQuickAddTest {

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
    fun testTaskCategoryEnumAttributes() {
        assertEquals("Assignment", TaskCategory.ASSIGNMENT.displayName)
        assertEquals("#7C3AED", TaskCategory.ASSIGNMENT.colorHex)

        assertEquals("Project", TaskCategory.PROJECT.displayName)
        assertEquals("#2563EB", TaskCategory.PROJECT.colorHex)

        assertEquals("Lab Report", TaskCategory.LAB_REPORT.displayName)
        assertEquals("Quiz / Exam", TaskCategory.EXAM_PREP.displayName)

        assertEquals(TaskCategory.ASSIGNMENT, TaskCategory.fromName("Assignment"))
        assertEquals(TaskCategory.PROJECT, TaskCategory.fromName("Project"))
        assertEquals(TaskCategory.ASSIGNMENT, TaskCategory.fromName("unknown"))
    }

    @Test
    fun testActivityCategoryEnumAttributes() {
        assertEquals("Study & Revision", ActivityCategory.STUDY.displayName)
        assertEquals("#2563EB", ActivityCategory.STUDY.colorHex)
        assertEquals(ActivityCategory.LAB, ActivityCategory.fromName("Lab & Practical"))
        assertEquals(ActivityCategory.SPORTS, ActivityCategory.fromName("Sports & Fitness"))
    }

    @Test
    fun testQuickAddActivityPersistence() = runBlocking {
        val event = CustomEvent(
            title = "Study Session",
            dayOfWeek = "Tuesday",
            startTime = "14:00",
            endTime = "16:00",
            location = "Library",
            category = "Study & Revision",
            colorTag = "#2563EB",
            alarmMinutes = 15
        )
        val eventId = repository.saveCustomEvent(event)
        assertTrue(eventId > 0)

        val events = repository.getCustomEvents().first()
        assertEquals(1, events.size)
        val saved = events.first()
        assertEquals("Study Session", saved.title)
        assertEquals("Tuesday", saved.dayOfWeek)
        assertEquals(15, saved.alarmMinutes)

        repository.deleteCustomEvent(eventId)
        val afterDelete = repository.getCustomEvents().first()
        assertTrue(afterDelete.isEmpty())
    }

    @Test
    fun testQuickAddTaskPersistenceAndCompletion() = runBlocking {
        val task = Assignment(
            title = "[Assignment] Pharmacology Lab 2",
            courseCode = "PHA3102",
            dueDate = "Tuesday • 17:00",
            priority = "High",
            reminderMinutes = 30,
            notes = "Submit via MUCMS portal",
            completed = false
        )
        val taskId = repository.saveAssignment(task)
        assertTrue(taskId > 0)

        val tasks = repository.getAssignments().first()
        assertEquals(1, tasks.size)
        val saved = tasks.first()
        assertEquals("[Assignment] Pharmacology Lab 2", saved.title)
        assertEquals("PHA3102", saved.courseCode)
        assertEquals("Tuesday • 17:00", saved.dueDate)
        assertEquals("High", saved.priority)
        assertFalse(saved.completed)

        repository.updateAssignmentCompletion(taskId, true)
        val updatedList = repository.getAssignments().first()
        assertTrue(updatedList.first().completed)

        repository.deleteAssignment(taskId)
        val afterDelete = repository.getAssignments().first()
        assertTrue(afterDelete.isEmpty())
    }

    @Test
    fun testTaskDayMatchingLogic() {
        val tuesdayTask = Assignment(
            title = "Physiology Assignment",
            courseCode = "PHA3102",
            dueDate = "Tuesday • 17:00",
            priority = "Medium",
            completed = false
        )

        // Helper matching logic as in TimetableScreen
        fun matchesDay(task: Assignment, dayName: String): Boolean {
            val lowerDue = task.dueDate.lowercase(Locale.getDefault())
            val lowerDay = dayName.lowercase(Locale.getDefault())
            val shortDay = lowerDay.take(3)

            if (lowerDue.contains(lowerDay) || lowerDue.contains(shortDay)) {
                return true
            }
            val todayName = TimeUtil.todayName().lowercase(Locale.getDefault())
            if (lowerDue.contains("today") && (todayName == lowerDay || todayName.startsWith(shortDay))) {
                return true
            }
            if (lowerDue.contains("tomorrow")) {
                val tomCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
                val tomDay = SimpleDateFormat("EEEE", Locale.getDefault()).format(tomCal.time).lowercase(Locale.getDefault())
                if (tomDay == lowerDay || tomDay.startsWith(shortDay)) {
                    return true
                }
            }
            val millis = TaskDateTimeParser.calculateDueMillis(task.dueDate)
            if (millis != null) {
                val cal = Calendar.getInstance().apply { timeInMillis = millis }
                val targetDay = SimpleDateFormat("EEEE", Locale.getDefault()).format(cal.time).lowercase(Locale.getDefault())
                return targetDay == lowerDay || targetDay.startsWith(shortDay)
            }
            return false
        }

        assertTrue(matchesDay(tuesdayTask, "Tuesday"))
        assertTrue(matchesDay(tuesdayTask, "Tue"))
        assertFalse(matchesDay(tuesdayTask, "Friday"))

        val todayTask = Assignment(
            title = "Lab prep",
            courseCode = "ANA1101",
            dueDate = "Today • 15:00",
            priority = "High",
            completed = false
        )
        assertTrue(matchesDay(todayTask, TimeUtil.todayName()))
    }
}
