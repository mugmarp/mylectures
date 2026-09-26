package com.mustime.features.timetable.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mustime.features.timetable.domain.TimetableEntry
import com.mustime.ui.LocalAppTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LectureDetailSheet(
    entry: TimetableEntry,
    initialNote: String = "",
    initialReminder: Int? = null,
    onSaveNote: (String, Int?) -> Unit,
    onDismiss: () -> Unit
) {
    val isDark = LocalAppTheme.current.isDark
    val surfaceColor = if (isDark) DarkSurfaceCard else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val textSub = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val inputBg = if (isDark) DarkSurfaceBase else Color(0xFFF8FAFC)
    val inputBorder = if (isDark) DarkBorderSubtle else Color(0xFFE2E8F0)

    var noteText by remember { mutableStateOf(initialNote) }
    val standardReminderOptions = listOf(5, 10, 15, 30, 45, 60)
    val isCustomInitial = initialReminder != null && initialReminder !in standardReminderOptions
    var isCustomReminder by remember { mutableStateOf(isCustomInitial) }
    var customReminderText by remember { mutableStateOf(if (isCustomInitial) initialReminder.toString() else "") }
    var selectedReminder by remember { mutableStateOf<Int?>(initialReminder ?: 15) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = surfaceColor,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .background(if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1), RoundedCornerShape(2.dp))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = entry.courseTitle,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${entry.courseCode} • ${entry.dayOfWeek} ${entry.startTime}-${entry.endTime}",
                fontSize = 14.sp,
                color = textSub
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            // Session Badge
            val sessionLabel = resolveSessionTypeLabel(entry)
            val sessionStyling = sessionStyle(entry.sessionType, entry.room ?: "", entry.courseTitle)
            Box(
                modifier = Modifier
                    .background(
                        sessionStyling.badgeBg.copy(alpha = if (isDark) 0.35f else 0.15f),
                        RoundedCornerShape(12.dp)
                    )
                    .border(
                        1.dp,
                        sessionStyling.badgeBg.copy(alpha = 0.3f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = sessionLabel,
                    color = if (isDark) sessionStyling.badgeBg else sessionStyling.accentBar,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Details rows
            val roomVal = entry.room?.trim()
            if (!roomVal.isNullOrBlank() && roomVal.uppercase() !in listOf("TBA", "TBD", "NONE", "N/A")) {
                DetailRow(icon = Icons.Outlined.Place, label = "Room", value = roomVal, isDark = isDark)
                Spacer(modifier = Modifier.height(12.dp))
            }
            val lecturerVal = entry.lecturer?.trim()
            if (!lecturerVal.isNullOrBlank() && !lecturerVal.equals("Staff", ignoreCase = true)) {
                DetailRow(icon = Icons.Outlined.Person, label = "Lecturer", value = lecturerVal, isDark = isDark)
                Spacer(modifier = Modifier.height(12.dp))
            }
            DetailRow(icon = Icons.Outlined.Schedule, label = "Time", value = "${entry.startTime} – ${entry.endTime}", isDark = isDark)

            Spacer(modifier = Modifier.height(20.dp))

            // Reminders
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Notifications, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Class Alarm Reminder", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = textColor)
            }
            Spacer(modifier = Modifier.height(10.dp))

            // Presets and Custom Chip
            androidx.compose.foundation.layout.FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                standardReminderOptions.forEach { minutes ->
                    val isSelected = !isCustomReminder && selectedReminder == minutes
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) PrimaryBlue else Color.Transparent)
                            .border(
                                1.dp,
                                if (isSelected) PrimaryBlue else (if (isDark) DarkBorderSubtle else Color(0xFFCBD5E1)),
                                RoundedCornerShape(20.dp)
                            )
                            .clickable {
                                isCustomReminder = false
                                selectedReminder = if (isSelected) null else minutes
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${minutes}m before",
                            color = if (isSelected) Color.White else (if (isDark) Color(0xFFCBD5E1) else Color(0xFF1E293B)),
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }

                // Custom chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isCustomReminder) PrimaryBlue else Color.Transparent)
                        .border(
                            1.dp,
                            if (isCustomReminder) PrimaryBlue else (if (isDark) DarkBorderSubtle else Color(0xFFCBD5E1)),
                            RoundedCornerShape(20.dp)
                        )
                        .clickable {
                            isCustomReminder = !isCustomReminder
                            if (!isCustomReminder) {
                                selectedReminder = 15
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Custom",
                        color = if (isCustomReminder) Color.White else (if (isDark) Color(0xFFCBD5E1) else Color(0xFF1E293B)),
                        fontSize = 12.sp,
                        fontWeight = if (isCustomReminder) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }

            if (isCustomReminder) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = customReminderText,
                    onValueChange = { input ->
                        val filtered = input.filter { it.isDigit() }.take(3)
                        customReminderText = filtered
                        selectedReminder = filtered.toIntOrNull()
                    },
                    label = { Text("Custom minutes before class") },
                    placeholder = { Text("e.g. 20, 25, 40, 90") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Personal notes
            Text("Personal Notes & Study Points", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = textColor)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                placeholder = { Text("Add lecture takeaways, exam prep notes...", color = Color(0xFF94A3B8)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = inputBorder,
                    focusedContainerColor = inputBg,
                    unfocusedContainerColor = inputBg,
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor
                )
            )

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Linked to: ${entry.courseCode} (${entry.dayOfWeek})",
                fontSize = 11.sp,
                color = textSub
            )

            Spacer(modifier = Modifier.height(18.dp))

            val effectiveMinutesToSave = if (isCustomReminder) customReminderText.toIntOrNull() else selectedReminder

            Button(
                onClick = {
                    onSaveNote(noteText, effectiveMinutesToSave)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue, contentColor = Color.White)
            ) {
                Text(
                    text = if (effectiveMinutesToSave != null) "Save Note & Set ${effectiveMinutesToSave}m Alarm" else "Save Note",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    isDark: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            label,
            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
        Text(
            value,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = if (isDark) Color.White else Color(0xFF0F172A)
        )
    }
}
