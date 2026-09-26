package com.mustime.ui.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
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
import androidx.core.app.NotificationManagerCompat
import com.mustime.core.alarm.TaskAlarmScheduler
import com.mustime.core.notification.NotificationHelper
import com.mustime.features.timetable.domain.Assignment
import com.mustime.features.timetable.domain.LectureNote
import com.mustime.features.timetable.ui.*
import com.mustime.ui.LocalAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationCenterSheet(
    activeNotesWithAlarms: List<LectureNote> = emptyList(),
    activeTasksWithAlarms: List<Assignment> = emptyList(),
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val isDark = LocalAppTheme.current.isDark
    val surfaceColor = if (isDark) DarkSurfaceCard else Color.White
    val textColor = if (isDark) Color.White else TextPrimaryLight
    val textSub = if (isDark) Color(0xFF94A3B8) else TextMutedLight
    val cardBg = if (isDark) DarkSurfaceBase else Color(0xFFF8FAFC)
    val cardBorder = if (isDark) DarkBorderSubtle else BorderSubtleLight

    val areNotificationsEnabled = remember(context) {
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    var testFeedback by remember { mutableStateOf<String?>(null) }

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
                .padding(bottom = 36.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(PrimaryBlue.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.NotificationsActive,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Notifications & Alarms",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Text(
                        text = "Academic alerts & class countdowns",
                        fontSize = 13.sp,
                        color = textSub
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Notification Status Card
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (areNotificationsEnabled) {
                    if (isDark) Color(0xFF064E3B).copy(alpha = 0.3f) else Color(0xFFF0FDF4)
                } else {
                    if (isDark) Color(0xFF78350F).copy(alpha = 0.3f) else Color(0xFFFEF3C7)
                },
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (areNotificationsEnabled) Color(0xFF86EFAC) else Color(0xFFFDE68A)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (areNotificationsEnabled) Icons.Filled.CheckCircle else Icons.Outlined.Warning,
                        contentDescription = null,
                        tint = if (areNotificationsEnabled) Color(0xFF16A34A) else Color(0xFFD97706),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (areNotificationsEnabled) "System Notifications Active" else "Notifications May Be Paused",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (areNotificationsEnabled) {
                                if (isDark) Color(0xFF86EFAC) else Color(0xFF166534)
                            } else {
                                if (isDark) Color(0xFFFDE68A) else Color(0xFF92400E)
                            }
                        )
                        Text(
                            text = if (areNotificationsEnabled) "Exact lecture alarms & reminders will wake device" else "Tap here to check notification settings",
                            fontSize = 11.sp,
                            color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Test Alarm Quick Action
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = cardBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF8B5CF6).copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = null,
                            tint = Color(0xFF8B5CF6),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Test Alarm & Chime",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = textColor
                        )
                        Text(
                            text = "Trigger a 3-second chime to verify sound",
                            fontSize = 11.sp,
                            color = textSub
                        )
                    }
                    Button(
                        onClick = {
                            NotificationHelper.showClassReminderNotification(
                                context = context,
                                courseCode = "LEC-TEST",
                                courseTitle = "Alarm Verification Test",
                                room = "Main Hall",
                                startTime = "Now"
                            )
                            testFeedback = "Chimed test notification!"
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryBlue,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Ring Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (testFeedback != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = testFeedback!!,
                    fontSize = 12.sp,
                    color = StatusGreenLive,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Active Class Reminders Header
            Text(
                text = "SCHEDULED CLASS ALARMS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = textSub,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (activeNotesWithAlarms.isEmpty() && activeTasksWithAlarms.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = cardBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No custom alarms currently set",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = textColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap 'Set 15m Alarm' on any upcoming lecture or task to schedule reminders.",
                            fontSize = 12.sp,
                            color = textSub,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(activeNotesWithAlarms) { note ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = cardBg,
                            border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Alarm,
                                    contentDescription = null,
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    val courseCode = note.naturalKey.split(":").firstOrNull()?.trim() ?: "Class Note"
                                    val displayTitle = note.title?.ifBlank { null } ?: note.content.take(35).ifBlank { "Lecture Note" }
                                    Text(
                                        text = "$courseCode · ${note.alarmMinutes ?: 15}m before",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor
                                    )
                                    Text(
                                        text = displayTitle,
                                        fontSize = 11.sp,
                                        color = textSub
                                    )
                                }
                            }
                        }
                    }

                    items(activeTasksWithAlarms) { task ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = cardBg,
                            border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.TaskAlt,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Task: ${task.title}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor
                                    )
                                    Text(
                                        text = "Due ${task.dueDate} · ${task.reminderMinutes ?: 30}m before",
                                        fontSize = 11.sp,
                                        color = textSub
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
