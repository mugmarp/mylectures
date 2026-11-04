package com.lectures.features.calendar

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lectures.TimetableApplication
import com.lectures.core.alarm.AlarmScheduler
import com.lectures.core.alarm.TaskAlarmScheduler
import com.lectures.features.tasks.TaskCard
import com.lectures.features.timetable.domain.ActivityCategory
import com.lectures.features.timetable.domain.Assignment
import com.lectures.features.timetable.domain.CustomEvent
import com.lectures.features.timetable.domain.TimetableEntry
import com.lectures.features.timetable.ui.ActivityCard
import com.lectures.features.timetable.ui.AddActivitySheet
import com.lectures.features.timetable.ui.LectureCard
import com.lectures.features.timetable.ui.LectureDetailSheet
import com.lectures.ui.Chrome
import com.lectures.ui.LocalAppTheme
import com.lectures.ui.components.AcademicProfileSheet
import com.lectures.ui.components.FloatingIslandTopBar
import com.lectures.ui.components.RoomFloorBadge
import com.lectures.ui.rememberLecturesWindow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private sealed class CalendarTimelineItem(val startTime: String) {
    data class Lecture(val entry: TimetableEntry) : CalendarTimelineItem(entry.startTime)
    data class Activity(val event: CustomEvent) : CalendarTimelineItem(event.startTime)
}

@Composable
fun CalendarScreen(
    onBack: (() -> Unit)? = null,
    onSettingsClick: () -> Unit = {},
    onReconfigureAcademicProfile: () -> Unit = {},
    onNavigateToTimetable: () -> Unit = {},
    onPeekProgramme: (String) -> Unit = {},
    onSetOfficialProgramme: (String) -> Unit = {}
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

    val classAlarmScheduler = remember(context) { AlarmScheduler(context) }
    val taskAlarmScheduler = remember(context) { TaskAlarmScheduler(context) }
    val viewModel: CalendarViewModel = viewModel(
        factory = CalendarViewModel.provideFactory(repository, classAlarmScheduler, taskAlarmScheduler)
    )

    val uiState by viewModel.uiState.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()

    LaunchedEffect(userMessage) {
        userMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearUserMessage()
        }
    }

    var showAddActivitySheet by remember { mutableStateOf(false) }
    var activityToEdit by remember { mutableStateOf<CustomEvent?>(null) }
    var showProfileSheet by remember { mutableStateOf(false) }
    var eventToDelete by remember { mutableStateOf<CustomEvent?>(null) }
    var taskToDelete by remember { mutableStateOf<Assignment?>(null) }
    var selectedActivityForDetail by remember { mutableStateOf<CustomEvent?>(null) }
    var selectedLectureForDetail by remember { mutableStateOf<TimetableEntry?>(null) }

    val isDark = LocalAppTheme.current.isDark

    // Contextual lectures: only displayed if instructional day (classes take place)
    val selectedDayLectures = remember(uiState.entries, uiState.selectedDayOfWeek, uiState.academicContext.isInstructionalDay) {
        if (uiState.academicContext.isInstructionalDay) {
            uiState.entries
                .filter { it.dayOfWeek.equals(uiState.selectedDayOfWeek, ignoreCase = true) }
                .sortedBy { it.startTime }
        } else {
            emptyList()
        }
    }

    // Activities for selected day
    val selectedDayActivities = remember(uiState.customEvents, uiState.selectedDayOfWeek) {
        uiState.customEvents
            .filter { it.dayOfWeek.equals(uiState.selectedDayOfWeek, ignoreCase = true) }
            .sortedBy { it.startTime }
    }

    // Deadlines / Assignments due on selected day
    val selectedDayDeadlines = remember(uiState.assignments, uiState.selectedDay, uiState.currentMonth, uiState.currentYear) {
        uiState.assignments.filter { assignment ->
            !assignment.completed && viewModel.isTaskDueOnDay(assignment, uiState.selectedDay)
        }
    }

    // Workload calculation
    val workload = remember(
        uiState.selectedDay,
        uiState.selectedDayOfWeek,
        selectedDayLectures,
        selectedDayActivities,
        selectedDayDeadlines
    ) {
        viewModel.getDayWorkload(
            uiState.selectedDay,
            uiState.selectedDayOfWeek,
            selectedDayLectures,
            selectedDayActivities,
            selectedDayDeadlines
        )
    }

    // Timeline items (Lectures & Activities)
    val timelineItems = remember(
        selectedDayLectures,
        selectedDayActivities,
        uiState.selectedFilter
    ) {
        val list = mutableListOf<CalendarTimelineItem>()
        when (uiState.selectedFilter) {
            CalendarFilter.ALL -> {
                list.addAll(selectedDayLectures.map { CalendarTimelineItem.Lecture(it) })
                list.addAll(selectedDayActivities.map { CalendarTimelineItem.Activity(it) })
            }
            CalendarFilter.LECTURES -> {
                list.addAll(selectedDayLectures.map { CalendarTimelineItem.Lecture(it) })
            }
            CalendarFilter.ACTIVITIES -> {
                list.addAll(selectedDayActivities.map { CalendarTimelineItem.Activity(it) })
            }
            CalendarFilter.TASKS -> {
                // Tasks are handled in the dedicated Deadlines section
            }
        }
        list.sortedBy { it.startTime }
    }

    val windowInfo = rememberLecturesWindow()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val listBottomPadding = Chrome.listBottomPadding(navBarBottom)
    val listState = rememberLazyListState()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            if (uiState.activeTabMode == CalendarTabMode.MONTH_PLANNER) {
                if (windowInfo.collapseFab) {
                    FloatingActionButton(
                        onClick = { showAddActivitySheet = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .navigationBarsPadding()
                            .padding(bottom = Chrome.fabBottomPadding())
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Schedule Activity")
                    }
                } else {
                    ExtendedFloatingActionButton(
                        onClick = { showAddActivitySheet = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = RoundedCornerShape(16.dp),
                        icon = { Icon(Icons.Default.Add, contentDescription = "Schedule Activity") },
                        text = { Text("Schedule Activity", fontWeight = FontWeight.Bold) },
                        modifier = Modifier
                            .navigationBarsPadding()
                            .padding(bottom = Chrome.fabBottomPadding())
                    )
                }
            }
        },
        topBar = {
            FloatingIslandTopBar {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (onBack != null) {
                            IconButton(onClick = onBack) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Outlined.Event,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                        Column {
                            Text(
                                "Academic Calendar",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${uiState.academicContext.semesterLabel} • ${uiState.academicContext.weekLabel}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssistChip(
                            onClick = {
                                viewModel.setTabMode(CalendarTabMode.MONTH_PLANNER)
                                viewModel.selectToday()
                            },
                            label = { Text("Today", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Today,
                                    contentDescription = "Jump to Today",
                                    modifier = Modifier.size(15.dp)
                                )
                            },
                            shape = RoundedCornerShape(12.dp)
                        )

                        IconButton(
                            onClick = { showProfileSheet = true },
                            modifier = Modifier.testTag("topbar_academic_info_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Menu,
                                contentDescription = "Academic Info",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = listBottomPadding
            )
        ) {
            // Screen View Switcher: [Month Planner] vs [Semester Milestones]
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = uiState.activeTabMode == CalendarTabMode.MONTH_PLANNER,
                        onClick = { viewModel.setTabMode(CalendarTabMode.MONTH_PLANNER) },
                        label = { Text("Month Planner", fontWeight = FontWeight.SemiBold) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = uiState.activeTabMode == CalendarTabMode.SEMESTER_MILESTONES,
                        onClick = { viewModel.setTabMode(CalendarTabMode.SEMESTER_MILESTONES) },
                        label = { Text("Semester Milestones", fontWeight = FontWeight.SemiBold) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.FormatListBulleted,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            if (uiState.activeTabMode == CalendarTabMode.SEMESTER_MILESTONES) {
                // SEMESTER MILESTONES VIEW: Full Academic Year Roadmap
                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                        Text(
                            text = "2026/2027 Academic Schedule",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Key university dates, test checkpoints, revision weeks, and official holidays.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                items(uiState.milestones, key = { it.id }) { milestone ->
                    val isLiveNow = remember { milestone.isLiveNow() }
                    val isSelectedDate = milestone.contains(
                        uiState.currentYear,
                        uiState.currentMonth,
                        uiState.selectedDay
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp)
                            .clickable {
                                // Jump directly to milestone start date in Month Planner with accurate year & month
                                viewModel.navigateToDate(
                                    milestone.startYear,
                                    milestone.startMonth,
                                    milestone.startDay
                                )
                                viewModel.setTabMode(CalendarTabMode.MONTH_PLANNER)
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                isLiveNow -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                isSelectedDate -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                else -> MaterialTheme.colorScheme.surface
                            }
                        ),
                        border = when {
                            isLiveNow -> BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                            isSelectedDate -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            else -> null
                        }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = when (milestone.phase) {
                                        AcademicPhase.EXAMINATIONS -> Color(0xFFEF4444).copy(alpha = 0.15f)
                                        AcademicPhase.MID_SEM_TESTS -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                                        AcademicPhase.REVISION -> Color(0xFF8B5CF6).copy(alpha = 0.15f)
                                        AcademicPhase.TEACHING -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        AcademicPhase.ORIENTATION -> Color(0xFF10B981).copy(alpha = 0.15f)
                                        AcademicPhase.RECESS -> Color(0xFF6B7280).copy(alpha = 0.15f)
                                    }
                                ) {
                                    Text(
                                        text = milestone.phase.badgeLabel.uppercase(Locale.ROOT),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (milestone.phase) {
                                            AcademicPhase.EXAMINATIONS -> Color(0xFFEF4444)
                                            AcademicPhase.MID_SEM_TESTS -> Color(0xFFF59E0B)
                                            AcademicPhase.REVISION -> Color(0xFF8B5CF6)
                                            AcademicPhase.TEACHING -> MaterialTheme.colorScheme.primary
                                            AcademicPhase.ORIENTATION -> Color(0xFF10B981)
                                            AcademicPhase.RECESS -> Color(0xFF6B7280)
                                        },
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                if (isLiveNow) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF10B981).copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "Ongoing Now",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF10B981),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                } else if (isSelectedDate) {
                                    Text(
                                        text = "Selected Date",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = milestone.title,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Outlined.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = milestone.dateDisplay,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = milestone.description,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            } else {
                // MONTH PLANNER VIEW: Interactive Month Grid & Day Horizon
                item {
                    // Month Navigation Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.previousMonth() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.ChevronLeft,
                                contentDescription = "Previous Month",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = uiState.yearMonthText,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = uiState.academicContext.phase.displayName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = { viewModel.nextMonth() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = "Next Month",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Days of week header
                    val dayNames = listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        dayNames.forEach { name ->
                            Text(
                                text = name,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Pre-index days with tasks due for O(1) grid dot lookup
                    val taskDaysWithDue = remember(uiState.assignments, uiState.currentMonth, uiState.currentYear) {
                        val days = mutableSetOf<Int>()
                        for (day in 1..uiState.daysInMonth) {
                            if (uiState.assignments.any { !it.completed && viewModel.isTaskDueOnDay(it, day) }) {
                                days.add(day)
                            }
                        }
                        days
                    }

                    val classDaysWithSchedule = remember(uiState.entries) {
                        uiState.entries.map { it.dayOfWeek.lowercase(Locale.ROOT) }.toSet()
                    }

                    val activityDaysWithSchedule = remember(uiState.customEvents) {
                        uiState.customEvents.map { it.dayOfWeek.lowercase(Locale.ROOT) }.toSet()
                    }

                    // Calendar Grid
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        var currentDay = 1
                        for (row in 0..5) {
                            if (currentDay > uiState.daysInMonth) break
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                for (col in 0..6) {
                                    val cellIndex = row * 7 + col
                                    if (cellIndex < uiState.firstDayOfWeekOffset || currentDay > uiState.daysInMonth) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
                                        )
                                    } else {
                                        val dayNum = currentDay
                                        val isSelected = dayNum == uiState.selectedDay
                                        val isToday = viewModel.isToday(dayNum)
                                        val cellDayName = viewModel.getDayOfWeekNameForDay(dayNum)
                                        val cellDayLower = cellDayName.lowercase(Locale.ROOT)

                                        val cellAcademic = uiState.monthAcademicContexts[dayNum] ?: AcademicCalendarEngine.getAcademicContextForDate(
                                            uiState.currentYear,
                                            uiState.currentMonth,
                                            dayNum
                                        )

                                        // Only show lecture indicator if it is an instructional day
                                        val hasClassesThisDay = cellAcademic.isInstructionalDay && (cellDayLower in classDaysWithSchedule)
                                        val hasActivitiesThisDay = cellDayLower in activityDaysWithSchedule
                                        val hasDeadlinesThisDay = dayNum in taskDaysWithDue
                                        val hasMilestoneThisDay = cellAcademic.primaryMilestone != null

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(
                                                    when {
                                                        isSelected -> MaterialTheme.colorScheme.primary
                                                        isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                                    }
                                                )
                                                .then(
                                                    if (isToday && !isSelected) {
                                                        Modifier.border(
                                                            1.5.dp,
                                                            MaterialTheme.colorScheme.primary,
                                                            RoundedCornerShape(10.dp)
                                                        )
                                                    } else Modifier
                                                )
                                                .clickable { viewModel.selectDay(dayNum) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Text(
                                                    text = "$dayNum",
                                                    fontSize = 13.sp,
                                                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                                                    color = when {
                                                        isSelected -> MaterialTheme.colorScheme.onPrimary
                                                        isToday -> MaterialTheme.colorScheme.primary
                                                        else -> MaterialTheme.colorScheme.onSurface
                                                    }
                                                )

                                                // Multi-category indicator dots
                                                if (hasClassesThisDay || hasActivitiesThisDay || hasDeadlinesThisDay || hasMilestoneThisDay) {
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        // Deadlines / Assignments Due (Red/Amber)
                                                        if (hasDeadlinesThisDay) {
                                                            CalendarDot(
                                                                color = if (isSelected) Color.White else Color(0xFFEF4444)
                                                            )
                                                        }
                                                        // Scheduled Lectures (Blue)
                                                        if (hasClassesThisDay) {
                                                            CalendarDot(
                                                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.primary
                                                            )
                                                        }
                                                        // Personal Activities (Green)
                                                        if (hasActivitiesThisDay) {
                                                            CalendarDot(
                                                                color = if (isSelected) Color.White else Color(0xFF10B981)
                                                            )
                                                        }
                                                        // Official University Milestone / Holiday (Purple)
                                                        if (hasMilestoneThisDay && !hasClassesThisDay && !hasDeadlinesThisDay) {
                                                            CalendarDot(
                                                                color = if (isSelected) Color.White else Color(0xFF8B5CF6)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        currentDay++
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Selected Day Academic Horizon Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "${uiState.selectedDayOfWeek}, ${SimpleDateFormat("MMM", Locale.getDefault()).format(Calendar.getInstance().apply { set(Calendar.MONTH, uiState.currentMonth) }.time)} ${uiState.selectedDay}",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${uiState.academicContext.semesterLabel} • ${uiState.academicContext.weekLabel}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { viewModel.previousDay() },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ChevronLeft,
                                            contentDescription = "Previous Day",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.nextDay() },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ChevronRight,
                                            contentDescription = "Next Day",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // If official milestone or holiday on this day, display official notice
                            uiState.academicContext.primaryMilestone?.let { milestone ->
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (milestone.isHoliday) {
                                        Color(0xFFEF4444).copy(alpha = 0.12f)
                                    } else {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Flag,
                                            contentDescription = null,
                                            tint = if (milestone.isHoliday) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = milestone.title,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (milestone.isHoliday) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = milestone.description,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Day Workload Diagnostic Pills
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                WorkloadPill(
                                    label = "Classes",
                                    value = if (workload.lectureCount > 0) "${workload.lectureCount} (${workload.totalHoursText})" else "None",
                                    icon = Icons.Outlined.School,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f)
                                )

                                WorkloadPill(
                                    label = "Deadlines",
                                    value = if (workload.deadlineCount > 0) "${workload.deadlineCount} Due" else "None",
                                    icon = Icons.Outlined.TaskAlt,
                                    color = if (workload.deadlineCount > 0) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )

                                WorkloadPill(
                                    label = "Activities",
                                    value = if (workload.activityCount > 0) "${workload.activityCount}" else "0",
                                    icon = Icons.Outlined.Schedule,
                                    color = Color(0xFF10B981),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Filter Chips Row
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(CalendarFilter.entries.toTypedArray()) { filter ->
                            val count = when (filter) {
                                CalendarFilter.ALL -> selectedDayLectures.size + selectedDayActivities.size + selectedDayDeadlines.size
                                CalendarFilter.LECTURES -> selectedDayLectures.size
                                CalendarFilter.ACTIVITIES -> selectedDayActivities.size
                                CalendarFilter.TASKS -> selectedDayDeadlines.size
                            }
                            FilterChip(
                                selected = uiState.selectedFilter == filter,
                                onClick = { viewModel.setFilter(filter) },
                                label = { Text("${filter.label} ($count)") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }

                // DEADLINES SECTION (Prioritized above lectures so students never miss due coursework)
                if (selectedDayDeadlines.isNotEmpty() && (uiState.selectedFilter == CalendarFilter.ALL || uiState.selectedFilter == CalendarFilter.TASKS)) {
                    item {
                        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    color = Color(0xFFEF4444),
                                    shape = CircleShape,
                                    modifier = Modifier.size(8.dp)
                                ) {}
                                Text(
                                    text = "Deadlines Due Today",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEF4444)
                                )
                            }
                        }
                    }

                    items(selectedDayDeadlines, key = { "deadline_${it.id}" }) { task ->
                        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                            TaskCard(
                                task = task,
                                isDark = isDark,
                                onToggleComplete = { viewModel.toggleTaskComplete(task) },
                                onEdit = { },
                                onDelete = { taskToDelete = task }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                // If non-instructional day and lectures were requested, explain why no classes
                if (!uiState.academicContext.isInstructionalDay && (uiState.selectedFilter == CalendarFilter.ALL || uiState.selectedFilter == CalendarFilter.LECTURES)) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.School,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "No Regular Lectures Scheduled",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${uiState.academicContext.phase.displayName}. University instructional timetable is paused during this period.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // CHRONOLOGICAL TIMELINE (Lectures & Activities)
                if (timelineItems.isEmpty() && (selectedDayDeadlines.isEmpty() || uiState.selectedFilter != CalendarFilter.ALL)) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(28.dp)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Event,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "No commitments scheduled for ${uiState.selectedDayOfWeek}",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tap 'Schedule Activity' below to plan a revision session or lab prep",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(timelineItems, key = { item ->
                        when (item) {
                            is CalendarTimelineItem.Lecture -> "lec_${item.entry.naturalKey}"
                            is CalendarTimelineItem.Activity -> "act_${item.event.id}"
                        }
                    }) { item ->
                        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                            when (item) {
                                is CalendarTimelineItem.Lecture -> {
                                    LectureCard(
                                        entry = item.entry,
                                        onClick = { selectedLectureForDetail = item.entry }
                                    )
                                }
                                is CalendarTimelineItem.Activity -> {
                                    ActivityCard(
                                        event = item.event,
                                        onClick = { selectedActivityForDetail = item.event },
                                        onEdit = { activityToEdit = item.event },
                                        onDelete = { eventToDelete = item.event }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Activity Detail Dialog
    selectedActivityForDetail?.let { event ->
        AlertDialog(
            onDismissRequest = { selectedActivityForDetail = null },
            title = {
                Text(
                    text = event.title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Category: ${event.category}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Time: ${event.dayOfWeek} · ${event.startTime} - ${event.endTime}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (event.location.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Venue: ${event.location}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            RoomFloorBadge(roomName = event.location, compact = true)
                        }
                    }
                    if (event.notes.isNotBlank()) {
                        Text(
                            text = "Notes: ${event.notes}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = {
                        val toEdit = event
                        selectedActivityForDetail = null
                        activityToEdit = toEdit
                    }) {
                        Text("Edit", fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = { selectedActivityForDetail = null }) {
                        Text("Close")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        val toDel = event
                        selectedActivityForDetail = null
                        eventToDelete = toDel
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Activity")
                }
            }
        )
    }

    if (activityToEdit != null) {
        AddActivitySheet(
            eventToEdit = activityToEdit,
            onDismiss = { activityToEdit = null },
            onSave = { updatedEvent ->
                viewModel.addCustomEvent(updatedEvent)
                activityToEdit = null
            }
        )
    }

    // Lecture Detail & Notes Bottom Sheet
    if (selectedLectureForDetail != null) {
        val entry = selectedLectureForDetail!!
        LectureDetailSheet(
            entry = entry,
            onSaveNote = { noteText, reminderMinutes ->
                viewModel.saveNote(entry, noteText, reminderMinutes)
                selectedLectureForDetail = null
            },
            onDismiss = { selectedLectureForDetail = null }
        )
    }

    // Add Activity Bottom Sheet
    if (showAddActivitySheet) {
        AddActivitySheet(
            initialDayOfWeek = uiState.selectedDayOfWeek,
            onDismiss = { showAddActivitySheet = false },
            onSave = { newEvent ->
                viewModel.addCustomEvent(newEvent)
            }
        )
    }

    // Academic Profile Sheet
    if (showProfileSheet) {
        val enrolledProg = repository.getInitialProgramme() ?: "EEE II"
        AcademicProfileSheet(
            programme = uiState.currentProgramme,
            enrolledProgramme = enrolledProg,
            onDismiss = { showProfileSheet = false },
            onPeekProgramme = { peekGroup ->
                showProfileSheet = false
                onPeekProgramme(peekGroup)
                onNavigateToTimetable()
            },
            onSetOfficialProgramme = { newGroup ->
                showProfileSheet = false
                onSetOfficialProgramme(newGroup)
                onNavigateToTimetable()
            },
            onReconfigureProfile = {
                showProfileSheet = false
                onReconfigureAcademicProfile()
            },
            onOpenSettings = {
                showProfileSheet = false
                onSettingsClick()
            },
            onNavigateToTimetable = {
                showProfileSheet = false
                onNavigateToTimetable()
            }
        )
    }

    // Delete Confirmation Dialog for Activity
    eventToDelete?.let { event ->
        AlertDialog(
            onDismissRequest = { eventToDelete = null },
            title = { Text("Delete Activity") },
            text = { Text("Are you sure you want to delete \"${event.title}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCustomEvent(event.id)
                        eventToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { eventToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Confirmation Dialog for Task
    taskToDelete?.let { task ->
        AlertDialog(
            onDismissRequest = { taskToDelete = null },
            title = { Text("Delete Deadline") },
            text = { Text("Are you sure you want to delete \"${task.title}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTask(task)
                        taskToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { taskToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun WorkloadPill(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = label,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CalendarDot(color: Color) {
    Box(
        modifier = Modifier
            .size(5.dp)
            .background(color, CircleShape)
    )
}
