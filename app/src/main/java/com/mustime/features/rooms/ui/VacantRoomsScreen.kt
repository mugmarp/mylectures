package com.mustime.features.rooms.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mustime.TimetableApplication
import com.mustime.core.util.TimeUtil
import com.mustime.features.rooms.BuildingLevel
import com.mustime.features.rooms.RoomVacancyStatus
import com.mustime.features.rooms.UniversityDirectory
import com.mustime.features.timetable.domain.TimetableEntry
import com.mustime.features.timetable.ui.*
import com.mustime.ui.LocalAppTheme
import com.mustime.ui.components.DedicatedDayPickerDialog
import com.mustime.ui.components.DedicatedTimePickerDialog
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VacantRoomsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as? TimetableApplication
    val repository = app?.repository

    val allEntries by (repository?.getAllEntries() ?: kotlinx.coroutines.flow.flowOf(emptyList()))
        .collectAsState(initial = emptyList())

    val customEvents by (repository?.getCustomEvents() ?: kotlinx.coroutines.flow.flowOf(emptyList()))
        .collectAsState(initial = emptyList())

    val isDark = LocalAppTheme.current.isDark
    val surfaceColor = if (isDark) DarkSurfaceCard else Color.White
    val bgColor = if (isDark) DarkSurfaceBase else SurfaceBaseLight
    val textPrimary = if (isDark) Color.White else TextPrimaryLight
    val textMuted = if (isDark) Color(0xFF94A3B8) else TextMutedLight
    val borderSubtle = if (isDark) DarkBorderSubtle else BorderSubtleLight

    // Current real-time clock defaults
    val calendar = remember { Calendar.getInstance() }
    val initialDay = remember { TimeUtil.todayName() }
    val initialHour = remember { calendar.get(Calendar.HOUR_OF_DAY) }
    val initialMinute = remember { calendar.get(Calendar.MINUTE) }
    val initialTimeStr = remember { "%02d:%02d".format(initialHour, initialMinute) }

    // Active Query Parameters
    var queryDay by remember { mutableStateOf(initialDay) }
    var queryTime by remember { mutableStateOf(initialTimeStr) }
    var isLiveNow by remember { mutableStateOf(true) }

    // Filters
    var minGapMinutes by remember { mutableIntStateOf(30) } // Default 30 min as requested
    var selectedLevel by remember { mutableStateOf<BuildingLevel?>(null) } // null = All levels
    var studyFriendlyOnly by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }

    // Pickers visibility
    var showTimePicker by remember { mutableStateOf(false) }
    var showDayPicker by remember { mutableStateOf(false) }
    var selectedRoomForSchedule by remember { mutableStateOf<RoomVacancyStatus?>(null) }

    // Calculate Room Vacancy algorithmically
    val vacancyStatuses = remember(allEntries, customEvents, queryDay, queryTime, minGapMinutes) {
        UniversityDirectory.calculateRoomVacancy(
            allEntries = allEntries,
            dayOfWeek = queryDay,
            queryTimeStr = queryTime,
            minGapMinutes = minGapMinutes,
            customEvents = customEvents
        )
    }

    // Filter results
    val filteredRooms = remember(vacancyStatuses, selectedLevel, studyFriendlyOnly, minGapMinutes, searchQuery) {
        vacancyStatuses.filter { status ->
            val matchesLevel = selectedLevel == null || status.room.level == selectedLevel
            val matchesStudy = !studyFriendlyOnly || status.room.isStudyFriendly
            val matchesSearch = searchQuery.isBlank() ||
                    status.room.name.contains(searchQuery, ignoreCase = true) ||
                    status.room.code.contains(searchQuery, ignoreCase = true)

            // When studyFriendlyOnly and free, apply the minimum gap requirement
            val matchesGap = if (!status.isOccupied && minGapMinutes > 0 && studyFriendlyOnly) {
                status.gapMinutes >= minGapMinutes || status.freeUntil == "Rest of day"
            } else true

            matchesLevel && matchesStudy && matchesSearch && matchesGap
        }
    }

    val freeCount = vacancyStatuses.count { !it.isOccupied && (it.gapMinutes >= minGapMinutes || it.freeUntil == "Rest of day") && it.room.isStudyFriendly }
    val occupiedCount = vacancyStatuses.count { it.isOccupied && it.room.isStudyFriendly }

    Scaffold(
        containerColor = bgColor,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Vacant Room Finder",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = textPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .background(StatusGreenLive.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "FCI Building",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusGreenLive
                                )
                            }
                        }
                        Text(
                            text = "Kihumuro Campus · Real-time Study Gaps",
                            fontSize = 12.sp,
                            color = textMuted
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = surfaceColor)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "header_spacer") {
                Spacer(modifier = Modifier.height(2.dp))
            }

            // SECTION 1: INTERACTIVE TIME & DAY CONTROLS
            item(key = "query_controls_card") {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = surfaceColor,
                    border = BorderStroke(1.dp, borderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(PrimaryBlue.copy(alpha = 0.12f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Outlined.AccessTime,
                                        contentDescription = null,
                                        tint = PrimaryBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Querying Vacancy At",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = textMuted
                                    )
                                    Text(
                                        text = "$queryDay at $queryTime",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textPrimary
                                    )
                                }
                            }

                            // Quick "Now" Reset Chip
                            FilterChip(
                                selected = isLiveNow,
                                onClick = {
                                    val now = Calendar.getInstance()
                                    queryDay = TimeUtil.todayName()
                                    queryTime = "%02d:%02d".format(now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE))
                                    isLiveNow = true
                                },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(if (isLiveNow) StatusGreenLive else textMuted, CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Live Now", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                },
                                shape = RoundedCornerShape(10.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Dedicated Pickers Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showDayPicker = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Outlined.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(queryDay.take(3), fontSize = 13.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    isLiveNow = false
                                    showTimePicker = true
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Outlined.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(queryTime, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // SECTION 2: METRIC KPI BANNER
            item(key = "kpi_summary") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Free Rooms Card
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFDCFCE7).copy(alpha = if (isDark) 0.15f else 0.8f),
                        border = BorderStroke(1.dp, Color(0xFF86EFAC).copy(alpha = 0.5f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(StatusGreenLive, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Available Now",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) Color(0xFF4ADE80) else Color(0xFF15803D)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "$freeCount Study Rooms",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDark) Color.White else Color(0xFF14532D)
                            )
                            Text(
                                text = "≥ $minGapMinutes min study gap",
                                fontSize = 11.sp,
                                color = if (isDark) Color(0xFF86EFAC) else Color(0xFF166534)
                            )
                        }
                    }

                    // Occupied Rooms Card
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFFEE2E2).copy(alpha = if (isDark) 0.15f else 0.8f),
                        border = BorderStroke(1.dp, Color(0xFFFCA5A5).copy(alpha = 0.5f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color(0xFFEF4444), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Class in Session",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) Color(0xFFF87171) else Color(0xFFB91C1C)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "$occupiedCount Rooms",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDark) Color.White else Color(0xFF7F1D1D)
                            )
                            Text(
                                text = "Tap room to view slot",
                                fontSize = 11.sp,
                                color = if (isDark) Color(0xFFFCA5A5) else Color(0xFF991B1B)
                            )
                        }
                    }
                }
            }

            // SECTION 3: MINIMUM GAP FILTER (ALGORITHM FIX)
            item(key = "algorithm_gap_filter") {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Minimum Study Gap",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Text(
                            text = if (minGapMinutes == 0) "All Gaps" else "Min $minGapMinutes minutes",
                            fontSize = 11.sp,
                            color = PrimaryBlue,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val gapOptions = listOf(
                            "30 min (Default)" to 30,
                            "15 min" to 15,
                            "45 min" to 45,
                            "1 hour" to 60,
                            "Any free" to 0
                        )
                        items(gapOptions) { (label, value) ->
                            val isSelected = minGapMinutes == value
                            FilterChip(
                                selected = isSelected,
                                onClick = { minGapMinutes = value },
                                label = { Text(label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }
            }

            // SECTION 4: LEVEL & STUDY-FRIENDLY FILTERS
            item(key = "level_filter_strip") {
                Column {
                    Text(
                        text = "Building Floor / Level",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterChip(
                                selected = selectedLevel == null,
                                onClick = { selectedLevel = null },
                                label = { Text("All Levels", fontSize = 12.sp) },
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                        items(BuildingLevel.entries.toTypedArray()) { level ->
                            val isSelected = selectedLevel == level
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedLevel = level },
                                label = { Text(level.shortName, fontSize = 12.sp) },
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                        item {
                            FilterChip(
                                selected = !studyFriendlyOnly,
                                onClick = { studyFriendlyOnly = !studyFriendlyOnly },
                                label = { Text(if (studyFriendlyOnly) "Classrooms Only" else "Include Offices/Board", fontSize = 12.sp) },
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }
            }

            // Search Bar
            item(key = "room_search_bar") {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search room (e.g. LR1, CR3, Library)") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = textMuted) },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )
            }

            // SECTION 5: ROOM CARDS LIST
            if (filteredRooms.isEmpty()) {
                item(key = "empty_rooms") {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = surfaceColor,
                        border = BorderStroke(1.dp, borderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Outlined.MeetingRoom,
                                contentDescription = null,
                                tint = textMuted,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No Rooms Match the Selected Filter",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = textPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Try lowering the minimum gap threshold or clearing the search query.",
                                fontSize = 12.sp,
                                color = textMuted,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(filteredRooms, key = { it.room.id }) { status ->
                    RoomVacancyCard(
                        status = status,
                        isDark = isDark,
                        onClick = { selectedRoomForSchedule = status }
                    )
                }
            }

            // Footer info regarding FAST building
            item(key = "fast_building_notice") {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = surfaceColor.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, borderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Info,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "FCI Building catalog verified from directory sign. FAST Complex building directory will be added once finalized.",
                            fontSize = 12.sp,
                            color = textMuted
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Modal Pickers
    if (showTimePicker) {
        DedicatedTimePickerDialog(
            initialTime = queryTime,
            title = "Set Vacancy Query Time",
            onTimeSelected = { newTime ->
                queryTime = newTime
                isLiveNow = false
            },
            onDismiss = { showTimePicker = false }
        )
    }

    if (showDayPicker) {
        DedicatedDayPickerDialog(
            selectedDay = queryDay,
            onDaySelected = { newDay ->
                queryDay = newDay
                isLiveNow = false
            },
            onDismiss = { showDayPicker = false }
        )
    }

    // Room Day Schedule Modal
    selectedRoomForSchedule?.let { status ->
        RoomScheduleDetailDialog(
            status = status,
            dayOfWeek = queryDay,
            isDark = isDark,
            onDismiss = { selectedRoomForSchedule = null }
        )
    }
}

/**
 * Visual Room Card displaying availability status, "Free until X", gap minutes, and level tag.
 */
@Composable
fun RoomVacancyCard(
    status: RoomVacancyStatus,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val room = status.room
    val surfaceColor = if (isDark) DarkSurfaceCard else Color.White
    val textPrimary = if (isDark) Color.White else TextPrimaryLight
    val textMuted = if (isDark) Color(0xFF94A3B8) else TextMutedLight
    val borderSubtle = if (isDark) DarkBorderSubtle else BorderSubtleLight

    val statusColor = when {
        !room.isStudyFriendly -> Color(0xFF8B5CF6)
        status.isOccupied -> Color(0xFFEF4444)
        status.gapMinutes >= 60 || status.freeUntil == "Rest of day" -> Color(0xFF16A34A)
        else -> Color(0xFFF59E0B)
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = surfaceColor,
        border = BorderStroke(1.dp, borderSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon indicator
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(statusColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        !room.isStudyFriendly -> Icons.Outlined.Lock
                        status.isOccupied -> Icons.Outlined.School
                        else -> Icons.Outlined.MeetingRoom
                    },
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Room Code and Level Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = room.code,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )

                    Box(
                        modifier = Modifier
                            .background(PrimaryBlue.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${room.level.shortName} · ${room.type.displayName}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = PrimaryBlue
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = room.name,
                    fontSize = 13.sp,
                    color = textMuted
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Availability Badge & Detail
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(statusColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = status.formattedAvailability,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }

                // Next session hint if available
                if (!status.isOccupied && status.nextSession != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Next: ${status.nextSession.courseCode} · ${status.nextSession.courseTitle}",
                        fontSize = 11.sp,
                        color = textMuted,
                        maxLines = 1
                    )
                } else if (status.isOccupied && status.currentSession != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "In session: ${status.currentSession.courseCode} (${status.currentSession.programmeGroup})",
                        fontSize = 11.sp,
                        color = textMuted,
                        maxLines = 1
                    )
                }
            }

            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = "View Day Schedule",
                tint = textMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Dialog showing complete schedule of all classes scheduled in this room on the query day.
 */
@Composable
fun RoomScheduleDetailDialog(
    status: RoomVacancyStatus,
    dayOfWeek: String,
    isDark: Boolean,
    onDismiss: () -> Unit
) {
    val room = status.room
    val sessions = status.allDaySessions
    val textPrimary = if (isDark) Color.White else TextPrimaryLight
    val textMuted = if (isDark) Color(0xFF94A3B8) else TextMutedLight

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = if (isDark) DarkSurfaceCard else Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp)
                .padding(horizontal = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${room.code} · ${room.name}",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                        }
                        Text(
                            text = "${room.level.displayName} · $dayOfWeek Schedule",
                            fontSize = 12.sp,
                            color = textMuted
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
                    }
                }

                if (room.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF8B5CF6).copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = room.notes,
                            fontSize = 12.sp,
                            color = Color(0xFF8B5CF6),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "All Scheduled Sessions (${sessions.size})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (sessions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = StatusGreenLive, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Entire Day is Free!",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = textPrimary
                            )
                            Text(
                                text = "No lectures scheduled in this room on $dayOfWeek.",
                                fontSize = 12.sp,
                                color = textMuted
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(sessions) { session ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDark) DarkSurfaceBase else Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, if (isDark) DarkBorderSubtle else BorderSubtleLight),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.width(68.dp)
                                    ) {
                                        Text(
                                            text = session.startTime,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = PrimaryBlue
                                        )
                                        Text(
                                            text = session.endTime,
                                            fontSize = 11.sp,
                                            color = textMuted
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))
                                    VerticalDivider(modifier = Modifier.height(34.dp), color = textMuted.copy(alpha = 0.3f))
                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = session.courseCode,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = textPrimary
                                        )
                                        Text(
                                            text = session.courseTitle,
                                            fontSize = 12.sp,
                                            color = textMuted,
                                            maxLines = 1
                                        )
                                        if (session.programmeGroup.isNotBlank()) {
                                            Text(
                                                text = "Group: ${session.programmeGroup}",
                                                fontSize = 11.sp,
                                                color = PrimaryBlue
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Close")
                }
            }
        }
    }
}
