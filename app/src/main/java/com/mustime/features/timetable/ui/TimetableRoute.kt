package com.mustime.features.timetable.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mustime.TimetableApplication
import com.mustime.core.alarm.AlarmScheduler
import com.mustime.core.alarm.TaskAlarmScheduler

@Composable
fun TimetableRoute(
    onSettingsClick: () -> Unit = {},
    onNavigateToNotes: () -> Unit = {},
    onNavigateToCalendar: () -> Unit = {},
    onNavigateToTasks: () -> Unit = {},
    onReconfigureAcademicProfile: () -> Unit = {}
) {
    val context = LocalContext.current
    val app = context.applicationContext as? TimetableApplication
    val repository = app?.repository

    if (repository == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    val alarmScheduler = remember(context) { TaskAlarmScheduler(context) }
    val classAlarmScheduler = remember(context) { AlarmScheduler(context) }
    val viewModel: TimetableViewModel = viewModel(
        factory = TimetableViewModel.provideFactory(repository, alarmScheduler, classAlarmScheduler)
    )

    val uiState by viewModel.uiState.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()

    LaunchedEffect(userMessage) {
        userMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearUserMessage()
        }
    }

    TimetableScreen(
        programme = uiState.programme,
        entries = uiState.entries,
        customEvents = uiState.customEvents,
        assignments = uiState.assignments,
        notes = uiState.notes,
        selectedDay = uiState.selectedDay,
        isLoading = uiState.isLoading,
        onDaySelected = { viewModel.selectDay(it) },
        onSaveNote = { entry, note, reminder -> viewModel.saveNote(entry, note, reminder) },
        onAddActivity = { viewModel.addCustomEvent(it) },
        onDeleteActivity = { viewModel.deleteCustomEvent(it) },
        onAddTask = { title, course, dueDate, priority, reminder, notes ->
            viewModel.addTask(title, course, dueDate, priority, reminder, notes)
        },
        onToggleTaskComplete = { viewModel.toggleTaskComplete(it) },
        onDeleteTask = { viewModel.deleteTask(it) },
        onSettingsClick = onSettingsClick,
        onReconfigureAcademicProfile = onReconfigureAcademicProfile,
        onNavigateToNotes = onNavigateToNotes,
        onNavigateToCalendar = onNavigateToCalendar,
        onNavigateToTasks = onNavigateToTasks
    )
}
