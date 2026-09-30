package com.mustime.features.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mustime.features.timetable.data.TimetableRepository
import com.mustime.features.timetable.domain.Assignment
import com.mustime.features.timetable.domain.CustomEvent
import com.mustime.features.timetable.domain.TimetableEntry
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.mustime.core.alarm.AlarmScheduler
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class CalendarFilter(val label: String) {
    ALL("All Events"),
    LECTURES("Lectures"),
    ACTIVITIES("Activities")
}

data class CalendarUiState(
    val isLoading: Boolean = true,
    val currentProgramme: String = "",
    val entries: List<TimetableEntry> = emptyList(),
    val assignments: List<Assignment> = emptyList(),
    val customEvents: List<CustomEvent> = emptyList(),
    val selectedFilter: CalendarFilter = CalendarFilter.ALL,
    val yearMonthText: String = "",
    val daysInMonth: Int = 30,
    val firstDayOfWeekOffset: Int = 0,
    val selectedDay: Int = 1,
    val selectedDayOfWeek: String = "Monday"
)

class CalendarViewModel(
    private val repository: TimetableRepository,
    private val classAlarmScheduler: AlarmScheduler? = null
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
        // Initialize calendar state
        val initialDay = calendar.get(Calendar.DAY_OF_MONTH)
        recalculateMonth(selectedDay = initialDay)

        if (initialProg.isNotEmpty()) {
            loadSchedule(initialProg)
        }

        // Observe saved programme and load data
        viewModelScope.launch {
            repository.programmePref.collect { pref ->
                val prog = pref?.trim()?.ifEmpty { null }
                if (prog != null && (prog != _uiState.value.currentProgramme || _uiState.value.entries.isEmpty())) {
                    _uiState.value = _uiState.value.copy(currentProgramme = prog)
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

        temp.set(Calendar.DAY_OF_MONTH, validSelectedDay)
        val dayOfWeekFormat = SimpleDateFormat("EEEE", Locale.getDefault())
        val selectedDayOfWeek = dayOfWeekFormat.format(temp.time)

        _uiState.value = _uiState.value.copy(
            yearMonthText = yearMonthText,
            daysInMonth = daysInMonth,
            firstDayOfWeekOffset = firstDayOfWeekOffset,
            selectedDay = validSelectedDay,
            selectedDayOfWeek = selectedDayOfWeek
        )
    }

    fun previousMonth() {
        calendar.add(Calendar.MONTH, -1)
        recalculateMonth(selectedDay = 1)
    }

    fun nextMonth() {
        calendar.add(Calendar.MONTH, 1)
        recalculateMonth(selectedDay = 1)
    }

    fun selectDay(day: Int) {
        val daysInMonth = _uiState.value.daysInMonth
        val validDay = day.coerceIn(1, daysInMonth)
        val temp = calendar.clone() as Calendar
        temp.set(Calendar.DAY_OF_MONTH, validDay)
        val dayOfWeekFormat = SimpleDateFormat("EEEE", Locale.getDefault())
        val selectedDayOfWeek = dayOfWeekFormat.format(temp.time)

        _uiState.value = _uiState.value.copy(
            selectedDay = validDay,
            selectedDayOfWeek = selectedDayOfWeek
        )
    }

    fun previousDay() {
        if (_uiState.value.selectedDay > 1) {
            selectDay(_uiState.value.selectedDay - 1)
        } else {
            calendar.add(Calendar.MONTH, -1)
            val prevDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            recalculateMonth(selectedDay = prevDays)
        }
    }

    fun nextDay() {
        if (_uiState.value.selectedDay < _uiState.value.daysInMonth) {
            selectDay(_uiState.value.selectedDay + 1)
        } else {
            calendar.add(Calendar.MONTH, 1)
            recalculateMonth(selectedDay = 1)
        }
    }

    fun getDayOfWeekNameForDay(day: Int): String {
        val temp = calendar.clone() as Calendar
        temp.set(Calendar.DAY_OF_MONTH, day.coerceIn(1, _uiState.value.daysInMonth))
        return SimpleDateFormat("EEEE", Locale.getDefault()).format(temp.time)
    }

    fun isToday(day: Int): Boolean {
        val now = Calendar.getInstance()
        return now.get(Calendar.YEAR) == calendar.get(Calendar.YEAR) &&
                now.get(Calendar.MONTH) == calendar.get(Calendar.MONTH) &&
                now.get(Calendar.DAY_OF_MONTH) == day
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
            classAlarmScheduler: AlarmScheduler? = null
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return CalendarViewModel(repository, classAlarmScheduler) as T
                }
            }
    }
}
