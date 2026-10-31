package com.mustime.features.settings

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mustime.core.alarm.AlarmScheduler
import com.mustime.core.alarm.ReminderScheduler
import com.mustime.core.alarm.TaskAlarmScheduler
import com.mustime.core.notification.NotificationHelper
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
    val classAlarmLeadMinutes: Int = 30,
    val taskReminderLeadHours: Int = 2,
    val alarmVibration: Boolean = true,
    val alarmSound: String = "Chime",
    val selectedAccent: Int = 0,
    val lastSyncTime: String = "Up to date",
    val timetableVersion: String = "Final Timetable",
    val timetableStatus: String = "Official Final",
    val isFinal: Boolean = true,
    val metadata: com.mustime.features.timetable.domain.TimetableMetadata? = null,
    val isSyncing: Boolean = false,
    val isResetting: Boolean = false,
    val userNotesCount: Int = 0,
    val userEventsCount: Int = 0,
    val userAssignmentsCount: Int = 0,
    val isBackingUpUserData: Boolean = false,
    val userDataBackupStatus: String = "Local storage active (Guest mode)",
    val userAccountEmail: String? = null,
    val isCloudSyncActive: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)

class SettingsViewModel(
    private val repository: TimetableRepository,
    private val dataLoader: DataLoader? = null,
    private val alarmScheduler: TaskAlarmScheduler? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val application: android.app.Application,
    private val classAlarmScheduler: AlarmScheduler? = null,
    private val reminderScheduler: ReminderScheduler? = null
) : AndroidViewModel(application) {

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

        // Collect class alarm lead minutes
        viewModelScope.launch {
            repository.classAlarmLeadPref.collect { lead ->
                _uiState.value = _uiState.value.copy(classAlarmLeadMinutes = lead)
            }
        }

        // Collect task reminder lead hours
        viewModelScope.launch {
            repository.taskReminderLeadPref.collect { lead ->
                _uiState.value = _uiState.value.copy(taskReminderLeadHours = lead)
            }
        }

        // Collect alarm vibration
        viewModelScope.launch {
            repository.alarmVibrationPref.collect { vib ->
                _uiState.value = _uiState.value.copy(alarmVibration = vib)
            }
        }

        // Collect alarm sound
        viewModelScope.launch {
            repository.alarmSoundPref.collect { sound ->
                _uiState.value = _uiState.value.copy(alarmSound = sound)
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

        // Collect timetable version & status
        viewModelScope.launch {
            repository.timetableMetadata.collect { meta ->
                if (meta != null) {
                    _uiState.value = _uiState.value.copy(
                        timetableVersion = meta.versionLabel,
                        timetableStatus = meta.status,
                        isFinal = meta.isFinal,
                        metadata = meta
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        timetableVersion = repository.getTimetableVersion(),
                        timetableStatus = repository.getTimetableStatus()
                    )
                }
            }
        }

        // Collect user item counts for cloud sync card
        viewModelScope.launch {
            repository.getAllNotes().collect { notes ->
                _uiState.value = _uiState.value.copy(userNotesCount = notes.size)
            }
        }
        viewModelScope.launch {
            repository.getCustomEvents().collect { events ->
                _uiState.value = _uiState.value.copy(userEventsCount = events.size)
            }
        }
        viewModelScope.launch {
            repository.getAssignments().collect { assignments ->
                _uiState.value = _uiState.value.copy(userAssignmentsCount = assignments.size)
            }
        }

        // Reactively observe authentication state from AuthRepository
        viewModelScope.launch {
            com.mustime.core.auth.AuthRepository.getInstance().currentUser.collect { user ->
                if (user != null) {
                    _uiState.value = _uiState.value.copy(
                        userAccountEmail = user.email ?: user.displayName ?: user.uid,
                        isCloudSyncActive = true,
                        userDataBackupStatus = "Signed in as ${user.displayName ?: user.email ?: "Student"}"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        userAccountEmail = null,
                        isCloudSyncActive = false,
                        userDataBackupStatus = "Guest mode · Local storage active"
                    )
                }
            }
        }
    }

    fun syncUserData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBackingUpUserData = true, errorMessage = null, successMessage = null)
            try {
                val currentUid = try {
                    com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                } catch (_: Throwable) { null }

                val stats = withContext(ioDispatcher) {
                    if (currentUid != null) {
                        repository.performFullUserSync(currentUid)
                    } else {
                        val notes = repository.dao.getAllNotesSync()
                        val events = repository.getCustomEvents().firstOrNull() ?: emptyList()
                        val assignments = repository.getAssignments().firstOrNull() ?: emptyList()
                        com.mustime.core.sync.SyncMergeStats(
                            notesCount = notes.size,
                            eventsCount = events.size,
                            assignmentsCount = assignments.size
                        )
                    }
                }

                val format = SimpleDateFormat("h:mm a, MMM d", Locale.getDefault())
                val timeString = format.format(Date())

                _uiState.value = _uiState.value.copy(
                    isBackingUpUserData = false,
                    userDataBackupStatus = if (currentUid != null) "Synced with Cloud at $timeString" else "Preserved locally · Verified at $timeString",
                    successMessage = if (stats != null && stats.pushedCount > 0) {
                        "Cloud sync completed: ${stats.pushedCount} records synced, ${stats.conflictsResolvedCount} conflicts safely resolved."
                    } else {
                        "Data secure: ${stats?.notesCount ?: 0} notes, ${stats?.eventsCount ?: 0} events, ${stats?.assignmentsCount ?: 0} coursework tasks ready to sync."
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isBackingUpUserData = false,
                    errorMessage = "User sync error: ${e.localizedMessage ?: "Unknown error"}"
                )
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
                val notesWithAlarms = repository.getNotesWithAlarms()

                if (enabled) {
                    var count = 0
                    tasks.forEach { task ->
                        if (!task.completed && task.reminderMinutes != null) {
                            val ok = alarmScheduler?.scheduleTaskReminder(task) == true
                            if (ok) count++
                        }
                    }
                    var classCount = 0
                    notesWithAlarms.forEach { note ->
                        val mins = note.alarmMinutes ?: return@forEach
                        val entry = repository.getTimetableEntryByNaturalKey(note.naturalKey)
                        val ok = if (entry != null) {
                            classAlarmScheduler?.scheduleClassAlarm(entry, mins) == true
                        } else {
                            classAlarmScheduler?.scheduleClassAlarmFromNote(note, mins) == true
                        }
                        if (ok) classCount++
                    }

                    reminderScheduler?.scheduleDailyReminder()

                    _uiState.value = _uiState.value.copy(
                        successMessage = "Reminders active. $count task alerts, $classCount class alarms, and daily digest enabled."
                    )
                } else {
                    alarmScheduler?.cancelAllTaskReminders(tasks)
                    notesWithAlarms.forEach { note ->
                        classAlarmScheduler?.cancelClassAlarm(note.naturalKey)
                    }
                    reminderScheduler?.cancelDailyReminder()

                    _uiState.value = _uiState.value.copy(
                        successMessage = "All alarms and reminders paused (quiet mode)."
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Failed to update notification settings: ${e.message}")
            }
        }
    }

    fun setClassAlarmLeadMinutes(minutes: Int) {
        viewModelScope.launch {
            try {
                repository.setClassAlarmLeadMinutes(minutes)
                _uiState.value = _uiState.value.copy(
                    classAlarmLeadMinutes = minutes,
                    successMessage = "Lecture reminder updated to $minutes minutes before."
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Failed to update class reminder: ${e.message}")
            }
        }
    }

    fun setTaskReminderLeadHours(hours: Int) {
        viewModelScope.launch {
            try {
                repository.setTaskReminderLeadHours(hours)
                val label = if (hours >= 24) "${hours / 24} day before" else "$hours hours before"
                _uiState.value = _uiState.value.copy(
                    taskReminderLeadHours = hours,
                    successMessage = "Task reminder updated to $label."
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Failed to update task reminder: ${e.message}")
            }
        }
    }

    fun setAlarmVibration(vibrate: Boolean) {
        viewModelScope.launch {
            try {
                repository.setAlarmVibration(vibrate)
                _uiState.value = _uiState.value.copy(
                    alarmVibration = vibrate,
                    successMessage = if (vibrate) "Alarm vibration enabled." else "Alarm vibration turned off."
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Failed to update vibration setting: ${e.message}")
            }
        }
    }

    fun setAlarmSound(sound: String) {
        viewModelScope.launch {
            try {
                repository.setAlarmSound(sound)
                NotificationHelper.applySoundProfile(getApplication(), sound)
                _uiState.value = _uiState.value.copy(
                    alarmSound = sound,
                    successMessage = "Alarm sound profile set to $sound."
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Failed to update alarm sound: ${e.message}")
            }
        }
    }

    fun sendTestNotification(context: Context) {
        try {
            NotificationHelper.showClassReminderNotification(
                context = context,
                courseCode = "BCS2101",
                courseTitle = "Data Structures & Algorithms",
                room = "Comp Lab 2 (Main Campus)",
                startTime = "09:00 AM",
                lecturer = "Dr. Mugisha"
            )
            _uiState.value = _uiState.value.copy(
                successMessage = "Test class reminder notification posted! Check your status bar."
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Failed to dispatch test notification: ${e.message}"
            )
        }
    }

    fun rescheduleAllAlarms() {
        viewModelScope.launch {
            try {
                val tasks = repository.getAssignments().firstOrNull() ?: emptyList()
                var scheduledCount = 0
                tasks.forEach { task ->
                    if (!task.completed && task.reminderMinutes != null) {
                        val ok = alarmScheduler?.scheduleTaskReminder(task) == true
                        if (ok) scheduledCount++
                    }
                }

                val notesWithAlarms = repository.getNotesWithAlarms()
                var classCount = 0
                notesWithAlarms.forEach { note ->
                    val mins = note.alarmMinutes ?: return@forEach
                    val entry = repository.getTimetableEntryByNaturalKey(note.naturalKey)
                    val ok = if (entry != null) {
                        classAlarmScheduler?.scheduleClassAlarm(entry, mins) == true
                    } else {
                        classAlarmScheduler?.scheduleClassAlarmFromNote(note, mins) == true
                    }
                    if (ok) classCount++
                }

                reminderScheduler?.scheduleDailyReminder()

                _uiState.value = _uiState.value.copy(
                    successMessage = "Rescheduled $scheduledCount task alerts, $classCount class alarms, and morning digest."
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Failed to reschedule alarms: ${e.message}")
            }
        }
    }

    fun syncTimetable() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true, errorMessage = null, successMessage = null)
            try {
                var remoteSynced = false
                withContext(ioDispatcher) {
                    val remoteMeta = repository.syncRepository?.fetchRemoteTimetableMetadata()
                    if (remoteMeta != null) {
                        repository.updateTimetableMetadata(remoteMeta)
                        remoteSynced = true
                    } else if (dataLoader != null) {
                        val entries = dataLoader.loadFromAssets()
                        if (entries.isNotEmpty()) {
                            repository.dao.deleteAllEntries()
                            repository.dao.upsertEntries(entries)
                        }
                        val meta = dataLoader.loadMetadataFromAssets()
                        if (meta != null) {
                            repository.updateTimetableMetadata(meta.copy(lastUpdated = System.currentTimeMillis()))
                        }
                    }
                }
                val format = SimpleDateFormat("h:mm a, MMM d", Locale.getDefault())
                val timeString = "Synced at ${format.format(Date())}"
                repository.setLastSyncTime(timeString)
                val totalEntries = withContext(ioDispatcher) { repository.dao.getEntryCount() }
                val syncStatus = if (remoteSynced) "Cloud release verified · $totalEntries lectures" else "$totalEntries lectures loaded"
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    lastSyncTime = timeString,
                    successMessage = "Timetable synchronized ($syncStatus)"
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
            classAlarmScheduler: AlarmScheduler? = null,
            reminderScheduler: ReminderScheduler? = null,
            ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
            application: Application
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SettingsViewModel(
                        repository = repository,
                        dataLoader = dataLoader,
                        alarmScheduler = alarmScheduler,
                        ioDispatcher = ioDispatcher,
                        application = application,
                        classAlarmScheduler = classAlarmScheduler,
                        reminderScheduler = reminderScheduler
                    ) as T
                }
            }
    }
}
