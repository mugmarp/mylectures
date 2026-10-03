package com.mustime.features.timetable.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mustime.core.alarm.AlarmScheduler
import com.mustime.core.alarm.TaskAlarmScheduler
import com.mustime.core.util.TimeUtil
import com.mustime.features.timetable.data.TimetableRepository
import com.mustime.features.timetable.domain.Assignment
import com.mustime.features.timetable.domain.CustomEvent
import com.mustime.features.timetable.domain.LectureNote
import com.mustime.features.timetable.domain.TimetableEntry
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TimetableUiState(
    val isLoading: Boolean = true,
    val selectedDay: String = TimeUtil.todayName(),
    val entries: List<TimetableEntry> = emptyList(),
    val customEvents: List<CustomEvent> = emptyList(),
    val assignments: List<Assignment> = emptyList(),
    val notes: Map<String, LectureNote> = emptyMap(),
    val programme: String = ""
)

class TimetableViewModel(
    private val repository: TimetableRepository,
    private val alarmScheduler: TaskAlarmScheduler? = null,
    private val classAlarmScheduler: AlarmScheduler? = null
) : ViewModel() {

    private val initialProg = repository.getInitialProgramme()?.trim() ?: ""
    private val _uiState = MutableStateFlow(
        TimetableUiState(
            isLoading = initialProg.isNotEmpty(),
            programme = initialProg
        )
    )
    val uiState: StateFlow<TimetableUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun refreshTimetable() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                repository.refreshTimetable(_uiState.value.programme)
                if (_uiState.value.programme.isNotEmpty()) {
                    loadSchedule(_uiState.value.programme)
                }
                kotlinx.coroutines.delay(500)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private var scheduleJob: Job? = null

    init {
        viewModelScope.launch {
            repository.programmePref.collect { pref ->
                val prog = pref?.trim() ?: ""
                val currentProg = _uiState.value.programme
                if (prog != currentProg) {
                    _uiState.value = _uiState.value.copy(programme = prog)
                    if (prog.isNotEmpty()) {
                        loadSchedule(prog)
                    } else {
                        _uiState.value = _uiState.value.copy(entries = emptyList(), isLoading = false)
                    }
                } else if (_uiState.value.entries.isEmpty() && scheduleJob == null) {
                    if (prog.isNotEmpty()) {
                        loadSchedule(prog)
                    } else {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                    }
                }
            }
        }
        viewModelScope.launch {
            repository.getAllNotes().collect { noteList ->
                _uiState.value = _uiState.value.copy(notes = noteList.associateBy { it.naturalKey })
            }
        }
        viewModelScope.launch {
            repository.getCustomEvents().collect { events ->
                _uiState.value = _uiState.value.copy(customEvents = events)
            }
        }
        viewModelScope.launch {
            repository.getAssignments().collect { assignmentList ->
                _uiState.value = _uiState.value.copy(assignments = assignmentList)
            }
        }
    }

    private fun loadSchedule(programme: String) {
        val progToLoad = programme.trim()
        if (progToLoad.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                entries = emptyList(),
                isLoading = false
            )
            return
        }
        scheduleJob?.cancel()
        scheduleJob = viewModelScope.launch {
            repository.getLocalSchedule(progToLoad).collect { entries ->
                _uiState.value = _uiState.value.copy(
                    entries = entries,
                    isLoading = false
                )
            }
        }
    }

    fun selectDay(day: String) {
        _uiState.value = _uiState.value.copy(selectedDay = day)
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

    fun addTask(
        title: String,
        courseCode: String,
        dueDate: String,
        priority: String,
        reminderMinutes: Int?,
        notes: String = ""
    ) {
        viewModelScope.launch {
            val task = Assignment(
                title = title,
                courseCode = courseCode,
                dueDate = dueDate,
                reminderMinutes = reminderMinutes,
                priority = priority,
                notes = notes,
                completed = false
            )
            val generatedId = repository.saveAssignment(task)
            if (reminderMinutes != null && generatedId > 0 && alarmScheduler != null) {
                alarmScheduler.scheduleTaskReminder(task.copy(id = generatedId))
            }
        }
    }

    fun toggleTaskComplete(task: Assignment) {
        val newCompleted = !task.completed
        // Instant optimistic update
        val updated = _uiState.value.assignments.map {
            if (it.id == task.id) it.copy(completed = newCompleted) else it
        }
        _uiState.value = _uiState.value.copy(assignments = updated)
        viewModelScope.launch {
            repository.updateAssignmentCompletion(task.id, newCompleted)
        }
    }

    fun deleteTask(id: Long) {
        viewModelScope.launch {
            repository.deleteAssignment(id)
        }
    }

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage = _userMessage.asStateFlow()

    fun clearUserMessage() {
        _userMessage.value = null
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

    fun triggerTestAlarm(entry: TimetableEntry) {
        classAlarmScheduler?.triggerTestAlarm(entry)
    }

    companion object {
        fun provideFactory(
            repository: TimetableRepository,
            alarmScheduler: TaskAlarmScheduler? = null,
            classAlarmScheduler: AlarmScheduler? = null
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return TimetableViewModel(repository, alarmScheduler, classAlarmScheduler) as T
                }
            }
    }
}

