package com.mustime.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mustime.core.alarm.TaskAlarmScheduler
import com.mustime.features.timetable.data.DataLoader
import com.mustime.features.timetable.data.TimetableRepository
import com.mustime.ui.ThemeMode
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SettingsUiState(
    val currentProgramme: String = "",
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val darkMode: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val selectedAccent: Int = 0,
    val lastSyncTime: String = "Up to date",
    val isSyncing: Boolean = false,
    val isResetting: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)

class SettingsViewModel(
    private val repository: TimetableRepository,
    private val dataLoader: DataLoader? = null,
    private val alarmScheduler: TaskAlarmScheduler? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val initialProg = repository.getInitialProgramme() ?: ""
    private val _uiState = MutableStateFlow(SettingsUiState(currentProgramme = initialProg))
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        // Collect current programme
        viewModelScope.launch {
            repository.programmePref.collect { pref ->
                if (!pref.isNullOrBlank()) {
                    _uiState.value = _uiState.value.copy(currentProgramme = pref)
                }
            }
        }

        // Collect theme mode
        viewModelScope.launch {
            repository.themeModePref.collect { modeStr ->
                val mode = try {
                    ThemeMode.valueOf(modeStr.uppercase())
                } catch (e: Exception) {
                    ThemeMode.SYSTEM
                }
                _uiState.value = _uiState.value.copy(
                    themeMode = mode,
                    darkMode = mode == ThemeMode.DARK
                )
            }
        }

        // Collect dark mode fallback
        viewModelScope.launch {
            repository.themeDarkPref.collect { dark ->
                _uiState.value = _uiState.value.copy(darkMode = dark)
            }
        }

        // Collect accent
        viewModelScope.launch {
            repository.themeAccentPref.collect { accent ->
                _uiState.value = _uiState.value.copy(selectedAccent = accent)
            }
        }

        // Collect notifications preference
        viewModelScope.launch {
            repository.notificationsEnabledPref.collect { enabled ->
                _uiState.value = _uiState.value.copy(notificationsEnabled = enabled)
            }
        }

        // Collect last sync time
        viewModelScope.launch {
            repository.lastSyncPref.collect { syncTime ->
                if (!syncTime.isNullOrBlank()) {
                    _uiState.value = _uiState.value.copy(lastSyncTime = syncTime)
                }
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            try {
                repository.setThemeMode(mode.name)
                _uiState.value = _uiState.value.copy(
                    themeMode = mode,
                    darkMode = mode == ThemeMode.DARK
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Failed to update appearance: ${e.message}")
            }
        }
    }

    fun setDarkMode(dark: Boolean) {
        viewModelScope.launch {
            try {
                repository.setThemeDark(dark)
                _uiState.value = _uiState.value.copy(
                    darkMode = dark,
                    themeMode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Failed to update appearance: ${e.message}")
            }
        }
    }

    fun setSelectedAccent(accent: Int) {
        viewModelScope.launch {
            try {
                repository.setThemeAccent(accent)
                _uiState.value = _uiState.value.copy(selectedAccent = accent)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Failed to update accent color: ${e.message}")
            }
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                repository.setNotificationsEnabled(enabled)
                _uiState.value = _uiState.value.copy(notificationsEnabled = enabled)

                val tasks = repository.getAssignments().firstOrNull() ?: emptyList()
                if (enabled) {
                    var count = 0
                    tasks.forEach { task ->
                        if (!task.completed && task.reminderMinutes != null) {
                            val ok = alarmScheduler?.scheduleTaskReminder(task) == true
                            if (ok) count++
                        }
                    }
                    _uiState.value = _uiState.value.copy(
                        successMessage = "Reminders active. $count alarms scheduled."
                    )
                } else {
                    alarmScheduler?.cancelAllTaskReminders(tasks)
                    _uiState.value = _uiState.value.copy(
                        successMessage = "All deadline alarms paused."
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Failed to update notification settings: ${e.message}")
            }
        }
    }

    fun syncTimetable() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true, errorMessage = null, successMessage = null)
            try {
                withContext(ioDispatcher) {
                    if (dataLoader != null) {
                        val entries = dataLoader.loadFromAssets()
                        if (entries.isNotEmpty()) {
                            repository.dao.deleteAllEntries()
                            repository.dao.upsertEntries(entries)
                        }
                    }
                }
                val format = SimpleDateFormat("h:mm a, MMM d", Locale.getDefault())
                val timeString = "Synced at ${format.format(Date())}"
                repository.setLastSyncTime(timeString)
                val totalEntries = withContext(ioDispatcher) { repository.dao.getEntryCount() }
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    lastSyncTime = timeString,
                    successMessage = "Timetable successfully synchronized! ($totalEntries lectures loaded)"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    errorMessage = "Synchronization failed: ${e.localizedMessage ?: "Unknown network/database error"}"
                )
            }
        }
    }

    fun resetAllNotes(onComplete: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isResetting = true, errorMessage = null)
            try {
                withContext(ioDispatcher) {
                    repository.deleteAllNotes()
                }
                _uiState.value = _uiState.value.copy(
                    isResetting = false,
                    successMessage = "All lecture notes have been reset."
                )
                onComplete()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isResetting = false,
                    errorMessage = "Failed to reset notes: ${e.message}"
                )
            }
        }
    }

    fun resetAllTasks(onComplete: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isResetting = true, errorMessage = null)
            try {
                withContext(ioDispatcher) {
                    val tasks = repository.getAssignments().firstOrNull() ?: emptyList()
                    alarmScheduler?.cancelAllTaskReminders(tasks)
                    repository.deleteAllAssignments()
                }
                _uiState.value = _uiState.value.copy(
                    isResetting = false,
                    successMessage = "All academic tasks and alarms have been cleared."
                )
                onComplete()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isResetting = false,
                    errorMessage = "Failed to reset tasks: ${e.message}"
                )
            }
        }
    }

    fun clearFeedbackMessages() {
        _uiState.value = _uiState.value.copy(successMessage = null, errorMessage = null)
    }

    companion object {
        fun provideFactory(
            repository: TimetableRepository,
            dataLoader: DataLoader? = null,
            alarmScheduler: TaskAlarmScheduler? = null,
            ioDispatcher: CoroutineDispatcher = Dispatchers.IO
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SettingsViewModel(repository, dataLoader, alarmScheduler, ioDispatcher) as T
                }
            }
    }
}
