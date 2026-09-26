package com.mustime.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
            CircularProgressIndicator(color = Color(0xFF2563EB))
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

            val tabs = listOf("Timetable", "Calendar", "Notes", "Tasks")
            val icons = listOf(
                Icons.Default.DateRange,
                Icons.Default.CalendarMonth,
                Icons.Default.Notes,
                Icons.Default.TaskAlt
            )

            val isDark = com.mustime.ui.LocalAppTheme.current.isDark
            val navBg = if (isDark) com.mustime.features.timetable.ui.DarkSurfaceCard else Color.White
            val navBorder = if (isDark) com.mustime.features.timetable.ui.DarkBorderSubtle else com.mustime.features.timetable.ui.BorderSubtleLight
            val primaryBlue = com.mustime.features.timetable.ui.PrimaryBlue
            val textMuted = if (isDark) Color(0xFF94A3B8) else com.mustime.features.timetable.ui.TextMutedLight

            if (isSettingsOpen) {
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
                Scaffold(
                    contentWindowInsets = WindowInsets.navigationBars,
                    containerColor = MaterialTheme.colorScheme.background,
                    bottomBar = {
                        NavigationBar(
                            containerColor = navBg,
                            tonalElevation = 6.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .drawBehind {
                                    drawLine(
                                        color = navBorder,
                                        start = Offset(0f, 0f),
                                        end = Offset(size.width, 0f),
                                        strokeWidth = 1.dp.toPx()
                                    )
                                }
                        ) {
                            tabs.forEachIndexed { index, tab ->
                                val isSelected = selectedTab == index
                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = { selectedTab = index },
                                    icon = {
                                        Icon(
                                            imageVector = icons[index],
                                            contentDescription = tab,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = tab,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = primaryBlue,
                                        selectedTextColor = primaryBlue,
                                        indicatorColor = primaryBlue.copy(alpha = 0.14f),
                                        unselectedIconColor = textMuted,
                                        unselectedTextColor = textMuted
                                    )
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                        when (selectedTab) {
                            0 -> TimetableRoute(
                                onSettingsClick = { isSettingsOpen = true },
                                onNavigateToNotes = { selectedTab = 2 },
                                onNavigateToCalendar = { selectedTab = 1 },
                                onNavigateToTasks = { selectedTab = 3 }
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
                                }
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
                }
            }
        }
    }
}
