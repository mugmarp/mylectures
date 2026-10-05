package com.mustime.features.timetable.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mustime.core.util.TimeUtil
import com.mustime.features.timetable.domain.TimetableEntry
import com.mustime.ui.LocalAppTheme
import com.mustime.ui.components.RoomFloorBadge

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ClassAlarmPickerDialog(
    entry: TimetableEntry,
    currentMinutes: Int?,
    onConfirm: (Int?) -> Unit,
    onDismiss: () -> Unit
) {
    val isDark = LocalAppTheme.current.isDark
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val textSub = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    val standardPresets = listOf(5, 10, 15, 30, 45, 60)
    val isCustomInitial = currentMinutes != null && currentMinutes !in standardPresets

    var selectedMinutes by remember { mutableStateOf<Int?>(currentMinutes ?: 30) }
    var isCustomSelected by remember { mutableStateOf(isCustomInitial) }
    var customText by remember { mutableStateOf(if (isCustomInitial) currentMinutes.toString() else "") }

    val startMinutes = remember(entry.startTime) { TimeUtil.toMinutes(entry.startTime) }
    val effectiveMinutes = if (isCustomSelected) customText.toIntOrNull() else selectedMinutes

    val triggerTimeDisplay = remember(effectiveMinutes, startMinutes) {
        if (effectiveMinutes == null || effectiveMinutes <= 0 || startMinutes <= 0) null
        else {
            val triggerMin = (startMinutes - effectiveMinutes + 1440) % 1440
            String.format(java.util.Locale.US, "%02d:%02d", triggerMin / 60, triggerMin % 60)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = surfaceColor,
        shape = RoundedCornerShape(20.dp),
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(primaryColor.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Alarm,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(26.dp)
                )
            }
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Class Alarm: ${entry.courseCode}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = textColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${entry.dayOfWeek} · ${entry.startTime} – ${entry.endTime}",
                    fontSize = 13.sp,
                    color = textSub
                )
                val roomText = entry.room?.trim()
                if (!roomText.isNullOrEmpty() && roomText.uppercase() !in listOf("TBA", "TBD", "NONE", "N/A")) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "📍 $roomText",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = textSub
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        RoomFloorBadge(roomName = roomText, compact = true, isDark = isDark)
                    }
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Alert Lead Time",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Presets Grid
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    standardPresets.forEach { mins ->
                        val isSelected = !isCustomSelected && selectedMinutes == mins
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) primaryColor else (if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9)))
                                .border(
                                    1.dp,
                                    if (isSelected) primaryColor else (if (isDark) Color(0xFF475569) else Color(0xFFE2E8F0)),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    isCustomSelected = false
                                    selectedMinutes = mins
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${mins}m before",
                                color = if (isSelected) Color.White else textColor,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }

                    // Custom Option Chip
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isCustomSelected) primaryColor else (if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9)))
                            .border(
                                1.dp,
                                if (isCustomSelected) primaryColor else (if (isDark) Color(0xFF475569) else Color(0xFFE2E8F0)),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                isCustomSelected = true
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Custom",
                            color = if (isCustomSelected) Color.White else textColor,
                            fontSize = 12.sp,
                            fontWeight = if (isCustomSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }

                // Custom Input Field
                if (isCustomSelected) {
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = customText,
                        onValueChange = { input ->
                            val filtered = input.filter { it.isDigit() }.take(3)
                            customText = filtered
                            selectedMinutes = filtered.toIntOrNull()
                        },
                        label = { Text("Custom minutes before class") },
                        placeholder = { Text("e.g. 20, 25, 40, 90") },
                        leadingIcon = {
                            Icon(Icons.Outlined.Timer, contentDescription = null, tint = primaryColor)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                if (triggerTimeDisplay != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = primaryColor.copy(alpha = 0.08f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Alarm, contentDescription = null, tint = primaryColor, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Alarm will sound at $triggerTimeDisplay (${effectiveMinutes}m before)",
                                fontSize = 12.sp,
                                color = primaryColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalMinutes = if (isCustomSelected) customText.toIntOrNull() else selectedMinutes
                    onConfirm(finalMinutes)
                },
                enabled = effectiveMinutes != null && effectiveMinutes > 0,
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Set Alarm")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (currentMinutes != null) {
                    TextButton(
                        onClick = { onConfirm(null) },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFDC2626))
                    ) {
                        Icon(Icons.Default.AlarmOff, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Turn Off")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}
