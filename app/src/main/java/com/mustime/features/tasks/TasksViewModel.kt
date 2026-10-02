package com.mustime.features.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mustime.core.alarm.TaskAlarmScheduler
import com.mustime.features.timetable.data.TimetableRepository
import com.mustime.features.timetable.domain.Assignment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TasksUiState(
    val isLoading: Boolean = true,
    val assignments: List<Assignment> = emptyList(),
    val searchQuery: String = "",
    val selectedFilter: String = "All", // "All", "Pending", "Completed"
    val messageSnackbar: String? = null
)

class TasksViewModel(
    private val repository: TimetableRepository,
    private val alarmScheduler: TaskAlarmScheduler? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(TasksUiState(isLoading = true))
    val uiState: StateFlow<TasksUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAssignments().collect { list ->
                _uiState.value = _uiState.value.copy(
                    assignments = list,
                    isLoading = false
                )
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun setFilter(filter: String) {
        _uiState.value = _uiState.value.copy(selectedFilter = filter)
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(messageSnackbar = null)
    }

    fun toggleComplete(task: Assignment) {
        val newCompleted = !task.completed
        // Instant optimistic in-memory update for 0ms UI response
        val updatedTasks = _uiState.value.assignments.map {
            if (it.id == task.id) it.copy(completed = newCompleted) else it
        }
        _uiState.value = _uiState.value.copy(assignments = updatedTasks)

        viewModelScope.launch {
            repository.updateAssignmentCompletion(task.id, newCompleted)
            if (newCompleted) {
                alarmScheduler?.cancelTaskReminder(task.id)
                _uiState.value = _uiState.value.copy(messageSnackbar = "Task completed • Alarm reminder removed")
            } else if (task.reminderMinutes != null) {
                val scheduled = alarmScheduler?.scheduleTaskReminder(task.copy(completed = false)) == true
                if (scheduled) {
                    _uiState.value = _uiState.value.copy(messageSnackbar = "Task reactivated • Alarm reminder restored")
                }
            }
        }
    }

    fun deleteAssignment(id: Long) {
        viewModelScope.launch {
            alarmScheduler?.cancelTaskReminder(id)
            repository.deleteAssignment(id)
            _uiState.value = _uiState.value.copy(messageSnackbar = "Task deleted • Alarm cancelled")
        }
    }

    fun saveAssignment(
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
            if (reminderMinutes != null && generatedId > 0) {
                val savedTask = task.copy(id = generatedId)
                val scheduled = alarmScheduler?.scheduleTaskReminder(savedTask) == true
                if (scheduled) {
                    _uiState.value = _uiState.value.copy(messageSnackbar = "Task saved • Alarm reminder scheduled")
                } else {
                    _uiState.value = _uiState.value.copy(messageSnackbar = "Task saved")
                }
            } else {
                _uiState.value = _uiState.value.copy(messageSnackbar = "Task saved")
            }
        }
    }

    fun updateAssignment(task: Assignment) {
        viewModelScope.launch {
            repository.saveAssignment(task)
            alarmScheduler?.cancelTaskReminder(task.id)
            if (task.reminderMinutes != null && !task.completed) {
                val scheduled = alarmScheduler?.scheduleTaskReminder(task) == true
                if (scheduled) {
                    _uiState.value = _uiState.value.copy(messageSnackbar = "Task updated • Alarm rescheduled")
                } else {
                    _uiState.value = _uiState.value.copy(messageSnackbar = "Task updated")
                }
            } else {
                _uiState.value = _uiState.value.copy(messageSnackbar = "Task updated")
            }
        }
    }

    fun triggerTestReminder(task: Assignment, delaySeconds: Int = 3) {
        alarmScheduler?.scheduleTestReminder(task, delaySeconds)
        _uiState.value = _uiState.value.copy(
            messageSnackbar = "AlarmManager will trigger notification in ${delaySeconds}s!"
        )
    }

    companion object {
        fun provideFactory(
            repository: TimetableRepository,
            alarmScheduler: TaskAlarmScheduler? = null
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return TasksViewModel(repository, alarmScheduler) as T
                }
            }
    }
}
