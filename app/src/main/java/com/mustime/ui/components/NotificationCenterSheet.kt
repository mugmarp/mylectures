package com.mustime.ui.components

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DoneAll
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import com.mustime.core.notification.InAppNotificationItem
import com.mustime.core.notification.InAppNotificationStore
import com.mustime.core.notification.NotificationHelper
import com.mustime.core.notification.NotificationItemType
import com.mustime.features.timetable.domain.Assignment
import com.mustime.features.timetable.domain.LectureNote
import com.mustime.features.timetable.ui.*
import com.mustime.ui.LocalAppTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationCenterSheet(
    activeNotesWithAlarms: List<LectureNote> = emptyList(),
    activeTasksWithAlarms: List<Assignment> = emptyList(),
    onCancelNoteAlarm: ((LectureNote) -> Unit)? = null,
    onCancelTaskAlarm: ((Assignment) -> Unit)? = null,
    onNavigateToTab: ((Int) -> Unit)? = null,
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

    val notifStore = remember(context) { InAppNotificationStore.getInstance(context) }
    val notificationHistory by notifStore.notifications.collectAsState()

    var selectedSection by remember { mutableIntStateOf(0) } // 0 = Scheduled Alarms, 1 = Notification History
    var testFeedback by remember { mutableStateOf<String?>(null) }

    val areNotificationsEnabled = remember(context) {
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

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
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            contentPadding = PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(PrimaryBlue.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.NotificationsActive,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(22.dp)
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
                            text = "Academic alerts, class countdowns & inbox",
                            fontSize = 13.sp,
                            color = textSub
                        )
                    }
                }
            }

            // Notification Status Card
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (areNotificationsEnabled) {
                        if (isDark) Color(0xFF064E3B).copy(alpha = 0.3f) else Color(0xFFF0FDF4)
                    } else {
                        if (isDark) Color(0xFF78350F).copy(alpha = 0.3f) else Color(0xFFFEF3C7)
                    },
                    border = BorderStroke(
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
            }

            // Test Alarm Quick Action
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, cardBorder),
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
                                text = "Trigger alert to verify sound & banner",
                                fontSize = 11.sp,
                                color = textSub
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                onClick = {
                                    NotificationHelper.showClassAlarmNotification(
                                        context = context,
                                        courseCode = "SWE2101",
                                        courseTitle = "Object Oriented Programming",
                                        room = "SFL01 (Computer Lab)",
                                        startTime = "10:00 AM"
                                    )
                                    testFeedback = "Triggered test Class Alert!"
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFEF4444),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text("Class Alert", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    com.mustime.core.alarm.ReminderScheduler(context).triggerTestDailyDigest()
                                    testFeedback = "Posted Daily Class Digest!"
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PrimaryBlue,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text("Daily Digest", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                if (testFeedback != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = testFeedback!!,
                        fontSize = 12.sp,
                        color = Color(0xFF10B981),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Tab Selector: Scheduled Alarms vs Notification History
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(cardBg)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val activeTotal = activeNotesWithAlarms.size + activeTasksWithAlarms.size
                    val unreadCount = notificationHistory.count { !it.isRead }

                    // Tab 0: Scheduled Alarms
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (selectedSection == 0) surfaceColor else Color.Transparent)
                            .clickable { selectedSection = 0 }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Active Alarms ($activeTotal)",
                            fontSize = 12.sp,
                            fontWeight = if (selectedSection == 0) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedSection == 0) PrimaryBlue else textSub
                        )
                    }

                    // Tab 1: Alert History / Inbox
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (selectedSection == 1) surfaceColor else Color.Transparent)
                            .clickable { selectedSection = 1 }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Alert History (${notificationHistory.size})",
                                fontSize = 12.sp,
                                fontWeight = if (selectedSection == 1) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedSection == 1) PrimaryBlue else textSub
                            )
                            if (unreadCount > 0) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(Color(0xFFEF4444), CircleShape)
                                )
                            }
                        }
                    }
                }
            }

            // Section 0: Scheduled Alarms
            if (selectedSection == 0) {
                if (activeNotesWithAlarms.isEmpty() && activeTasksWithAlarms.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = cardBg,
                            border = BorderStroke(1.dp, cardBorder),
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
                                    text = "Tap 'Set Alarm' on any lecture or task to schedule reminders.",
                                    fontSize = 12.sp,
                                    color = textSub,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(activeNotesWithAlarms) { note ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = cardBg,
                            border = BorderStroke(1.dp, cardBorder),
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
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    val courseCode = note.naturalKey.split("|", ":").firstOrNull()?.trim() ?: "Class Note"
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
                                if (onCancelNoteAlarm != null) {
                                    IconButton(
                                        onClick = { onCancelNoteAlarm(note) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AlarmOff,
                                            contentDescription = "Cancel Alarm",
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    items(activeTasksWithAlarms) { task ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = cardBg,
                            border = BorderStroke(1.dp, cardBorder),
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
                                    modifier = Modifier.size(18.dp)
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
                                if (onCancelTaskAlarm != null) {
                                    IconButton(
                                        onClick = { onCancelTaskAlarm(task) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AlarmOff,
                                            contentDescription = "Cancel Reminder",
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 1: Alert History & Logging
            if (selectedSection == 1) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { notifStore.markAllAsRead() },
                            enabled = notificationHistory.any { !it.isRead }
                        ) {
                            Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Mark all read", fontSize = 12.sp)
                        }

                        TextButton(
                            onClick = { notifStore.clearAll() },
                            enabled = notificationHistory.isNotEmpty()
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear history", fontSize = 12.sp)
                        }
                    }
                }

                if (notificationHistory.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = cardBg,
                            border = BorderStroke(1.dp, cardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Notifications,
                                    contentDescription = null,
                                    tint = textSub,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No notification history yet",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = textColor
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Fired alarms and reminders will be logged here.",
                                    fontSize = 11.sp,
                                    color = textSub
                                )
                            }
                        }
                    }
                } else {
                    items(notificationHistory, key = { it.id }) { item ->
                        val timeFormat = remember { SimpleDateFormat("h:mm a, MMM d", Locale.getDefault()) }
                        val timeStr = remember(item.timestamp) { timeFormat.format(Date(item.timestamp)) }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (item.isRead) cardBg else surfaceColor,
                            border = BorderStroke(1.dp, if (!item.isRead) PrimaryBlue.copy(alpha = 0.4f) else cardBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    notifStore.markAsRead(item.id)
                                    if (item.navTab != null && onNavigateToTab != null) {
                                        onNavigateToTab(item.navTab)
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(
                                            when (item.type) {
                                                NotificationItemType.CLASS_ALARM -> Color(0xFFEF4444).copy(alpha = 0.15f)
                                                NotificationItemType.CLASS_REMINDER -> Color(0xFF10B981).copy(alpha = 0.15f)
                                                NotificationItemType.TASK_REMINDER -> Color(0xFF2563EB).copy(alpha = 0.15f)
                                                else -> Color(0xFF8B5CF6).copy(alpha = 0.15f)
                                            },
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when (item.type) {
                                            NotificationItemType.CLASS_ALARM -> Icons.Default.Alarm
                                            NotificationItemType.TASK_REMINDER -> Icons.Outlined.TaskAlt
                                            else -> Icons.Outlined.Notifications
                                        },
                                        contentDescription = null,
                                        tint = when (item.type) {
                                            NotificationItemType.CLASS_ALARM -> Color(0xFFEF4444)
                                            NotificationItemType.CLASS_REMINDER -> Color(0xFF10B981)
                                            NotificationItemType.TASK_REMINDER -> Color(0xFF2563EB)
                                            else -> Color(0xFF8B5CF6)
                                        },
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = item.title,
                                            fontSize = 12.sp,
                                            fontWeight = if (!item.isRead) FontWeight.Bold else FontWeight.SemiBold,
                                            color = textColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = timeStr,
                                            fontSize = 10.sp,
                                            color = textSub
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = item.message,
                                        fontSize = 11.sp,
                                        color = textSub,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = { notifStore.deleteNotification(item.id) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Delete",
                                        tint = textSub,
                                        modifier = Modifier.size(15.dp)
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
