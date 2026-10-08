package com.mustime.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mustime.TimetableApplication
import com.mustime.features.calendar.CalendarScreen
import com.mustime.features.notes.NotesScreen
import com.mustime.features.onboarding.*
import com.mustime.features.settings.SettingsScreen
import com.mustime.features.tasks.TasksScreen
import com.mustime.features.timetable.ui.TimetableRoute
import com.mustime.features.timetable.ui.WelcomeScreen
import kotlinx.coroutines.launch

sealed class AppNavState {
    data object Welcome : AppNavState()
    data object FacultySelection : AppNavState()
    data class ProgrammeSelection(val faculty: Faculty) : AppNavState()
    data class ClassGroupSelection(val faculty: Faculty, val programme: Programme) : AppNavState()
    data object MainApp : AppNavState()
}

@Composable
fun MainScaffold() {
    val context = LocalContext.current
    val app = context.applicationContext as? TimetableApplication
    val repository = app?.repository

    if (repository == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    val scope = rememberCoroutineScope()
    val initialProg = remember { repository.getInitialProgramme() }
    val savedProgramme by repository.programmePref.collectAsState(initial = initialProg)
    val initialOnboarding = remember { repository.isOnboardingCompleted() }
    val onboardingCompleted by repository.onboardingCompletedPref.collectAsState(initial = initialOnboarding)
    
    var navState by remember {
        mutableStateOf<AppNavState>(
            if (initialOnboarding) AppNavState.MainApp else AppNavState.Welcome
        )
    }
    var selectedTab by remember { mutableIntStateOf(0) }
    var isNotesEditing by remember { mutableStateOf(false) }

    // First app launch resolution via DataStore / Preferences
    LaunchedEffect(onboardingCompleted) {
        if (onboardingCompleted && (navState is AppNavState.Welcome || navState is AppNavState.FacultySelection)) {
            navState = AppNavState.MainApp
        }
    }

    when (val state = navState) {
        is AppNavState.Welcome -> {
            WelcomeScreen(
                onGetStarted = { navState = AppNavState.FacultySelection }
            )
        }
        is AppNavState.FacultySelection -> {
            BackHandler {
                navState = if (onboardingCompleted == true) AppNavState.MainApp else AppNavState.Welcome
            }
            FacultySelectionScreen(
                onSelectFaculty = { faculty -> navState = AppNavState.ProgrammeSelection(faculty) },
                onBack = {
                    navState = if (onboardingCompleted == true) AppNavState.MainApp else AppNavState.Welcome
                }
            )
        }
        is AppNavState.ProgrammeSelection -> {
            BackHandler {
                navState = AppNavState.FacultySelection
            }
            ProgrammeSelectionScreen(
                faculty = state.faculty,
                onSelectProgramme = { prog -> navState = AppNavState.ClassGroupSelection(state.faculty, prog) },
                onBack = { navState = AppNavState.FacultySelection }
            )
        }
        is AppNavState.ClassGroupSelection -> {
            BackHandler {
                navState = AppNavState.ProgrammeSelection(state.faculty)
            }
            val allEntries by repository.getAllEntries().collectAsState(initial = emptyList())
            val calculatedCounts = remember(allEntries, state.programme) {
                state.programme.defaultYears.associate { yr ->
                    val g = "${state.programme.code} $yr"
                    g to com.mustime.features.timetable.domain.TimetableMatcher.filterTimetable(allEntries, g).size
                }
            }
            ClassGroupSelectionScreen(
                programme = state.programme,
                faculty = state.faculty,
                entryCounts = calculatedCounts,
                onConfirm = { chosenGroup ->
                    scope.launch {
                        repository.setProgrammePref(chosenGroup)
                        repository.setOnboardingCompleted(true)
                        navState = AppNavState.MainApp
                    }
                },
                onBack = { navState = AppNavState.ProgrammeSelection(state.faculty) }
            )
        }
        is AppNavState.MainApp -> {
            var isSettingsOpen by remember { mutableStateOf(false) }
            var isVacantRoomsOpen by remember { mutableStateOf(false) }

            val haptic = LocalHapticFeedback.current
            val tabs = listOf("Timetable", "Calendar", "Notes", "Tasks")
            val filledIcons = listOf(
                Icons.Filled.CalendarToday,
                Icons.Filled.CalendarMonth,
                Icons.Filled.Notes,
                Icons.Filled.TaskAlt
            )
            val outlinedIcons = listOf(
                Icons.Outlined.CalendarToday,
                Icons.Outlined.CalendarMonth,
                Icons.Outlined.Notes,
                Icons.Outlined.TaskAlt
            )

            val isDark = com.mustime.ui.LocalAppTheme.current.isDark
            val navBg = if (isDark) com.mustime.features.timetable.ui.DarkSurfaceCard else Color.White
            val navBorder = if (isDark) com.mustime.features.timetable.ui.DarkBorderSubtle else com.mustime.features.timetable.ui.BorderSubtleLight
            val primaryBlue = com.mustime.features.timetable.ui.PrimaryBlue
            val textMuted = if (isDark) Color(0xFF94A3B8) else com.mustime.features.timetable.ui.TextMutedLight

            if (isVacantRoomsOpen) {
                BackHandler {
                    isVacantRoomsOpen = false
                }
                com.mustime.features.rooms.ui.VacantRoomsScreen(
                    onBack = { isVacantRoomsOpen = false },
                    enrolledProgramme = savedProgramme
                )
            } else if (isSettingsOpen) {
                BackHandler {
                    isSettingsOpen = false
                }
                SettingsScreen(
                    currentProgramme = savedProgramme ?: "",
                    onReconfigureAcademicProfile = {
                        isSettingsOpen = false
                        navState = AppNavState.FacultySelection
                    },
                    onResetOnboarding = {
                        scope.launch {
                            repository.setOnboardingCompleted(false)
                            isSettingsOpen = false
                            navState = AppNavState.Welcome
                        }
                    },
                    onBack = { isSettingsOpen = false }
                )
            } else {
                BackHandler(enabled = selectedTab != 0) {
                    selectedTab = 0
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    // Main Screen Content (flows edge-to-edge behind floating dock with smooth transitions)
                    Crossfade(
                        targetState = selectedTab,
                        animationSpec = tween(90),
                        label = "main_tab_crossfade",
                        modifier = Modifier.fillMaxSize()
                    ) { tabIndex ->
                        when (tabIndex) {
                            0 -> TimetableRoute(
                                onSettingsClick = { isSettingsOpen = true },
                                onNavigateToNotes = { selectedTab = 2 },
                                onNavigateToCalendar = { selectedTab = 1 },
                                onNavigateToTasks = { selectedTab = 3 },
                                onReconfigureAcademicProfile = {
                                    isSettingsOpen = false
                                    navState = AppNavState.FacultySelection
                                },
                                onOpenVacantRooms = { isVacantRoomsOpen = true }
                            )
                            1 -> CalendarScreen(
                                onBack = null,
                                onSettingsClick = { isSettingsOpen = true },
                                onReconfigureAcademicProfile = {
                                    isSettingsOpen = false
                                    navState = AppNavState.FacultySelection
                                }
                            )
                            2 -> NotesScreen(
                                onBack = null,
                                onSettingsClick = { isSettingsOpen = true },
                                onReconfigureAcademicProfile = {
                                    isSettingsOpen = false
                                    navState = AppNavState.FacultySelection
                                },
                                onEditingChanged = { isNotesEditing = it }
                            )
                            3 -> TasksScreen(
                                onBack = null,
                                onSettingsClick = { isSettingsOpen = true },
                                onReconfigureAcademicProfile = {
                                    isSettingsOpen = false
                                    navState = AppNavState.FacultySelection
                                }
                            )
                        }
                    }

                    // Floating Pill Dock (overlaid above content, letting unoccupied space reveal content behind - hidden when in dedicated Note Editor)
                    if (!isNotesEditing) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(start = 14.dp, end = 14.dp, bottom = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = navBg.copy(alpha = 0.96f),
                            tonalElevation = 6.dp,
                            shadowElevation = 8.dp,
                            border = BorderStroke(1.dp, navBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                tabs.forEachIndexed { index, tab ->
                                    val isSelected = selectedTab == index
                                    val icon = if (isSelected) filledIcons[index] else outlinedIcons[index]

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(20.dp))
                                            .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                selectedTab = index
                                            }
                                            .testTag("nav_${tab.lowercase()}"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(if (isSelected) primaryBlue.copy(alpha = 0.12f) else Color.Transparent)
                                                .padding(horizontal = 12.dp, vertical = 2.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = icon,
                                                    contentDescription = tab,
                                                    tint = if (isSelected) primaryBlue else textMuted,
                                                    modifier = Modifier.size(21.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(1.dp))
                                            Text(
                                                text = tab,
                                                fontSize = 10.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) primaryBlue else textMuted
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                }
            }
        }
    }
}
