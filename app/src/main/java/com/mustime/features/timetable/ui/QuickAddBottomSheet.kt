package com.mustime.features.timetable.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mustime.core.alarm.TaskDateTimeParser
import com.mustime.core.util.TimeUtil
import com.mustime.features.timetable.domain.ActivityCategory
import com.mustime.features.timetable.domain.CustomEvent
import com.mustime.features.timetable.domain.TaskCategory
import com.mustime.ui.components.DedicatedCalendarDatePickerDialog
import com.mustime.ui.components.DedicatedCoursePickerDialog
import com.mustime.ui.components.DedicatedDayPickerDialog
import com.mustime.ui.components.DedicatedSchedulePickerDialog
import com.mustime.ui.components.DedicatedTimePickerDialog
import com.mustime.ui.components.RoomFloorBadge

enum class QuickAddMode(val title: String, val subtitle: String) {
    ACTIVITY("Timetable Activity", "Scheduled session on timetable"),
    TASK("Academic Task", "To-do, assignment & deadline")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuickAddBottomSheet(
    initialDayOfWeek: String = TimeUtil.todayName(),
    initialMode: QuickAddMode = QuickAddMode.ACTIVITY,
    availableCourses: List<String> = emptyList(),
    courseDetails: List<Pair<String, String>> = emptyList(),
    onDismiss: () -> Unit,
    onSaveActivity: (CustomEvent) -> Unit,
    onSaveTask: (
        title: String,
        courseCode: String,
        dueDate: String,
        priority: String,
        reminderMinutes: Int?,
        notes: String
    ) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var currentMode by remember { mutableStateOf(initialMode) }

    // Activity state
    var activityTitle by remember { mutableStateOf("") }
    var selectedActivityCategory by remember { mutableStateOf(ActivityCategory.STUDY) }
    var activityDay by remember {
        mutableStateOf(
            if (initialDayOfWeek.isNotBlank()) initialDayOfWeek else TimeUtil.todayName()
        )
    }
    var activityStartTime by remember { mutableStateOf("14:00") }
    var activityEndTime by remember { mutableStateOf("15:30") }
    var activityLocation by remember { mutableStateOf("") }
    var activityAlarmMinutes by remember { mutableStateOf<Int?>(30) }
    var isActivityCustomAlarm by remember { mutableStateOf(false) }
    var activityCustomMinutesText by remember { mutableStateOf("") }
    var activityNotes by remember { mutableStateOf("") }
    var activityHasError by remember { mutableStateOf(false) }

    // Task state
    var taskTitle by remember { mutableStateOf("") }
    var selectedTaskCategory by remember { mutableStateOf(TaskCategory.ASSIGNMENT) }
    var taskCourseCode by remember {
        mutableStateOf(availableCourses.firstOrNull() ?: "PHA3102")
    }
    var taskDueDate by remember {
        mutableStateOf("Tomorrow")
    }
    var taskPriority by remember { mutableStateOf("Medium") }
    var taskReminderMinutes by remember { mutableStateOf<Int?>(1440) }
    var isTaskCustomReminder by remember { mutableStateOf(false) }
    var taskCustomMinutesText by remember { mutableStateOf("") }
    var taskNotes by remember { mutableStateOf("") }
    var taskHasError by remember { mutableStateOf(false) }

    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var showCoursePicker by remember { mutableStateOf(false) }
    var showDueCalendarPicker by remember { mutableStateOf(false) }
    var tempDueDay by remember { mutableStateOf(initialDayOfWeek) }

    val daysOfWeek = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.testTag("quick_add_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Quick Add Entry",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Add an activity or task directly into your schedule",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            onDismiss()
                        },
                        modifier = Modifier.testTag("close_quick_add_sheet")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

            // Mode Distinction Selector Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Activity Tab
                val isActivity = currentMode == QuickAddMode.ACTIVITY
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isActivity) MaterialTheme.colorScheme.surface else Color.Transparent
                        )
                        .clickable { currentMode = QuickAddMode.ACTIVITY }
                        .padding(vertical = 10.dp, horizontal = 12.dp)
                        .testTag("quick_add_tab_activity"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.EventNote,
                            contentDescription = null,
                            tint = if (isActivity) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "User Activity",
                                fontWeight = if (isActivity) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp,
                                color = if (isActivity) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Timetable Block",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Task Tab
                val isTask = currentMode == QuickAddMode.TASK
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isTask) MaterialTheme.colorScheme.surface else Color.Transparent
                        )
                        .clickable { currentMode = QuickAddMode.TASK }
                        .padding(vertical = 10.dp, horizontal = 12.dp)
                        .testTag("quick_add_tab_task"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Assignment,
                            contentDescription = null,
                            tint = if (isTask) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "Academic Task",
                                fontWeight = if (isTask) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp,
                                color = if (isTask) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Deadline & Alarm",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            when (currentMode) {
                QuickAddMode.ACTIVITY -> {
                    // Category Selection Header
                    Text(
                        text = "Activity Category",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.testTag("activity_category_row")
                    ) {
                        items(ActivityCategory.entries) { cat ->
                            val isSelected = cat == selectedActivityCategory
                            val categoryIcon = when (cat) {
                                ActivityCategory.STUDY -> Icons.Outlined.MenuBook
                                ActivityCategory.LAB -> Icons.Outlined.Science
                                ActivityCategory.SPORTS -> Icons.Outlined.FitnessCenter
                                ActivityCategory.CLUB -> Icons.Outlined.Groups
                                ActivityCategory.PERSONAL -> Icons.Outlined.Person
                                ActivityCategory.EXAM -> Icons.Outlined.Timer
                            }
                            Card(
                                onClick = { selectedActivityCategory = cat },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) cat.color.copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ),
                                border = if (isSelected) BorderStroke(1.5.dp, cat.color) else null
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = categoryIcon,
                                        contentDescription = null,
                                        tint = if (isSelected) cat.color else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = cat.displayName,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) cat.color else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Title input
                    OutlinedTextField(
                        value = activityTitle,
                        onValueChange = {
                            activityTitle = it
                            if (it.isNotBlank()) activityHasError = false
                        },
                        label = { Text("Activity Title *") },
                        placeholder = { Text("e.g. Group Revision, Gym, Physics Lab") },
                        isError = activityHasError,
                        supportingText = if (activityHasError) {
                            { Text("Title is required", color = MaterialTheme.colorScheme.error) }
                        } else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("activity_title_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    // Title quick suggestions
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 6.dp)
                    ) {
                        val suggestions = listOf("Group Study", "Physics Lab", "Library Work", "Discussion", "Exam Revision")
                        items(suggestions) { sugg ->
                            SuggestionChip(
                                onClick = {
                                    activityTitle = sugg
                                    activityHasError = false
                                },
                                label = { Text(sugg, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Day of Week
                    Text(
                        text = "Day of Week",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(end = 16.dp),
                        modifier = Modifier.testTag("activity_day_row")
                    ) {
                        items(daysOfWeek) { day ->
                            val isSelected = day.equals(activityDay, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = { activityDay = day },
                                label = { Text(day.take(3), fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Time Window: Start & End Time (Tap to open circular Google Clock picker)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Start Time Selector Tile
                        Surface(
                            onClick = { showStartTimePicker = true },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("activity_start_time_input")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = "Start Time",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = activityStartTime.ifBlank { "14:00" },
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // End Time Selector Tile
                        Surface(
                            onClick = { showEndTimePicker = true },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("activity_end_time_input")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = "End Time",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = activityEndTime.ifBlank { "15:30" },
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // Duration presets
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("+1 hr" to 60, "+1.5 hrs" to 90, "+2 hrs" to 120).forEach { (label, durationMinutes) ->
                            AssistChip(
                                onClick = {
                                    val startMin = TimeUtil.toMinutes(activityStartTime)
                                    val endMin = (startMin + durationMinutes).coerceAtMost(23 * 60 + 59)
                                    val h = endMin / 60
                                    val m = endMin % 60
                                    activityEndTime = "%02d:%02d".format(h, m)
                                },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Location
                    OutlinedTextField(
                        value = activityLocation,
                        onValueChange = { activityLocation = it },
                        label = { Text("Location (Optional)") },
                        placeholder = { Text("e.g. Main Library, Room 204, Sports Field") },
                        leadingIcon = {
                            Icon(Icons.Outlined.MeetingRoom, contentDescription = "Venue", modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            if (activityLocation.isNotBlank()) {
                                RoomFloorBadge(roomName = activityLocation, compact = true)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("activity_location_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Reminder Alarm
                    Text(
                        text = "Alarm Notification",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(end = 16.dp)
                    ) {
                        items(
                            listOf(
                                "None" to null,
                                "10m" to 10,
                                "15m" to 15,
                                "30m" to 30,
                                "1h" to 60
                            )
                        ) { (label, minutes) ->
                            val isSelected = !isActivityCustomAlarm && activityAlarmMinutes == minutes
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    isActivityCustomAlarm = false
                                    activityAlarmMinutes = minutes
                                },
                                label = { Text(label, fontSize = 12.sp) },
                                leadingIcon = if (isSelected && minutes != null) {
                                    { Icon(Icons.Outlined.NotificationsActive, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                } else null,
                                shape = RoundedCornerShape(10.dp)
                            )
                        }

                        item {
                            FilterChip(
                                selected = isActivityCustomAlarm,
                                onClick = { isActivityCustomAlarm = !isActivityCustomAlarm },
                                label = { Text("Custom", fontSize = 12.sp) },
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }

                    if (isActivityCustomAlarm) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = activityCustomMinutesText,
                            onValueChange = { input ->
                                val filtered = input.filter { it.isDigit() }.take(3)
                                activityCustomMinutesText = filtered
                                activityAlarmMinutes = filtered.toIntOrNull()
                            },
                            label = { Text("Custom lead minutes") },
                            placeholder = { Text("e.g. 20, 45, 90") },
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Notes
                    OutlinedTextField(
                        value = activityNotes,
                        onValueChange = { activityNotes = it },
                        label = { Text("Notes (Optional)") },
                        placeholder = { Text("Any prep notes or materials needed...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("activity_notes_input"),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }

                QuickAddMode.TASK -> {
                    // Task Category Header
                    Text(
                        text = "Task Category",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.testTag("task_category_row")
                    ) {
                        items(TaskCategory.entries) { cat ->
                            val isSelected = cat == selectedTaskCategory
                            Card(
                                onClick = { selectedTaskCategory = cat },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) cat.color.copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ),
                                border = if (isSelected) BorderStroke(1.5.dp, cat.color) else null
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = cat.icon,
                                        contentDescription = null,
                                        tint = if (isSelected) cat.color else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = cat.displayName,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) cat.color else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Priority level
                    Text(
                        text = "Priority Level",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple("High", Color(0xFFE11D48), Color(0xFFFFE4E6)),
                            Triple("Medium", Color(0xFFD97706), Color(0xFFFEF3C7)),
                            Triple("Low", Color(0xFF2563EB), Color(0xFFEFF6FF))
                        ).forEach { (prio, textColor, bgColor) ->
                            val isSelected = taskPriority.equals(prio, ignoreCase = true)
                            Card(
                                onClick = { taskPriority = prio },
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) bgColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ),
                                border = if (isSelected) BorderStroke(1.5.dp, textColor) else null,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = prio,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) textColor else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Task Title
                    OutlinedTextField(
                        value = taskTitle,
                        onValueChange = {
                            taskTitle = it
                            if (it.isNotBlank()) taskHasError = false
                        },
                        label = { Text("Task Title *") },
                        placeholder = { Text("e.g. Lab Report 3, Assignment 2, Paper Draft") },
                        isError = taskHasError,
                        supportingText = if (taskHasError) {
                            { Text("Title is required", color = MaterialTheme.colorScheme.error) }
                        } else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("task_title_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Course Code
                    OutlinedTextField(
                        value = taskCourseCode,
                        onValueChange = { taskCourseCode = it },
                        label = { Text("Course Code *") },
                        placeholder = { Text("e.g. Course Code") },
                        leadingIcon = {
                            Icon(Icons.Outlined.School, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            IconButton(onClick = { showCoursePicker = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Pick Course", modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("task_course_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    // Quick course suggestions if available
                    val courseSuggestions = availableCourses.distinct().take(5)
                    if (courseSuggestions.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 6.dp)
                        ) {
                            item {
                                AssistChip(
                                    onClick = { showCoursePicker = true },
                                    label = { Text("Select Course ▼", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                )
                            }
                            items(courseSuggestions) { code ->
                                SuggestionChip(
                                    onClick = { taskCourseCode = code },
                                    label = { Text(code, fontSize = 11.sp) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Due Date Card (Tapping opens the full interactive Calendar)
                    Text(
                        text = "Due Date *",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth().testTag("task_due_date_button")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Current Selected Due Date Header with Calendar Action
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { showDueCalendarPicker = true }
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = taskDueDate.ifBlank { "Tomorrow" },
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Target day (time discarded)",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.DateRange,
                                        contentDescription = "Open Calendar",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Calendar",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            // Quick Day Selection: strictly "Today" and "Tomorrow"
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val isToday = taskDueDate.equals("Today", ignoreCase = true)
                                val isTomorrow = taskDueDate.equals("Tomorrow", ignoreCase = true)

                                FilterChip(
                                    selected = isToday,
                                    onClick = { taskDueDate = "Today" },
                                    label = {
                                        Text(
                                            "Today",
                                            fontSize = 12.sp,
                                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    leadingIcon = if (isToday) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                    } else null,
                                    shape = RoundedCornerShape(9.dp),
                                    modifier = Modifier.weight(1f)
                                )

                                FilterChip(
                                    selected = isTomorrow,
                                    onClick = { taskDueDate = "Tomorrow" },
                                    label = {
                                        Text(
                                            "Tomorrow",
                                            fontSize = 12.sp,
                                            fontWeight = if (isTomorrow) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    leadingIcon = if (isTomorrow) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                    } else null,
                                    shape = RoundedCornerShape(9.dp),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Schedule Preview
                    val schedulePreview = remember(taskDueDate, taskReminderMinutes) {
                        TaskDateTimeParser.formatSchedulePreview(taskDueDate, taskReminderMinutes)
                    }
                    if (schedulePreview.isNotBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.Alarm, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = schedulePreview,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Alarm Reminder
                    Text(
                        text = "Alarm Reminder",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    androidx.compose.foundation.layout.FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "None" to null,
                            "15m" to 15,
                            "30m" to 30,
                            "1h" to 60,
                            "1d" to 1440
                        ).forEach { (label, minutes) ->
                            val isSelected = !isTaskCustomReminder && taskReminderMinutes == minutes
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    isTaskCustomReminder = false
                                    taskReminderMinutes = minutes
                                },
                                label = { Text(label, fontSize = 12.sp) },
                                leadingIcon = if (isSelected && minutes != null) {
                                    { Icon(Icons.Outlined.NotificationsActive, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                } else null,
                                shape = RoundedCornerShape(10.dp)
                            )
                        }

                        FilterChip(
                            selected = isTaskCustomReminder,
                            onClick = { isTaskCustomReminder = !isTaskCustomReminder },
                            label = { Text("Custom", fontSize = 12.sp) },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    if (isTaskCustomReminder) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = taskCustomMinutesText,
                            onValueChange = { input ->
                                val filtered = input.filter { it.isDigit() }.take(4)
                                taskCustomMinutesText = filtered
                                taskReminderMinutes = filtered.toIntOrNull()
                            },
                            label = { Text("Custom lead minutes before deadline") },
                            placeholder = { Text("e.g. 45, 90, 120, 2880") },
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Notes
                    OutlinedTextField(
                        value = taskNotes,
                        onValueChange = { taskNotes = it },
                        label = { Text("Task Notes (Optional)") },
                        placeholder = { Text("Submission details, portal links, guidelines...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("task_notes_input"),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // Pinned / Sticky Bottom Action Bar - ALWAYS VISIBLE, ZERO SCROLLING REQUIRED!
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                when (currentMode) {
                    QuickAddMode.ACTIVITY -> {
                        Button(
                            onClick = {
                                if (activityTitle.isBlank()) {
                                    activityHasError = true
                                    return@Button
                                }
                                val newEvent = CustomEvent(
                                    title = activityTitle.trim(),
                                    dayOfWeek = activityDay,
                                    startTime = activityStartTime.trim(),
                                    endTime = activityEndTime.trim(),
                                    location = activityLocation.trim(),
                                    notes = activityNotes.trim(),
                                    category = selectedActivityCategory.displayName,
                                    colorTag = selectedActivityCategory.colorHex,
                                    alarmMinutes = activityAlarmMinutes
                                )
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                onSaveActivity(newEvent)
                                onDismiss()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("save_activity_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = selectedActivityCategory.color
                            )
                        ) {
                            Icon(Icons.Default.EventNote, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Add Activity",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                    QuickAddMode.TASK -> {
                        Button(
                            onClick = {
                                if (taskTitle.isBlank()) {
                                    taskHasError = true
                                    return@Button
                                }
                                val effectiveCourse = taskCourseCode.ifBlank { "GEN100" }
                                val fullTitle = if (selectedTaskCategory != TaskCategory.GENERAL && !taskTitle.startsWith(selectedTaskCategory.displayName)) {
                                    "[${selectedTaskCategory.displayName}] ${taskTitle.trim()}"
                                } else {
                                    taskTitle.trim()
                                }
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                onSaveTask(
                                    fullTitle,
                                    effectiveCourse.trim(),
                                    taskDueDate.trim(),
                                    taskPriority,
                                    taskReminderMinutes,
                                    taskNotes.trim()
                                )
                                onDismiss()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("save_task_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.AutoMirrored.Outlined.Assignment, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Add Task",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

    if (showStartTimePicker) {
        DedicatedTimePickerDialog(
            initialTime = activityStartTime,
            title = "Set Activity Start Time",
            onTimeSelected = { activityStartTime = it },
            onDismiss = { showStartTimePicker = false }
        )
    }

    if (showEndTimePicker) {
        DedicatedTimePickerDialog(
            initialTime = activityEndTime,
            title = "Set Activity End Time",
            onTimeSelected = { activityEndTime = it },
            onDismiss = { showEndTimePicker = false }
        )
    }

    if (showCoursePicker) {
        val resolvedCourseList = remember(availableCourses, courseDetails) {
            if (courseDetails.isNotEmpty()) courseDetails
            else availableCourses.map { it to "" }
        }
        DedicatedCoursePickerDialog(
            selectedCourse = taskCourseCode,
            availableCourses = resolvedCourseList,
            onCourseSelected = { code, _ -> taskCourseCode = code },
            onDismiss = { showCoursePicker = false }
        )
    }

    if (showDueCalendarPicker) {
        DedicatedCalendarDatePickerDialog(
            initialDate = taskDueDate,
            title = "Select Task Due Date",
            onDateSelected = { picked ->
                taskDueDate = picked
                showDueCalendarPicker = false
            },
            onDismiss = { showDueCalendarPicker = false }
        )
    }
}
