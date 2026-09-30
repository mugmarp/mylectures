package com.mustime.features.calendar

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mustime.TimetableApplication
import com.mustime.core.alarm.AlarmScheduler
import com.mustime.features.timetable.domain.ActivityCategory
import com.mustime.features.timetable.domain.CustomEvent
import com.mustime.features.timetable.domain.TimetableEntry
import com.mustime.features.timetable.ui.ActivityCard
import com.mustime.features.timetable.ui.AddActivitySheet
import com.mustime.features.timetable.ui.LectureCard
import com.mustime.features.timetable.ui.LectureDetailSheet
import com.mustime.ui.components.AcademicProfileSheet
import com.mustime.ui.components.NotificationCenterSheet

private sealed class CalendarItem(val startTime: String) {
    data class Lecture(val entry: TimetableEntry) : CalendarItem(entry.startTime)
    data class Activity(val event: CustomEvent) : CalendarItem(event.startTime)
}

@Composable
fun CalendarScreen(
    onBack: (() -> Unit)? = null,
    onSettingsClick: () -> Unit = {},
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

    val classAlarmScheduler = remember(context) { AlarmScheduler(context) }
    val viewModel: CalendarViewModel = viewModel(
        factory = CalendarViewModel.provideFactory(repository, classAlarmScheduler)
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
    var showNotificationSheet by remember { mutableStateOf(false) }
    var showProfileSheet by remember { mutableStateOf(false) }
    var eventToDelete by remember { mutableStateOf<CustomEvent?>(null) }
    var selectedActivityForDetail by remember { mutableStateOf<CustomEvent?>(null) }
    var selectedLectureForDetail by remember { mutableStateOf<TimetableEntry?>(null) }

    // Lectures for selected day
    val selectedDayLectures = remember(uiState.entries, uiState.selectedDayOfWeek) {
        uiState.entries
            .filter { it.dayOfWeek.equals(uiState.selectedDayOfWeek, ignoreCase = true) }
            .sortedBy { it.startTime }
    }

    // Activities for selected day
    val selectedDayActivities = remember(uiState.customEvents, uiState.selectedDayOfWeek) {
        uiState.customEvents
            .filter { it.dayOfWeek.equals(uiState.selectedDayOfWeek, ignoreCase = true) }
            .sortedBy { it.startTime }
    }

    // Combined & Filtered items
    val displayedItems = remember(
        selectedDayLectures,
        selectedDayActivities,
        uiState.selectedFilter
    ) {
        val list = mutableListOf<CalendarItem>()
        if (uiState.selectedFilter != CalendarFilter.ACTIVITIES) {
            list.addAll(selectedDayLectures.map { CalendarItem.Lecture(it) })
        }
        if (uiState.selectedFilter != CalendarFilter.LECTURES) {
            list.addAll(selectedDayActivities.map { CalendarItem.Activity(it) })
        }
        list.sortedBy { it.startTime }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddActivitySheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                icon = { Icon(Icons.Default.Add, contentDescription = "Add Activity") },
                text = { Text("Add Activity", fontWeight = FontWeight.Bold) },
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 76.dp)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding(),
            contentPadding = PaddingValues(bottom = 150.dp)
        ) {
            // Top Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.Event,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .clickable { showProfileSheet = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Academic Profile",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        FilledTonalButton(
                            onClick = { showAddActivitySheet = true },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Activity", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Text(
                    text = "Calendar",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Month Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { viewModel.previousMonth() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.ChevronLeft,
                            contentDescription = "Previous Month",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = uiState.yearMonthText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { viewModel.nextMonth() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = "Next Month",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Days of week header
                val dayNames = listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    dayNames.forEach { name ->
                        Text(
                            text = name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(40.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Calendar Grid with distinguished color indicators
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    var currentDay = 1
                    for (row in 0..5) {
                        if (currentDay > uiState.daysInMonth) break
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            for (col in 0..6) {
                                val cellIndex = row * 7 + col
                                if (cellIndex < uiState.firstDayOfWeekOffset || currentDay > uiState.daysInMonth) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color.Transparent)
                                    )
                                } else {
                                    val dayNum = currentDay
                                    val isSelected = dayNum == uiState.selectedDay
                                    val isToday = viewModel.isToday(dayNum)
                                    val cellDayName = viewModel.getDayOfWeekNameForDay(dayNum)

                                    val hasClassesThisDay = uiState.entries.any {
                                        it.dayOfWeek.equals(cellDayName, ignoreCase = true)
                                    }
                                    val activitiesThisDay = uiState.customEvents.filter {
                                        it.dayOfWeek.equals(cellDayName, ignoreCase = true)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
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
                                                fontSize = 14.sp,
                                                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                                                color = when {
                                                    isSelected -> MaterialTheme.colorScheme.onPrimary
                                                    isToday -> MaterialTheme.colorScheme.primary
                                                    else -> MaterialTheme.colorScheme.onSurface
                                                }
                                            )

                                            // Distinguished color dots below date
                                            if (hasClassesThisDay || activitiesThisDay.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    // Class indicator dot
                                                    if (hasClassesThisDay) {
                                                        Dot(
                                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                    // Custom activity distinguished color dot(s)
                                                    activitiesThisDay.take(2).forEach { act ->
                                                        val cat = ActivityCategory.fromName(act.category)
                                                        val dotColor = if (isSelected) {
                                                            Color.White.copy(alpha = 0.9f)
                                                        } else {
                                                            ActivityCategory.colorForHex(act.colorTag.ifBlank { cat.colorHex })
                                                        }
                                                        Dot(color = dotColor)
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

                Spacer(modifier = Modifier.height(20.dp))

                // Schedule for selected day header (supports horizontal swipe from day to another day)
                var dragTotal by remember { mutableFloatStateOf(0f) }
                Column(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .pointerInput(uiState.selectedDay, uiState.daysInMonth) {
                            detectHorizontalDragGestures(
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    dragTotal += dragAmount
                                },
                                onDragEnd = {
                                    if (dragTotal < -50f) {
                                        viewModel.nextDay()
                                    } else if (dragTotal > 50f) {
                                        viewModel.previousDay()
                                    }
                                    dragTotal = 0f
                                },
                                onDragCancel = {
                                    dragTotal = 0f
                                }
                            )
                        }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${uiState.selectedDayOfWeek}, Day ${uiState.selectedDay}",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${selectedDayLectures.size} classes • ${selectedDayActivities.size} activities • Swipe to change",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            IconButton(
                                onClick = { viewModel.nextDay() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = "Next Day",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Filter chips row
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(CalendarFilter.entries.toTypedArray()) { filter ->
                            val count = when (filter) {
                                CalendarFilter.ALL -> selectedDayLectures.size + selectedDayActivities.size
                                CalendarFilter.LECTURES -> selectedDayLectures.size
                                CalendarFilter.ACTIVITIES -> selectedDayActivities.size
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
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Loading state
            if (uiState.isLoading && uiState.entries.isEmpty() && uiState.customEvents.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp),
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Loading schedule...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            } else if (displayedItems.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(28.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Event,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No items scheduled for ${uiState.selectedDayOfWeek}",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Tap 'Add Activity' to schedule a study session, lab, or personal activity",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { showAddActivitySheet = true },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add an Activity")
                            }
                        }
                    }
                }
            } else {
                items(displayedItems, key = { item ->
                    when (item) {
                        is CalendarItem.Lecture -> "lec_${item.entry.naturalKey}"
                        is CalendarItem.Activity -> "act_${item.event.id}"
                    }
                }) { item ->
                    Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                        when (item) {
                            is CalendarItem.Lecture -> {
                                LectureCard(
                                    entry = item.entry,
                                    onClick = { selectedLectureForDetail = item.entry }
                                )
                            }
                            is CalendarItem.Activity -> {
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

    // Activity Detail Dialog
    selectedActivityForDetail?.let { event ->
        AlertDialog(
            onDismissRequest = { selectedActivityForDetail = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = event.title,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
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

    // Notification Center Sheet
    if (showNotificationSheet) {
        NotificationCenterSheet(onDismiss = { showNotificationSheet = false })
    }

    // Academic Profile Sheet
    if (showProfileSheet) {
        AcademicProfileSheet(
            programme = uiState.currentProgramme,
            onDismiss = { showProfileSheet = false },
            onReconfigureProfile = {
                showProfileSheet = false
                onReconfigureAcademicProfile()
            },
            onOpenSettings = {
                showProfileSheet = false
                onSettingsClick()
            }
        )
    }

    // Delete Confirmation Dialog
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
}

@Composable
private fun Dot(color: Color) {
    Box(
        modifier = Modifier
            .size(5.dp)
            .background(color, CircleShape)
    )
}
