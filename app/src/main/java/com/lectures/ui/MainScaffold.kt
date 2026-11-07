package com.lectures.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.zIndex
import com.lectures.core.notification.InAppNotificationItem
import com.lectures.core.notification.InAppNotificationStore
import com.lectures.core.notification.NotificationItemType
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
import com.lectures.TimetableApplication
import com.lectures.features.calendar.CalendarScreen
import com.lectures.features.notes.NotesScreen
import com.lectures.features.onboarding.*
import com.lectures.features.settings.SettingsScreen
import com.lectures.features.tasks.TasksScreen
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateColorAsState
import com.lectures.features.timetable.ui.TimetableRoute
import com.lectures.features.timetable.ui.WelcomeScreen
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

enum class MainScreenDestination {
    TABS,
    SETTINGS,
    VACANT_ROOMS,
    ACCOUNT
}

sealed class AppNavState {
    data object Welcome : AppNavState()
    data object FacultySelection : AppNavState()
    data class ProgrammeSelection(val faculty: Faculty) : AppNavState()
    data class ClassGroupSelection(val faculty: Faculty, val programme: Programme) : AppNavState()
    data object MainApp : AppNavState()
}

@Composable
fun MainScaffold(
    initialTab: Int = 0,
    notificationIntentFlow: SharedFlow<Pair<Int, Long?>>? = null
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
    var selectedTab by remember { mutableIntStateOf(initialTab) }
    var isNotesEditing by remember { mutableStateOf(false) }
    var pendingHighlightTaskId by remember { mutableStateOf<Long?>(null) }

    // Handle notification deep-link intents.
    LaunchedEffect(notificationIntentFlow) {
        notificationIntentFlow?.collect { (tab, taskId) ->
            if (navState is AppNavState.MainApp) {
                selectedTab = tab
                pendingHighlightTaskId = taskId
            }
        }
    }

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
            val liveCohortCounts by repository.getCohortCounts(state.programme.code).collectAsState(initial = emptyMap())
            val calculatedCounts = remember(state.programme, allEntries, liveCohortCounts) {
                state.programme.allGroups.associateWith { g ->
                    com.lectures.features.timetable.domain.TimetableMatcher.getGroupCount(
                        group = g,
                        entries = allEntries,
                        cohortCounts = liveCohortCounts
                    )
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
                        com.lectures.core.alarm.ReminderScheduler(context).scheduleDailyReminder()
                        val enrolledClasses = repository.getLocalSchedule(chosenGroup).firstOrNull() ?: emptyList()
                        if (enrolledClasses.isNotEmpty() && repository.etagStore.isNotificationsEnabled()) {
                            val leadMinutes = repository.etagStore.getClassAlarmLeadMinutes()
                            com.lectures.core.alarm.AlarmScheduler(context).scheduleAllEnrolledClassAlarms(enrolledClasses, leadMinutes)
                        }
                        navState = AppNavState.MainApp
                    }
                },
                onBack = { navState = AppNavState.ProgrammeSelection(state.faculty) }
            )
        }
        is AppNavState.MainApp -> {
            var currentDestination by remember { mutableStateOf(MainScreenDestination.TABS) }

            val inAppStore = remember(context) { InAppNotificationStore(context) }
            var activeForegroundAlert by remember { mutableStateOf<InAppNotificationItem?>(null) }

            LaunchedEffect(Unit) {
                InAppNotificationStore.foregroundAlertFlow.collect { alert ->
                    activeForegroundAlert = alert
                }
            }

            LaunchedEffect(activeForegroundAlert) {
                if (activeForegroundAlert != null) {
                    kotlinx.coroutines.delay(5000)
                    activeForegroundAlert = null
                }
            }

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

            val isDark = com.lectures.ui.LocalAppTheme.current.isDark
            val navBg = if (isDark) com.lectures.features.timetable.ui.DarkSurfaceCard else Color.White
            val navBorder = if (isDark) com.lectures.features.timetable.ui.DarkBorderSubtle else com.lectures.features.timetable.ui.BorderSubtleLight
            val primaryBlue = com.lectures.features.timetable.ui.PrimaryBlue
            val textMuted = if (isDark) Color(0xFF94A3B8) else com.lectures.features.timetable.ui.TextMutedLight

            val classAlarmScheduler = remember(context) { com.lectures.core.alarm.AlarmScheduler(context) }
            val taskAlarmScheduler = remember(context) { com.lectures.core.alarm.TaskAlarmScheduler(context) }
            val timetableViewModel: com.lectures.features.timetable.ui.TimetableViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = com.lectures.features.timetable.ui.TimetableViewModel.provideFactory(repository, taskAlarmScheduler, classAlarmScheduler)
            )

            val onPeekProgrammeAndGoToTimetable: (String) -> Unit = { group ->
                timetableViewModel.peekProgramme(group)
                currentDestination = MainScreenDestination.TABS
                selectedTab = 0
            }
            val onSetOfficialProgrammeAndGoToTimetable: (String) -> Unit = { group ->
                timetableViewModel.setOfficialProgramme(group)
                currentDestination = MainScreenDestination.TABS
                selectedTab = 0
            }

            // BackHandler per screen destination
            BackHandler(enabled = currentDestination != MainScreenDestination.TABS) {
                currentDestination = MainScreenDestination.TABS
            }
            BackHandler(enabled = currentDestination == MainScreenDestination.TABS && selectedTab != 0) {
                selectedTab = 0
            }

            AnimatedContent(
                targetState = currentDestination,
                transitionSpec = {
                    if (targetState != MainScreenDestination.TABS) {
                        (slideInHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { it } +
                                fadeIn(animationSpec = tween(220)))
                            .togetherWith(
                                slideOutHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { -it / 3 } +
                                        fadeOut(animationSpec = tween(150))
                            )
                    } else {
                        (slideInHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { -it / 3 } +
                                fadeIn(animationSpec = tween(220)))
                            .togetherWith(
                                slideOutHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { it } +
                                        fadeOut(animationSpec = tween(150))
                            )
                    }
                },
                label = "SubScreenNavigation"
            ) { destination ->
                when (destination) {
                    MainScreenDestination.VACANT_ROOMS -> {
                        com.lectures.features.rooms.ui.VacantRoomsScreen(
                            onBack = { currentDestination = MainScreenDestination.TABS },
                            enrolledProgramme = savedProgramme
                        )
                    }
                    MainScreenDestination.ACCOUNT -> {
                        com.lectures.features.account.AccountScreen(
                            onBack = { currentDestination = MainScreenDestination.TABS }
                        )
                    }
                    MainScreenDestination.SETTINGS -> {
                        SettingsScreen(
                            currentProgramme = savedProgramme ?: "",
                            onReconfigureAcademicProfile = {
                                currentDestination = MainScreenDestination.TABS
                                navState = AppNavState.FacultySelection
                            },
                            onOpenAccount = {
                                currentDestination = MainScreenDestination.ACCOUNT
                            },
                            onResetOnboarding = {
                                scope.launch {
                                    repository.setOnboardingCompleted(false)
                                    currentDestination = MainScreenDestination.TABS
                                    navState = AppNavState.Welcome
                                }
                            },
                            onBack = { currentDestination = MainScreenDestination.TABS }
                        )
                    }
                    MainScreenDestination.TABS -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background)
                        ) {
                            // Fluid animated tab switching along navigation direction
                            AnimatedContent(
                                targetState = selectedTab,
                                transitionSpec = {
                                    val direction = if (targetState > initialState) 1 else -1
                                    (slideInHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { width -> (direction * width * 0.14f).toInt() } +
                                            fadeIn(animationSpec = tween(180)))
                                        .togetherWith(
                                            slideOutHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { width -> (-direction * width * 0.14f).toInt() } +
                                                    fadeOut(animationSpec = tween(120))
                                        )
                                },
                                label = "MainTabsTransition"
                            ) { tabIndex ->
                                when (tabIndex) {
                                    0 -> TimetableRoute(
                                        onSettingsClick = { currentDestination = MainScreenDestination.SETTINGS },
                                        onNavigateToNotes = { selectedTab = 2 },
                                        onNavigateToCalendar = { selectedTab = 1 },
                                        onNavigateToTasks = { selectedTab = 3 },
                                        onReconfigureAcademicProfile = {
                                            currentDestination = MainScreenDestination.SETTINGS
                                            navState = AppNavState.FacultySelection
                                        },
                                        onOpenVacantRooms = { currentDestination = MainScreenDestination.VACANT_ROOMS }
                                    )
                                    1 -> CalendarScreen(
                                        onBack = null,
                                        onSettingsClick = { currentDestination = MainScreenDestination.SETTINGS },
                                        onReconfigureAcademicProfile = {
                                            currentDestination = MainScreenDestination.SETTINGS
                                            navState = AppNavState.FacultySelection
                                        },
                                        onNavigateToTimetable = { selectedTab = 0 },
                                        onPeekProgramme = onPeekProgrammeAndGoToTimetable,
                                        onSetOfficialProgramme = onSetOfficialProgrammeAndGoToTimetable
                                    )
                                    2 -> NotesScreen(
                                        onBack = null,
                                        onSettingsClick = { currentDestination = MainScreenDestination.SETTINGS },
                                        onReconfigureAcademicProfile = {
                                            currentDestination = MainScreenDestination.SETTINGS
                                            navState = AppNavState.FacultySelection
                                        },
                                        onNavigateToTimetable = { selectedTab = 0 },
                                        onPeekProgramme = onPeekProgrammeAndGoToTimetable,
                                        onSetOfficialProgramme = onSetOfficialProgrammeAndGoToTimetable,
                                        onEditingChanged = { isNotesEditing = it }
                                    )
                                    3 -> TasksScreen(
                                        onBack = null,
                                        onSettingsClick = { currentDestination = MainScreenDestination.SETTINGS },
                                        onReconfigureAcademicProfile = {
                                            currentDestination = MainScreenDestination.SETTINGS
                                            navState = AppNavState.FacultySelection
                                        },
                                        onNavigateToTimetable = { selectedTab = 0 },
                                        onPeekProgramme = onPeekProgrammeAndGoToTimetable,
                                        onSetOfficialProgramme = onSetOfficialProgrammeAndGoToTimetable,
                                        highlightTaskId = pendingHighlightTaskId
                                    )
                                }
                            }

                            // Real-time Foreground Heads-Up Alert Toast Banner
                            AnimatedVisibility(
                                visible = activeForegroundAlert != null,
                                enter = slideInVertically(
                                    initialOffsetY = { -it },
                                    animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium)
                                ) + fadeIn(),
                                exit = slideOutVertically(
                                    targetOffsetY = { -it },
                                    animationSpec = tween(180)
                                ) + fadeOut(),
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .statusBarsPadding()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .zIndex(100f)
                            ) {
                                activeForegroundAlert?.let { alert ->
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = if (isDark) com.lectures.features.timetable.ui.DarkSurfaceCard else Color.White,
                                        border = BorderStroke(1.dp, primaryBlue.copy(alpha = 0.4f)),
                                        shadowElevation = 8.dp,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                alert.navTab?.let { selectedTab = it }
                                                activeForegroundAlert = null
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .background(primaryBlue.copy(alpha = 0.15f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.NotificationsActive,
                                                    contentDescription = null,
                                                    tint = primaryBlue,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = alert.title,
                                                    fontSize = 13.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isDark) Color.White else Color(0xFF0F172A),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = alert.message,
                                                    fontSize = 12.sp,
                                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            IconButton(
                                                onClick = { activeForegroundAlert = null },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Dismiss",
                                                    tint = textMuted,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            val windowInfo = rememberLecturesWindow()

                            // Floating Pill Dock with animated pill selection and icon spring scale
                            if (!isNotesEditing && !windowInfo.isImeVisible) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .navigationBarsPadding()
                                        .padding(start = Chrome.DockSideInset, end = Chrome.DockSideInset, bottom = Chrome.DockBottomGap),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(24.dp),
                                        color = navBg.copy(alpha = 0.96f),
                                        tonalElevation = 6.dp,
                                        shadowElevation = 8.dp,
                                        border = BorderStroke(1.dp, navBorder),
                                        modifier = Modifier
                                            .widthIn(max = 440.dp)
                                            .fillMaxWidth()
                                            .height(Chrome.DockHeight)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 4.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceEvenly,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            tabs.forEachIndexed { index, tab ->
                                                val isSelected = selectedTab == index
                                                val icon = if (isSelected) filledIcons[index] else outlinedIcons[index]

                                                val pillColor by animateColorAsState(
                                                    targetValue = if (isSelected) primaryBlue.copy(alpha = 0.14f) else Color.Transparent,
                                                    animationSpec = tween(180),
                                                    label = "pill_color"
                                                )
                                                val iconScale by animateFloatAsState(
                                                    targetValue = if (isSelected) 1.08f else 1.0f,
                                                    animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMedium),
                                                    label = "icon_scale"
                                                )

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
                                                                .background(pillColor)
                                                                .padding(horizontal = if (windowInfo.dockIconOnly) 16.dp else 10.dp, vertical = if (windowInfo.dockIconOnly) 6.dp else 2.dp),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(
                                                                imageVector = icon,
                                                                contentDescription = tab,
                                                                tint = if (isSelected) primaryBlue else textMuted,
                                                                modifier = Modifier
                                                                    .size(if (windowInfo.dockIconOnly) 24.dp else 21.dp)
                                                                    .graphicsLayer {
                                                                        scaleX = iconScale
                                                                        scaleY = iconScale
                                                                    }
                                                            )
                                                        }
                                                        if (!windowInfo.dockIconOnly) {
                                                            Spacer(modifier = Modifier.height(1.dp))
                                                            Text(
                                                                text = tab,
                                                                fontSize = 10.sp,
                                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                                color = if (isSelected) primaryBlue else textMuted,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
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
        }
    }
}

