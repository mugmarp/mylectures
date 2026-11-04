package com.lectures.features.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lectures.core.alarm.AlarmScheduler
import com.lectures.core.alarm.TaskAlarmScheduler
import com.lectures.core.alarm.TaskDateTimeParser
import com.lectures.core.util.TimeUtil
import com.lectures.features.timetable.data.TimetableRepository
import com.lectures.features.timetable.domain.Assignment
import com.lectures.features.timetable.domain.CustomEvent
import com.lectures.features.timetable.domain.TimetableEntry
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class CalendarTabMode {
    MONTH_PLANNER,
    SEMESTER_MILESTONES
}

enum class CalendarFilter(val label: String) {
    ALL("All Events"),
    LECTURES("Lectures"),
    ACTIVITIES("Activities"),
    TASKS("Deadlines")
}

data class DayWorkload(
    val totalLectureMinutes: Int = 0,
    val lectureCount: Int = 0,
    val deadlineCount: Int = 0,
    val activityCount: Int = 0,
    val primaryMilestone: AcademicMilestone? = null
) {
    val totalHoursText: String
        get() {
            val hours = totalLectureMinutes / 60
            val mins = totalLectureMinutes % 60
            return when {
                hours > 0 && mins > 0 -> "${hours}h ${mins}m"
                hours > 0 -> "${hours}h"
                mins > 0 -> "${mins}m"
                else -> "0h"
            }
        }
}

data class CalendarUiState(
    val isLoading: Boolean = true,
    val currentProgramme: String = "",
    val entries: List<TimetableEntry> = emptyList(),
    val assignments: List<Assignment> = emptyList(),
    val customEvents: List<CustomEvent> = emptyList(),
    val selectedFilter: CalendarFilter = CalendarFilter.ALL,
    val activeTabMode: CalendarTabMode = CalendarTabMode.MONTH_PLANNER,
    val yearMonthText: String = "",
    val currentYear: Int = 2026,
    val currentMonth: Int = 0,
    val daysInMonth: Int = 30,
    val firstDayOfWeekOffset: Int = 0,
    val selectedDay: Int = 1,
    val selectedDayOfWeek: String = "Monday",
    val todayDay: Int? = null,
    val academicContext: DayAcademicContext = AcademicCalendarEngine.getAcademicContextForDate(2026, 0, 1),
    val monthAcademicContexts: Map<Int, DayAcademicContext> = emptyMap(),
    val milestones: List<AcademicMilestone> = AcademicCalendarEngine.getMilestonesForAcademicYear()
)

class CalendarViewModel(
    private val repository: TimetableRepository,
    private val classAlarmScheduler: AlarmScheduler? = null,
    private val taskAlarmScheduler: TaskAlarmScheduler? = null
) : ViewModel() {

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage = _userMessage.asStateFlow()

    fun clearUserMessage() {
        _userMessage.value = null
    }

    private val calendar = Calendar.getInstance()

    private val initialProg = repository.getInitialProgramme() ?: ""
    private val _uiState = MutableStateFlow(
        CalendarUiState(
            currentProgramme = initialProg,
            isLoading = true
        )
    )
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    private var scheduleJob: Job? = null

    init {
        // Initialize calendar state to current day
        val initialDay = calendar.get(Calendar.DAY_OF_MONTH)
        recalculateMonth(selectedDay = initialDay)

        // Observe saved programme and load data
        viewModelScope.launch {
            repository.programmePref.collect { pref ->
                val prog = pref?.trim()?.ifEmpty { null }
                val current = _uiState.value.currentProgramme
                if (prog != null && prog != current) {
                    _uiState.value = _uiState.value.copy(currentProgramme = prog)
                    loadSchedule(prog)
                } else if (prog != null && _uiState.value.entries.isEmpty() && scheduleJob == null) {
                    loadSchedule(prog)
                }
            }
        }

        // Observe assignments
        viewModelScope.launch {
            repository.getAssignments().collect { list ->
                _uiState.value = _uiState.value.copy(assignments = list)
            }
        }

        // Observe custom activities / events
        viewModelScope.launch {
            repository.getCustomEvents().collect { events ->
                _uiState.value = _uiState.value.copy(customEvents = events)
            }
        }
    }

    fun setTabMode(mode: CalendarTabMode) {
        _uiState.value = _uiState.value.copy(activeTabMode = mode)
    }

    fun addCustomEvent(event: CustomEvent) {
        viewModelScope.launch {
            repository.saveCustomEvent(event)
        }
    }

    fun deleteCustomEvent(id: Long) {
        viewModelScope.launch {
            repository.deleteCustomEvent(id)
        }
    }

    fun setFilter(filter: CalendarFilter) {
        _uiState.value = _uiState.value.copy(selectedFilter = filter)
    }

    private fun loadSchedule(programme: String) {
        scheduleJob?.cancel()
        scheduleJob = viewModelScope.launch {
            repository.getLocalSchedule(programme).collect { entriesList ->
                _uiState.value = _uiState.value.copy(
                    entries = entriesList,
                    isLoading = false
                )
            }
        }
    }

    private fun recalculateMonth(selectedDay: Int = 1) {
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val validSelectedDay = selectedDay.coerceIn(1, daysInMonth)

        val temp = calendar.clone() as Calendar
        temp.set(Calendar.DAY_OF_MONTH, 1)
        val dow = temp.get(Calendar.DAY_OF_WEEK) // 1=Sunday, 2=Monday...
        val firstDayOfWeekOffset = (dow + 5) % 7 // Monday = 0, Sunday = 6

        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val yearMonthText = monthFormat.format(calendar.time)
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)

        temp.set(Calendar.DAY_OF_MONTH, validSelectedDay)
        val dayOfWeekFormat = SimpleDateFormat("EEEE", Locale.getDefault())
        val selectedDayOfWeek = dayOfWeekFormat.format(temp.time)

        // Determine if viewed month contains today
        val realNow = Calendar.getInstance()
        val todayDay = if (realNow.get(Calendar.YEAR) == currentYear && realNow.get(Calendar.MONTH) == currentMonth) {
            realNow.get(Calendar.DAY_OF_MONTH)
        } else {
            null
        }

        // Precompute day academic context for all days in the month once (zero UI-thread allocations)
        val monthContexts = HashMap<Int, DayAcademicContext>(daysInMonth)
        for (d in 1..daysInMonth) {
            monthContexts[d] = AcademicCalendarEngine.getAcademicContextForDate(currentYear, currentMonth, d)
        }

        val academicContext = monthContexts[validSelectedDay] ?: AcademicCalendarEngine.getAcademicContextForDate(
            currentYear,
            currentMonth,
            validSelectedDay
        )

        _uiState.value = _uiState.value.copy(
            yearMonthText = yearMonthText,
            currentYear = currentYear,
            currentMonth = currentMonth,
            daysInMonth = daysInMonth,
            firstDayOfWeekOffset = firstDayOfWeekOffset,
            selectedDay = validSelectedDay,
            selectedDayOfWeek = selectedDayOfWeek,
            todayDay = todayDay,
            academicContext = academicContext,
            monthAcademicContexts = monthContexts
        )
    }

    fun isTaskDueOnDay(assignment: Assignment, day: Int): Boolean {
        val dueMillis = TaskDateTimeParser.calculateDueMillis(assignment.dueDate)
        if (dueMillis != null) {
            val dueCal = Calendar.getInstance().apply { timeInMillis = dueMillis }
            return dueCal.get(Calendar.YEAR) == _uiState.value.currentYear &&
                    dueCal.get(Calendar.MONTH) == _uiState.value.currentMonth &&
                    dueCal.get(Calendar.DAY_OF_MONTH) == day
        }
        val dayOfWeekName = getDayOfWeekNameForDay(day)
        return assignment.dueDate.contains(dayOfWeekName, ignoreCase = true)
    }

    fun getDayWorkload(
        day: Int,
        dayOfWeek: String,
        dayLectures: List<TimetableEntry>,
        activities: List<CustomEvent>,
        assignments: List<Assignment>
    ): DayWorkload {
        val academic = _uiState.value.monthAcademicContexts[day] ?: AcademicCalendarEngine.getAcademicContextForDate(
            _uiState.value.currentYear,
            _uiState.value.currentMonth,
            day
        )

        val totalMins = dayLectures.sumOf { entry ->
            val start = TimeUtil.toMinutes(entry.startTime)
            val end = TimeUtil.toMinutes(entry.endTime)
            if (end > start) end - start else 60
        }

        val dayActivities = activities.filter { it.dayOfWeek.equals(dayOfWeek, ignoreCase = true) }
        val dueTasks = assignments.filter { !it.completed && isTaskDueOnDay(it, day) }

        return DayWorkload(
            totalLectureMinutes = totalMins,
            lectureCount = dayLectures.size,
            deadlineCount = dueTasks.size,
            activityCount = dayActivities.size,
            primaryMilestone = academic.primaryMilestone
        )
    }

    fun toggleTaskComplete(task: Assignment) {
        viewModelScope.launch {
            val updated = task.copy(completed = !task.completed)
            repository.saveAssignment(updated)
            if (updated.completed) {
                taskAlarmScheduler?.cancelTaskReminder(task.id)
                _userMessage.value = "Task completed • Alarm cancelled"
            } else if (task.reminderMinutes != null) {
                taskAlarmScheduler?.scheduleTaskReminder(updated)
                _userMessage.value = "Task reactivated • Alarm restored"
            }
        }
    }

    fun deleteTask(task: Assignment) {
        viewModelScope.launch {
            taskAlarmScheduler?.cancelTaskReminder(task.id)
            repository.deleteAssignment(task.id)
            _userMessage.value = "Task deleted • Alarm cancelled"
        }
    }

    fun saveTask(task: Assignment) {
        viewModelScope.launch {
            val generatedId = repository.saveAssignment(task)
            if (task.reminderMinutes != null && generatedId > 0) {
                taskAlarmScheduler?.scheduleTaskReminder(task.copy(id = generatedId))
                _userMessage.value = "Task saved • Alarm scheduled"
            } else {
                _userMessage.value = "Task saved"
            }
        }
    }

    fun previousMonth() {
        // Anchor to day 1 before adding/subtracting months to eliminate day-31 drift
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.add(Calendar.MONTH, -1)
        recalculateMonth(selectedDay = 1)
    }

    fun nextMonth() {
        // Anchor to day 1 before adding/subtracting months to eliminate day-31 drift
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.add(Calendar.MONTH, 1)
        recalculateMonth(selectedDay = 1)
    }

    /**
     * Navigates directly to an exact target date (year, month, day), ensuring
     * full synchronization across month boundaries when jumping from milestones.
     */
    fun navigateToDate(year: Int, month: Int, day: Int) {
        calendar.set(Calendar.YEAR, year)
        calendar.set(Calendar.MONTH, month)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val validDay = day.coerceIn(1, maxDays)
        calendar.set(Calendar.DAY_OF_MONTH, validDay)
        recalculateMonth(selectedDay = validDay)
    }

    fun selectDay(day: Int) {
        val daysInMonth = _uiState.value.daysInMonth
        val validDay = day.coerceIn(1, daysInMonth)
        val temp = calendar.clone() as Calendar
        temp.set(Calendar.DAY_OF_MONTH, validDay)
        val dayOfWeekFormat = SimpleDateFormat("EEEE", Locale.getDefault())
        val selectedDayOfWeek = dayOfWeekFormat.format(temp.time)

        val academicContext = _uiState.value.monthAcademicContexts[validDay] ?: AcademicCalendarEngine.getAcademicContextForDate(
            _uiState.value.currentYear,
            _uiState.value.currentMonth,
            validDay
        )

        _uiState.value = _uiState.value.copy(
            selectedDay = validDay,
            selectedDayOfWeek = selectedDayOfWeek,
            academicContext = academicContext
        )
    }

    fun previousDay() {
        if (_uiState.value.selectedDay > 1) {
            selectDay(_uiState.value.selectedDay - 1)
        } else {
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.add(Calendar.MONTH, -1)
            val prevDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            calendar.set(Calendar.DAY_OF_MONTH, prevDays)
            recalculateMonth(selectedDay = prevDays)
        }
    }

    fun nextDay() {
        if (_uiState.value.selectedDay < _uiState.value.daysInMonth) {
            selectDay(_uiState.value.selectedDay + 1)
        } else {
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.add(Calendar.MONTH, 1)
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            recalculateMonth(selectedDay = 1)
        }
    }

    fun getDayOfWeekNameForDay(day: Int): String {
        val temp = calendar.clone() as Calendar
        temp.set(Calendar.DAY_OF_MONTH, day.coerceIn(1, _uiState.value.daysInMonth))
        return SimpleDateFormat("EEEE", Locale.getDefault()).format(temp.time)
    }

    fun isToday(day: Int): Boolean {
        return _uiState.value.todayDay == day
    }

    fun selectToday() {
        val now = Calendar.getInstance()
        calendar.timeInMillis = now.timeInMillis
        recalculateMonth(selectedDay = now.get(Calendar.DAY_OF_MONTH))
    }

    fun saveNote(entry: TimetableEntry, noteText: String, reminderMinutes: Int?) {
        viewModelScope.launch {
            repository.saveNote(entry.naturalKey, noteText, reminderMinutes)
            if (reminderMinutes != null) {
                val ok = classAlarmScheduler?.scheduleClassAlarm(entry, reminderMinutes) ?: true
                _userMessage.value = if (ok) "Alarm scheduled for ${entry.courseCode} (${reminderMinutes}m before)" else "Class alarm set"
            } else {
                classAlarmScheduler?.cancelClassAlarm(entry.naturalKey)
                _userMessage.value = "Alarm turned off for ${entry.courseCode}"
            }
        }
    }

    companion object {
        fun provideFactory(
            repository: TimetableRepository,
            classAlarmScheduler: AlarmScheduler? = null,
            taskAlarmScheduler: TaskAlarmScheduler? = null
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return CalendarViewModel(repository, classAlarmScheduler, taskAlarmScheduler) as T
                }
            }
    }
}
