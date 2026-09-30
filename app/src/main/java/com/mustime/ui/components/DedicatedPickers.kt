package com.mustime.ui.components

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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mustime.features.timetable.ui.PrimaryBlue

/**
 * Dedicated Time Picker Dialog featuring the official Google Clock circular dial clock (TimePicker),
 * with interactive circular clock face, rotating clock hand for hours and minutes, and AM/PM toggle.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DedicatedTimePickerDialog(
    initialTime: String = "14:00",
    title: String = "Select Time",
    onTimeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val initialParts = initialTime.split(":")
    val initialHour = initialParts.getOrNull(0)?.toIntOrNull() ?: 14
    val initialMinute = initialParts.getOrNull(1)?.toIntOrNull() ?: 0

    val timePickerState = rememberTimePickerState(
        initialHour = initialHour.coerceIn(0, 23),
        initialMinute = initialMinute.coerceIn(0, 59),
        is24Hour = false
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Circular Clock Dial (Google Clock style)
                TimePicker(
                    state = timePickerState
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val formatted = "%02d:%02d".format(timePickerState.hour, timePickerState.minute)
                            onTimeSelected(formatted)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("OK", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

/**
 * Unified Google Clock inspired Schedule Picker Dialog that handles BOTH Day and Time atomically.
 * Prevents raw typing of "Tomorrow • 17:00" and ensures the time scheduler handles the whole flow.
 */
@Composable
fun DedicatedSchedulePickerDialog(
    initialSchedule: String = "Tomorrow • 17:00",
    title: String = "Set Due Schedule",
    onScheduleSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val parts = initialSchedule.split("•", "-").map { it.trim() }
    val parsedDay = parts.getOrNull(0)?.ifBlank { "Tomorrow" } ?: "Tomorrow"
    val parsedTime = parts.getOrNull(1)?.ifBlank { "17:00" } ?: "17:00"

    var selectedDay by remember { mutableStateOf(parsedDay) }
    var selectedTime by remember { mutableStateOf(parsedTime) }
    var showTimeDialog by remember { mutableStateOf(false) }

    val primaryColor = MaterialTheme.colorScheme.primary

    val quickDays = listOf("Today", "Tomorrow", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    val quickTimes = listOf("08:00", "10:00", "12:00", "14:00", "17:00", "20:00", "23:59")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Unified Result Preview Tile (Clickable to open circular Google Clock picker)
                Surface(
                    onClick = { showTimeDialog = true },
                    shape = RoundedCornerShape(16.dp),
                    color = primaryColor.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "SCHEDULED FOR",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryColor,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "$selectedDay • $selectedTime",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Button(
                            onClick = { showTimeDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit Time", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Day Selection Strip
                Text(
                    text = "1. Choose Target Day",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(quickDays) { day ->
                        val isSelected = day.equals(selectedDay, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedDay = day },
                            label = {
                                Text(
                                    text = day,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Quick Times Strip
                Text(
                    text = "2. Quick Time Preset",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(quickTimes) { t ->
                        val isSelected = t == selectedTime
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedTime = t },
                            label = {
                                Text(
                                    text = t,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Confirm Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            onScheduleSelected("$selectedDay • $selectedTime")
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                    ) {
                        Text("Apply Schedule ✓", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showTimeDialog) {
        DedicatedTimePickerDialog(
            initialTime = selectedTime,
            title = "Pick Time",
            onTimeSelected = { picked ->
                selectedTime = picked
                showTimeDialog = false
            },
            onDismiss = { showTimeDialog = false }
        )
    }
}

/**
 * Dedicated Day & Date Picker Dialog.
 */
@Composable
fun DedicatedDayPickerDialog(
    selectedDay: String = "Monday",
    onDaySelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val daysOfWeek = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = PrimaryBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Select Academic Day",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                daysOfWeek.forEach { day ->
                    val isSelected = day.equals(selectedDay, ignoreCase = true)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) PrimaryBlue.copy(alpha = 0.12f) else Color.Transparent,
                        border = if (isSelected) BorderStroke(1.dp, PrimaryBlue) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onDaySelected(day)
                                onDismiss()
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = day,
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) PrimaryBlue else MaterialTheme.colorScheme.onSurface
                            )
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

/**
 * Dedicated Course Picker Dialog with enrolled courses as default,
 * showing full course titles and code hints, plus search functionality.
 */
@Composable
fun DedicatedCoursePickerDialog(
    selectedCourse: String,
    availableCourses: List<Pair<String, String>>, // code to title
    onCourseSelected: (code: String, title: String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredList = remember(searchQuery, availableCourses) {
        if (searchQuery.isBlank()) availableCourses
        else {
            val q = searchQuery.trim().lowercase()
            availableCourses.filter { (code, title) ->
                code.lowercase().contains(q) || title.lowercase().contains(q)
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 520.dp)
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.School, contentDescription = null, tint = PrimaryBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Choose Course Unit",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search course code or name...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Enrolled & Department Courses",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (filteredList.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No matching courses found",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(filteredList) { (code, title) ->
                            val isSelected = code.equals(selectedCourse, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) PrimaryBlue.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = if (isSelected) BorderStroke(1.dp, PrimaryBlue) else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onCourseSelected(code, title)
                                        onDismiss()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) PrimaryBlue else PrimaryBlue.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = code.take(3),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else PrimaryBlue
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = code,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (title.isNotBlank()) {
                                            Text(
                                                text = title,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1
                                            )
                                        }
                                    }

                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = PrimaryBlue,
                                            modifier = Modifier.size(18.dp)
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
