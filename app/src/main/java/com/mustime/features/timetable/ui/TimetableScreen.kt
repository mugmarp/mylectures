package com.mustime.features.timetable.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mustime.core.util.SessionProgress
import com.mustime.core.util.TimeUtil
import com.mustime.features.timetable.domain.Assignment
import com.mustime.features.timetable.domain.CustomEvent
import com.mustime.features.timetable.domain.LectureNote
import com.mustime.features.timetable.domain.TimetableEntry
import com.mustime.ui.LocalAppTheme
import com.mustime.ui.components.AcademicProfileSheet
import com.mustime.ui.components.NotificationCenterSheet
import com.mustime.ui.components.RoomFloorBadge
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Helper for Real-Time Global Next Up session
data class GlobalNextUp(
    val entry: TimetableEntry,
    val isLive: Boolean,
    val progress: SessionProgress?,
    val minutesUntil: Int,
    val dayLabel: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(
    programme: String,
    entries: List<TimetableEntry>,
    customEvents: List<CustomEvent> = emptyList(),
    assignments: List<Assignment> = emptyList(),
    notes: Map<String, LectureNote> = emptyMap(),
    selectedDay: String,
    isLoading: Boolean = false,
    onDaySelected: (String) -> Unit,
    onSaveNote: (TimetableEntry, String, Int?) -> Unit = { _, _, _ -> },
    onAddActivity: (CustomEvent) -> Unit = {},
    onDeleteActivity: (Long) -> Unit = {},
    onAddTask: (title: String, courseCode: String, dueDate: String, priority: String, reminderMinutes: Int?, notes: String) -> Unit = { _, _, _, _, _, _ -> },
    onToggleTaskComplete: (Assignment) -> Unit = {},
    onDeleteTask: (Long) -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onReconfigureAcademicProfile: () -> Unit = {},
    onNavigateToNotes: () -> Unit = {},
    onNavigateToCalendar: () -> Unit = {},
    onNavigateToTasks: () -> Unit = {},
    onOpenVacantRooms: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    val isDark = LocalAppTheme.current.isDark
    val primaryColor = MaterialTheme.colorScheme.primary

    // Dynamic current real-time day and calendar
    val todayName = remember { TimeUtil.todayName() }
    val daysList = remember { TimeUtil.DAYS }

    // Dialog & BottomSheet state
    var selectedEntryForDetail by remember { mutableStateOf<TimetableEntry?>(null) }
    var selectedEntryForAlarm by remember { mutableStateOf<TimetableEntry?>(null) }
    var showQuickAddSheet by remember { mutableStateOf(false) }
    var showAcademicProfileSheet by remember { mutableStateOf(false) }
    var selectedActivityForDetail by remember { mutableStateOf<CustomEvent?>(null) }
    var activityToEdit by remember { mutableStateOf<CustomEvent?>(null) }
    var selectedFilter by remember { mutableStateOf("All") }

    // 1. Compute day dates for the current week (Monday through Sunday)
    val dayInfoList = remember {
        val now = Calendar.getInstance()
        val currentDayOfWeek = now.get(Calendar.DAY_OF_WEEK) // Sunday=1, Monday=2
        val daysFromMonday = when (currentDayOfWeek) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }
        val mondayCal = (now.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, -daysFromMonday)
        }
        val dateFormat = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())

        daysList.mapIndexed { index, dayName ->
            val dayCal = (mondayCal.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, index)
            }
            Triple(dayName, dayCal.get(Calendar.DAY_OF_MONTH), dateFormat.format(dayCal.time))
        }
    }

    // Real-time clock pulse to re-evaluate next-up, countdowns, and progress bars periodically
    var timeTick by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(15_000L)
            timeTick = System.currentTimeMillis()
        }
    }

    // 2. Compute TRUE Global Real-Time Next Up (Consistent across ALL day views)
    val globalNextUp: GlobalNextUp? = remember(entries, todayName, timeTick) {
        findActualNextUp(entries, todayName)
    }

    // 3. Filter sessions for the selected day from the real Room database
    val dayEntries = remember(entries, selectedDay, selectedFilter) {
        val onDay = entries.filter { it.dayOfWeek.equals(selectedDay, ignoreCase = true) }
        val filtered = when (selectedFilter) {
            "Lectures" -> onDay.filter {
                val label = resolveSessionTypeLabel(it)
                label == "Lecture" || label == "Clinical"
            }
            "Labs" -> onDay.filter {
                val label = resolveSessionTypeLabel(it)
                label == "Lab" || label == "Practical"
            }
            "Associations" -> onDay.filter {
                resolveSessionTypeLabel(it) == "Student Association"
            }
            else -> onDay
        }
        filtered.sortedBy { TimeUtil.toMinutes(it.startTime) }
    }

    val dayCustomEvents = remember(customEvents, selectedDay, selectedFilter) {
        val onDay = customEvents.filter { it.dayOfWeek.equals(selectedDay, ignoreCase = true) }
        val filtered = when (selectedFilter) {
            "Lectures", "Labs" -> emptyList()
            else -> onDay
        }
        filtered.sortedBy { TimeUtil.toMinutes(it.startTime) }
    }

    // Selected day info
    val isViewingToday = selectedDay.equals(todayName, ignoreCase = true)
    val selectedDayInfo = dayInfoList.firstOrNull { it.first.equals(selectedDay, ignoreCase = true) }
    val formattedSelectedDate = selectedDayInfo?.third ?: selectedDay

    val todayEntries = remember(entries, todayName) {
        entries.filter { it.dayOfWeek.equals(todayName, ignoreCase = true) }
            .sortedBy { TimeUtil.toMinutes(it.startTime) }
    }
    val todayDateFormatted = remember(dayInfoList, todayName) {
        dayInfoList.firstOrNull { it.first.equals(todayName, ignoreCase = true) }?.third
            ?: SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date())
    }

    val allTodayClassesCompleted = remember(todayEntries, timeTick) {
        if (todayEntries.isEmpty()) false
        else {
            val now = Calendar.getInstance()
            val curMin = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
            todayEntries.all { entry ->
                val endMin = TimeUtil.toMinutes(entry.endTime)
                endMin > 0 && endMin <= curMin
            }
        }
    }

    // Horizontal drag gesture tracking for swiping between days
    var dragTotal by remember { mutableFloatStateOf(0f) }
    var showNotificationCenterSheet by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = if (isDark) DarkSurfaceBase else SurfaceBaseLight,
        topBar = {
            TimetableTopAppBar(
                program = programme,
                subtitle = if (programme.isNotBlank()) "Academic Schedule" else "Select Programme",
                hasUnreadNotifications = false,
                onNotificationClick = {
                    showNotificationCenterSheet = true
                    onNotificationClick()
                },
                onProfileClick = {
                    showAcademicProfileSheet = true
                },
                onSettingsClick = onSettingsClick,
                onOpenVacantRooms = onOpenVacantRooms
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showQuickAddSheet = true },
                containerColor = PrimaryBlue,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                icon = {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                },
                text = {
                    Text(
                        text = "Add Activity",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                },
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 96.dp)
                    .testTag("timetable_fab")
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 150.dp)
            ) {
                // Prompt to select programme if not yet chosen
                if (programme.isBlank()) {
                    item(key = "no_prog_prompt") {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = PrimaryBlue.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.25f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.School,
                                    contentDescription = null,
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "No Programme Selected",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = if (isDark) Color.White else TextPrimaryLight
                                    )
                                    Text(
                                        text = "Select your programme to view your class timetable.",
                                        fontSize = 12.sp,
                                        color = if (isDark) Color(0xFF94A3B8) else TextMutedLight
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = onSettingsClick,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("Select", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // SECTION 1: TODAY'S SCHEDULE HEADER (ABOVE THE DAYS)
                item(key = "today_schedule_header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Today's Schedule",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else TextPrimaryLight
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = todayDateFormatted,
                                fontSize = 13.sp,
                                color = if (isDark) Color(0xFF94A3B8) else TextMutedLight,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if (todayEntries.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = PrimaryBlue.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "${todayEntries.size} ${if (todayEntries.size == 1) "session" else "sessions"}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryBlue,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // SECTION 2: HAPPENING NOW / NEXT UP HERO CARD (ABOVE THE DAYS)
                if (allTodayClassesCompleted && globalNextUp != null && !globalNextUp.isLive) {
                    item(key = "today_completed_badge") {
                        PaddingBox(top = 4.dp, bottom = 2.dp) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.25f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "All classes for today are completed",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF10B981)
                                    )
                                }
                            }
                        }
                    }
                }

                if (globalNextUp != null) {
                    item(key = "today_hero_card") {
                        val entry = globalNextUp.entry
                        val note = notes[entry.naturalKey]

                        if (globalNextUp.isLive) {
                            val session = entry.toSpecClassSession(
                                note = note,
                                isLive = true,
                                progress = globalNextUp.progress
                            )
                            HappeningNowHeroCard(
                                session = session,
                                progress = session.progress,
                                elapsedText = session.elapsedText,
                                remainingText = session.remainingText,
                                onQuickNote = { selectedEntryForDetail = entry },
                                onAlarmDismiss = {
                                    selectedEntryForAlarm = entry
                                },
                                onClick = { selectedEntryForDetail = entry },
                                modifier = Modifier.padding(top = 6.dp, bottom = 6.dp)
                            )
                        } else {
                            val countdownText = when {
                                globalNextUp.dayLabel.equals("Today", ignoreCase = true) -> {
                                    when {
                                        globalNextUp.minutesUntil in 1..59 -> "in ${globalNextUp.minutesUntil}m"
                                        globalNextUp.minutesUntil >= 60 -> "in ${globalNextUp.minutesUntil / 60}h ${globalNextUp.minutesUntil % 60}m"
                                        else -> "Starting soon"
                                    }
                                }
                                else -> "${globalNextUp.dayLabel} at ${entry.startTime}"
                            }

                            val session = entry.toSpecClassSession(
                                note = note,
                                isLive = false,
                                startsInText = countdownText
                            )
                            NextUpHeroCard(
                                session = session,
                                onSetAlarm = {
                                    selectedEntryForAlarm = entry
                                },
                                onQuickNote = { selectedEntryForDetail = entry },
                                onClick = { selectedEntryForDetail = entry },
                                modifier = Modifier.padding(top = 6.dp, bottom = 6.dp)
                            )
                        }
                    }
                } else if (todayEntries.isEmpty() && programme.isNotBlank() && !isLoading) {
                    item(key = "no_classes_today_card") {
                        PaddingBox(top = 8.dp, bottom = 6.dp) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isDark) DarkSurfaceCard else Color.White,
                                border = BorderStroke(1.dp, if (isDark) DarkBorderSubtle else BorderSubtleLight),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(PrimaryBlue.copy(alpha = 0.1f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.EventAvailable,
                                            contentDescription = null,
                                            tint = PrimaryBlue,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "No classes scheduled for today",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            color = if (isDark) Color.White else TextPrimaryLight
                                        )
                                        Text(
                                            text = "Take time for self-study or add personal activities.",
                                            fontSize = 12.sp,
                                            color = if (isDark) Color(0xFF94A3B8) else TextMutedLight
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // SECTION 3: 7-DAY CAPSULE SELECTOR (MON - SUN) (PLACED DIRECTLY BELOW TODAY'S SCHEDULE & CARD)
                item(key = "day_capsules") {
                    val specDays = remember(daysList, selectedDay, todayName) {
                        daysList.map { dayName ->
                            SpecDayItem(
                                name = dayName,
                                isToday = dayName.equals(todayName, ignoreCase = true),
                                isSelected = dayName.equals(selectedDay, ignoreCase = true)
                            )
                        }
                    }
                    DayCapsuleStrip(
                        dayItems = specDays,
                        onDaySelected = { onDaySelected(it) },
                        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                    )
                }

                // SECTION 4: SELECTED DAY SESSIONS HEADER
                item(key = "schedule_header") {
                    val filterLabel = when (selectedFilter) {
                        "All" -> "All Sessions"
                        "Lectures" -> "Lectures"
                        "Labs" -> "Labs"
                        "Associations" -> "Associations"
                        else -> selectedFilter
                    }
                    ScheduleHeaderSection(
                        title = "$selectedDay's Schedule",
                        dateText = formattedSelectedDate,
                        activeFilter = filterLabel,
                        onFilterClicked = {
                            selectedFilter = when (selectedFilter) {
                                "All" -> "Lectures"
                                "Lectures" -> "Labs"
                                "Labs" -> "Associations"
                                else -> "All"
                            }
                        },
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                // SECTION 4: REAL TIMETABLE SESSIONS & CUSTOM ACTIVITIES
                if (isLoading) {
                    item(key = "loading_state") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = PrimaryBlue)
                        }
                    }
                } else if (dayEntries.isEmpty() && dayCustomEvents.isEmpty()) {
                    item(key = "empty_state") {
                        PaddingBox(top = 24.dp, bottom = 24.dp) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isDark) DarkSurfaceCard else Color.White,
                                border = BorderStroke(1.dp, if (isDark) DarkBorderSubtle else BorderSubtleLight),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .background(PrimaryBlue.copy(alpha = 0.1f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.EventAvailable,
                                            contentDescription = null,
                                            tint = PrimaryBlue,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        text = "No classes scheduled for $selectedDay",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = if (isDark) Color.White else TextPrimaryLight
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Take time to study, revise, or add a custom study session.",
                                        fontSize = 12.sp,
                                        color = if (isDark) Color(0xFF94A3B8) else TextMutedLight,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(18.dp))
                                    Button(
                                        onClick = { showQuickAddSheet = true },
                                        shape = ActionButtonShape,
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Add Custom Activity", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Render real lectures with TimelineClassCard from specification
                    items(dayEntries, key = { it.naturalKey.ifBlank { "${it.courseCode}_${it.startTime}_${it.dayOfWeek}" } }) { entry ->
                        val entryNote = notes[entry.naturalKey]
                        val specSession = entry.toSpecClassSession(
                            note = entryNote,
                            startsInText = if (isViewingToday) {
                                val now = Calendar.getInstance()
                                val curMin = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
                                val startMin = TimeUtil.toMinutes(entry.startTime)
                                val diff = startMin - curMin
                                if (diff in 1..120) "in ${diff}m" else null
                            } else null
                        )
                        TimelineClassCard(
                            session = specSession,
                            onToggleAlarm = {
                                selectedEntryForAlarm = entry
                            },
                            onAttachedNoteClick = {
                                selectedEntryForDetail = entry
                            },
                            onClick = {
                                selectedEntryForDetail = entry
                            },
                            modifier = Modifier.padding(vertical = 5.dp)
                        )
                    }

                    // Render custom student events
                    items(dayCustomEvents, key = { "custom_event_${it.id}" }) { event ->
                        PaddingBox(top = 4.dp, bottom = 6.dp) {
                            ActivityCard(
                                event = event,
                                onClick = { selectedActivityForDetail = event },
                                onEdit = { activityToEdit = event },
                                onDelete = { onDeleteActivity(event.id) }
                            )
                        }
                    }
                }

                // Dedicated bottom spacer so content scrolls completely above floating pill dock
                item(key = "bottom_nav_spacer") {
                    Spacer(modifier = Modifier.height(110.dp))
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
                        Text(
                            text = "Venue: ${event.location}",
                            style = MaterialTheme.typography.bodyMedium
                        )
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
                        val id = event.id
                        selectedActivityForDetail = null
                        onDeleteActivity(id)
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
                onAddActivity(updatedEvent)
                activityToEdit = null
            }
        )
    }

    // MODAL: Real Native LectureDetailSheet connected to Room Notes & Alarms
    if (selectedEntryForDetail != null) {
        val entry = selectedEntryForDetail!!
        LectureDetailSheet(
            entry = entry,
            initialNote = notes[entry.naturalKey]?.content ?: "",
            onSaveNote = { noteText, reminderMinutes ->
                onSaveNote(entry, noteText, reminderMinutes)
                selectedEntryForDetail = null
            },
            onDismiss = { selectedEntryForDetail = null }
        )
    }

    // MODAL: Dedicated Customizable Class Alarm Picker Dialog
    if (selectedEntryForAlarm != null) {
        val entry = selectedEntryForAlarm!!
        val currentRem = notes[entry.naturalKey]?.alarmMinutes
        ClassAlarmPickerDialog(
            entry = entry,
            currentMinutes = currentRem,
            onConfirm = { chosenMinutes ->
                val currentNoteContent = notes[entry.naturalKey]?.content ?: ""
                onSaveNote(entry, currentNoteContent, chosenMinutes)
                selectedEntryForAlarm = null
            },
            onDismiss = {
                selectedEntryForAlarm = null
            }
        )
    }

    // MODAL: Native QuickAddBottomSheet supporting Custom Events & Tasks
    if (showQuickAddSheet) {
        val courseDetailsList = remember(entries) {
            entries.map { it.courseCode to it.courseTitle }.distinctBy { it.first }
        }
        QuickAddBottomSheet(
            initialDayOfWeek = selectedDay,
            availableCourses = entries.map { it.courseCode }.distinct(),
            courseDetails = courseDetailsList,
            onDismiss = { showQuickAddSheet = false },
            onSaveActivity = { event ->
                onAddActivity(event)
                showQuickAddSheet = false
            },
            onSaveTask = { title, course, due, prio, rem, nts ->
                onAddTask(title, course, due, prio, rem, nts)
                showQuickAddSheet = false
            }
        )
    }

    if (showNotificationCenterSheet) {
        NotificationCenterSheet(
            onDismiss = { showNotificationCenterSheet = false }
        )
    }

    if (showAcademicProfileSheet) {
        AcademicProfileSheet(
            programme = programme,
            onDismiss = { showAcademicProfileSheet = false },
            onReconfigureProfile = {
                showAcademicProfileSheet = false
                onReconfigureAcademicProfile()
            },
            onOpenSettings = {
                showAcademicProfileSheet = false
                onSettingsClick()
            }
        )
    }
}

// -----------------------------------------------------------------------------
// UI SUBCOMPONENTS
// -----------------------------------------------------------------------------

@Composable
private fun PaddingBox(
    top: androidx.compose.ui.unit.Dp = 0.dp,
    bottom: androidx.compose.ui.unit.Dp = 0.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = top, bottom = bottom)
    ) {
        content()
    }
}

/**
 * Converts a TimetableEntry into a SpecClassSession for high-fidelity specification UI rendering.
 */
private fun TimetableEntry.toSpecClassSession(
    note: LectureNote? = null,
    isLive: Boolean = false,
    progress: SessionProgress? = null,
    startsInText: String? = null
): SpecClassSession {
    val codeUpper = courseCode.trim().uppercase()
    val titleUpper = courseTitle.trim().uppercase()
    val typeUpper = sessionType?.trim()?.uppercase() ?: ""

    val specType = when {
        codeUpper.contains("MUCOSA") || titleUpper.contains("MUCOSA") ||
                titleUpper.contains("ASSOCIATION") || titleUpper.contains("GUILD") ||
                codeUpper.contains("GUILD") || titleUpper.contains("SOCIETY") -> SpecSessionType.ASSOCIATION
        typeUpper.contains("LAB") || room?.uppercase()?.contains("LAB") == true -> SpecSessionType.LAB
        typeUpper.contains("PRACTICAL") -> SpecSessionType.PRACTICAL
        typeUpper.contains("CLINICAL") || titleUpper.contains("CLINICAL") || titleUpper.contains("WARD") -> SpecSessionType.CLINICAL
        else -> SpecSessionType.LECTURE
    }

    val elapsedMins = progress?.elapsedMinutes ?: 0
    val remMins = progress?.remainingMinutes ?: 0
    val elapsedStr = if (elapsedMins >= 60) "${elapsedMins / 60}h ${elapsedMins % 60}m elapsed" else "${elapsedMins}m elapsed"
    val remStr = if (remMins >= 60) {
        val h = remMins / 60
        val m = remMins % 60
        if (m > 0) "${h}h ${m}m left · Ends $endTime" else "${h}h left · Ends $endTime"
    } else if (remMins > 0) {
        "$remMins mins left · Ends $endTime"
    } else {
        "Ending soon · Ends $endTime"
    }

    val cleanVenue = room?.trim()?.takeIf {
        it.isNotBlank() && it.uppercase() !in listOf("TBA", "TBD", "NONE", "N/A", "NULL")
    }
    val cleanLecturer = lecturer?.trim()?.takeIf {
        it.isNotBlank() && !it.equals("Staff", ignoreCase = true) && !it.equals("TBD", ignoreCase = true) && !it.equals("None", ignoreCase = true)
    }
    val cleanNotePreview = note?.content?.trim()?.takeIf { it.isNotBlank() }?.take(40)

    return SpecClassSession(
        id = naturalKey,
        courseCode = courseCode,
        courseName = courseTitle,
        department = programmeGroup,
        topicDescription = null,
        startTime = startTime,
        endTime = endTime,
        type = specType,
        venue = cleanVenue,
        lecturer = cleanLecturer,
        startsInText = startsInText,
        attachedNotesCount = if (cleanNotePreview != null) 1 else 0,
        attachedNotePreview = cleanNotePreview,
        isAlarmSet = note?.alarmMinutes != null,
        alarmMinutes = note?.alarmMinutes,
        isHappeningNow = isLive,
        progress = progress?.progress ?: 0.5f,
        elapsedText = elapsedStr,
        remainingText = remStr
    )
}

/**
 * Global Real-Time Next Up Hero Card.
 * Displays what is ACTUALLY next right now in real time, consistent across all day tabs.
 */
@Composable
private fun GlobalNextUpHeroCard(
    nextUp: GlobalNextUp,
    onClick: () -> Unit
) {
    val entry = nextUp.entry
    val isLive = nextUp.isLive
    val progress = nextUp.progress

    val gradient = if (isLive) {
        Brush.horizontalGradient(listOf(Color(0xFF1E3A8A), Color(0xFF2563EB), Color(0xFF3B82F6)))
    } else {
        Brush.horizontalGradient(listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF334155)))
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shadowElevation = 4.dp
    ) {
        Box(
            modifier = Modifier
                .background(gradient)
                .padding(18.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isLive) Color(0xFF22C55E) else Color.White.copy(alpha = 0.2f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isLive) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(Color.White, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = if (isLive) "HAPPENING NOW" else "NEXT UP",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = nextUp.dayLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.25f)
                    ) {
                        Text(
                            text = if (isLive) {
                                val rem = progress?.remainingMinutes ?: 0
                                if (rem > 0) "${rem}m left" else "Ending soon"
                            } else {
                                if (nextUp.minutesUntil > 0) "in ${nextUp.minutesUntil}m" else "Starting soon"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isLive) Color(0xFF86EFAC) else Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = entry.courseCode,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.White.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = entry.sessionType ?: "Theory",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = entry.courseTitle,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${entry.startTime} – ${entry.endTime}",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )

                        val roomText = entry.room?.trim()
                        if (!roomText.isNullOrEmpty() && roomText.uppercase() !in listOf("TBA", "TBD", "NONE", "N/A")) {
                            Spacer(modifier = Modifier.width(14.dp))
                            Icon(
                                imageVector = Icons.Outlined.MeetingRoom,
                                contentDescription = "Venue",
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = roomText,
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            RoomFloorBadge(roomName = roomText, compact = true, useContrastColor = true)
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "View Details",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // If live: render real-time progress bar
                if (isLive && progress != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.25f))
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Live Progress (${(progress.progress * 100).toInt()}%)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "${progress.elapsedMinutes}m elapsed",
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { progress.progress.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = Color(0xFF4ADE80),
                            trackColor = Color.White.copy(alpha = 0.2f)
                        )
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// LOGIC: True Real-Time Next Up Finder Across Entire Schedule
// -----------------------------------------------------------------------------

private fun findActualNextUp(
    entries: List<TimetableEntry>,
    todayName: String
): GlobalNextUp? {
    if (entries.isEmpty()) return null

    val now = Calendar.getInstance()
    val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
    val todayEntries = entries.filter { it.dayOfWeek.equals(todayName, ignoreCase = true) }

    // 1. Check if any class is ongoing right now today
    for (entry in todayEntries) {
        val prog = TimeUtil.calculateProgress(entry.startTime, entry.endTime, entry.dayOfWeek)
        if (prog.isOngoing) {
            return GlobalNextUp(
                entry = entry,
                isLive = true,
                progress = prog,
                minutesUntil = 0,
                dayLabel = "Today"
            )
        }
    }

    // 2. Check if any class is scheduled later today
    val upcomingToday = todayEntries
        .mapNotNull { entry ->
            val start = TimeUtil.toMinutes(entry.startTime)
            if (start > currentMinutes) {
                Pair(entry, start - currentMinutes)
            } else null
        }
        .minByOrNull { it.second }

    if (upcomingToday != null) {
        return GlobalNextUp(
            entry = upcomingToday.first,
            isLive = false,
            progress = null,
            minutesUntil = upcomingToday.second,
            dayLabel = "Today"
        )
    }

    // 3. Look ahead on subsequent days of the week
    val days = TimeUtil.DAYS
    val todayIdx = days.indexOfFirst { it.equals(todayName, ignoreCase = true) }.coerceAtLeast(0)
    for (offset in 1..6) {
        val nextDayIdx = (todayIdx + offset) % 7
        val nextDayName = days[nextDayIdx]
        val nextDayEntries = entries
            .filter { it.dayOfWeek.equals(nextDayName, ignoreCase = true) }
            .sortedBy { TimeUtil.toMinutes(it.startTime) }

        val earliest = nextDayEntries.firstOrNull()
        if (earliest != null) {
            val startMin = TimeUtil.toMinutes(earliest.startTime)
            val minutesUntil = (offset * 24 * 60) - currentMinutes + startMin
            val label = if (offset == 1) "Tomorrow" else nextDayName
            return GlobalNextUp(
                entry = earliest,
                isLive = false,
                progress = null,
                minutesUntil = minutesUntil.coerceAtLeast(1),
                dayLabel = label
            )
        }
    }

    return null
}
