package com.mustime.features.timetable.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mustime.core.util.TimeUtil
import com.mustime.features.timetable.domain.ActivityCategory
import com.mustime.features.timetable.domain.CustomEvent
import com.mustime.ui.components.DedicatedTimePickerDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddActivitySheet(
    initialDayOfWeek: String = TimeUtil.todayName(),
    eventToEdit: CustomEvent? = null,
    onDismiss: () -> Unit,
    onSave: (CustomEvent) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val isEditMode = eventToEdit != null

    var title by remember { mutableStateOf(eventToEdit?.title ?: "") }
    var selectedCategory by remember {
        mutableStateOf(
            if (eventToEdit != null) ActivityCategory.fromName(eventToEdit.category) else ActivityCategory.STUDY
        )
    }
    var selectedDay by remember {
        mutableStateOf(
            eventToEdit?.dayOfWeek ?: if (initialDayOfWeek.isNotBlank()) initialDayOfWeek else TimeUtil.todayName()
        )
    }
    var startTime by remember { mutableStateOf(eventToEdit?.startTime ?: "14:00") }
    var endTime by remember { mutableStateOf(eventToEdit?.endTime ?: "15:30") }
    var location by remember { mutableStateOf(eventToEdit?.location ?: "") }
    var notes by remember { mutableStateOf(eventToEdit?.notes ?: "") }
    var hasError by remember { mutableStateOf(false) }

    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .imePadding()
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isEditMode) "Edit Activity" else "Add New Activity",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isEditMode) "Update schedule, venue, category, or notes" else "Schedule custom study, lab, sports, or club events",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    onDismiss()
                }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Title input
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    if (it.isNotBlank()) hasError = false
                },
                label = { Text("Activity Title *") },
                placeholder = { Text("e.g., Biochemistry Revision, Gym Session") },
                isError = hasError && title.isBlank(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            if (hasError && title.isBlank()) {
                Text(
                    text = "Please enter an activity title",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Category Selection
            Text(
                text = "Activity Category & Color",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(ActivityCategory.entries.toTypedArray()) { cat ->
                    val isSelected = cat == selectedCategory
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedCategory = cat },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) cat.color else cat.color.copy(alpha = 0.1f),
                        border = if (isSelected) null else BorderStroke(1.dp, cat.color.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(if (isSelected) Color.White else cat.color, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = cat.displayName,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else cat.color
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Day of Week Selection
            Text(
                text = "Day of Week",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(TimeUtil.DAYS) { day ->
                    val isSelected = day.equals(selectedDay, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedDay = day },
                        label = { Text(day.take(3)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Time range row (Tap to open circular Google Clock picker)
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
                        .testTag("add_activity_start_time_input")
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
                            text = startTime.ifBlank { "14:00" },
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
                        .testTag("add_activity_end_time_input")
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
                            text = endTime.ifBlank { "15:30" },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Quick time suggestions
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val presets = listOf(
                    "08:00 - 09:30" to ("08:00" to "09:30"),
                    "10:00 - 12:00" to ("10:00" to "12:00"),
                    "14:00 - 15:30" to ("14:00" to "15:30"),
                    "16:00 - 18:00" to ("16:00" to "18:00")
                )
                presets.forEach { (label, times) ->
                    SuggestionChip(
                        onClick = {
                            startTime = times.first
                            endTime = times.second
                        },
                        label = { Text(label, fontSize = 11.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Location input
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Location (Optional)") },
                placeholder = { Text("e.g., Library Study Room 4, Sports Hall") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = {
                    Icon(Icons.Outlined.MeetingRoom, contentDescription = "Venue", modifier = Modifier.size(18.dp))
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Notes input
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes / Description (Optional)") },
                placeholder = { Text("e.g., Bring past questions and calculator") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                minLines = 2,
                maxLines = 3,
                leadingIcon = {
                    Icon(Icons.Outlined.Notes, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Save button
            Button(
                onClick = {
                    if (title.isBlank()) {
                        hasError = true
                        return@Button
                    }
                    val event = CustomEvent(
                        id = eventToEdit?.id ?: 0L,
                        title = title.trim(),
                        dayOfWeek = selectedDay,
                        startTime = startTime.trim(),
                        endTime = endTime.trim(),
                        location = location.trim(),
                        notes = notes.trim(),
                        repeatWeekly = true,
                        category = selectedCategory.name,
                        colorTag = selectedCategory.colorHex
                    )
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    onSave(event)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = selectedCategory.color
                )
            ) {
                Icon(if (isEditMode) Icons.Outlined.Check else Icons.Outlined.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isEditMode) "Save Changes" else "Add Activity",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    if (showStartTimePicker) {
        DedicatedTimePickerDialog(
            initialTime = startTime,
            title = "Select Start Time",
            onTimeSelected = { picked ->
                startTime = picked
                showStartTimePicker = false
            },
            onDismiss = { showStartTimePicker = false }
        )
    }

    if (showEndTimePicker) {
        DedicatedTimePickerDialog(
            initialTime = endTime,
            title = "Select End Time",
            onTimeSelected = { picked ->
                endTime = picked
                showEndTimePicker = false
            },
            onDismiss = { showEndTimePicker = false }
        )
    }
}
