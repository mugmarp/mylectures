package com.mustime.features.timetable.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mustime.ui.LocalAppTheme

// Design Tokens & Theme Colors from Specification
val SurfaceBaseLight = Color(0xFFFAF8FF)
val SurfaceContainerLowestLight = Color(0xFFFFFFFF)
val SurfaceContainerLowLight = Color(0xFFF2F3FF)
val SurfaceContainerHighLight = Color(0xFFE8EBFA)
val SurfaceDimLight = Color(0xFFD2D9F4)

val DarkSurfaceBase = Color(0xFF101216)
val DarkSurfaceCard = Color(0xFF181B22)
val DarkSurfaceContainer = Color(0xFF20242D)
val DarkBorderSubtle = Color(0xFF2B303C)

val PrimaryBlue = Color(0xFF2563EB)
val PrimaryBlueDark = Color(0xFF1D4ED8)
val PrimaryBlueLight = Color(0xFF3B82F6)
val IndigoAccent = Color(0xFF4F46E5)

val HeroGradientStart = Color(0xFF2563EB)
val HeroGradientMid = Color(0xFF3B82F6)
val HeroGradientEnd = Color(0xFF4F46E5)

val ProgressTrackBackground = Color(0xFFFFFFFF).copy(alpha = 0.25f)
val ProgressIndicatorActive = Color(0xFF34D399) // Mint / Emerald active
val ProgressGradientStart = Color(0xFF60A5FA)
val ProgressGradientEnd = Color(0xFF34D399)

val TextPrimaryLight = Color(0xFF0F172A)
val TextSecondaryLight = Color(0xFF475569)
val TextMutedLight = Color(0xFF64748B)
val TextTertiaryLight = Color(0xFF94A3B8)

val StatusGreenLive = Color(0xFF10B981)
val StatusTheoryBlue = Color(0xFF2563EB)
val StatusLabPurple = Color(0xFF7C3AED)
val StatusLectureGreen = Color(0xFF059669)

val BorderSubtleLight = Color(0xFFE2E8F0)

val DayCapsuleShape = RoundedCornerShape(22.dp)
val HeroCardShape = RoundedCornerShape(24.dp)
val UpNextBarShape = RoundedCornerShape(18.dp)
val TimelineCardShape = RoundedCornerShape(16.dp)
val ProgressTrackShape = RoundedCornerShape(10.dp)
val PillBadgeShape = RoundedCornerShape(12.dp)
val ActionButtonShape = RoundedCornerShape(14.dp)
val FabShape = RoundedCornerShape(28.dp)

enum class SpecSessionType(val label: String, val tagColor: Color) {
    LECTURE("Lecture", StatusLectureGreen),
    THEORY("Lecture", StatusTheoryBlue),
    LAB("Lab Session", StatusLabPurple),
    PRACTICAL("Practical", Color(0xFF16A34A)),
    CLINICAL("Clinical", Color(0xFFD97706)),
    ASSOCIATION("Student Association", Color(0xFF8B5CF6))
}

data class SpecClassSession(
    val id: String,
    val courseCode: String,
    val courseName: String,
    val department: String,
    val topicDescription: String?,
    val startTime: String,
    val endTime: String,
    val type: SpecSessionType,
    val venue: String?,
    val lecturer: String?,
    val startsInText: String? = null,
    val attachedNotesCount: Int = 0,
    val attachedNotePreview: String? = null,
    val isAlarmSet: Boolean = false,
    val isHappeningNow: Boolean = false,
    val progress: Float = 0.70f,
    val elapsedText: String = "1h 45m elapsed",
    val remainingText: String = "35 mins left · Ends 11:00"
)

/**
 * Top App Bar with Calendar Icon, Title, Program Pill, Week Subtitle,
 * Notification Bell with unread dot, and Settings Gear Icon.
 */
@Composable
fun TimetableTopAppBar(
    program: String,
    subtitle: String = "Academic Schedule",
    hasUnreadNotifications: Boolean = true,
    onNotificationClick: () -> Unit,
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDark = LocalAppTheme.current.isDark
    val textPrimary = if (isDark) Color.White else TextPrimaryLight
    val textMuted = if (isDark) Color(0xFF94A3B8) else TextMutedLight
    val capsuleBg = if (isDark) DarkSurfaceCard else SurfaceContainerLowLight
    val borderSubtle = if (isDark) DarkBorderSubtle else BorderSubtleLight
    val iconTint = if (isDark) Color(0xFFCBD5E1) else TextSecondaryLight

    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Calendar Container & Title Block
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onProfileClick() }
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(PrimaryBlue, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.CalendarMonth,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Timetable",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    if (program.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(PrimaryBlue.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = program,
                                color = PrimaryBlue,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = textMuted
                )
            }
        }

        // Right: Notification Bell, Profile Avatar & Settings Gear inside Capsule Pill
        Row(
            modifier = Modifier
                .background(capsuleBg, RoundedCornerShape(20.dp))
                .border(1.dp, borderSubtle, RoundedCornerShape(20.dp))
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.TopEnd) {
                IconButton(onClick = onNotificationClick, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = "Notifications",
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
                if (hasUnreadNotifications) {
                    Box(
                        modifier = Modifier
                            .padding(top = 6.dp, end = 6.dp)
                            .size(6.dp)
                            .background(PrimaryBlue, CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.width(2.dp))

            IconButton(onClick = onProfileClick, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = "Academic Profile",
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Schedule Header Section with "Today's Schedule", Date Subheader,
 * and "All Sessions" Filter Pill.
 */
@Composable
fun ScheduleHeaderSection(
    title: String = "Today's Schedule",
    dateText: String = "Tuesday, Sep 8",
    activeFilter: String = "All Sessions",
    onFilterClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalAppTheme.current.isDark
    val textPrimary = if (isDark) Color.White else TextPrimaryLight
    val textMuted = if (isDark) Color(0xFF94A3B8) else TextMutedLight
    val pillBg = if (isDark) DarkSurfaceCard else SurfaceContainerLowLight
    val borderSubtle = if (isDark) DarkBorderSubtle else BorderSubtleLight

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f, fill = false)) {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = dateText,
                fontSize = 13.sp,
                color = textMuted,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // All Sessions Filter Pill Button (no wrapping)
        Surface(
            onClick = onFilterClicked,
            shape = RoundedCornerShape(14.dp),
            color = pillBg,
            border = BorderStroke(1.dp, borderSubtle)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Tune,
                    contentDescription = "Filter",
                    tint = PrimaryBlue,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = activeFilter,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryBlue,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

/**
 * Spec Day Item for calendar capsules (without day-of-month numbers).
 */
data class SpecDayItem(
    val name: String,
    val isToday: Boolean,
    val isSelected: Boolean
)

/**
 * Refined Day Capsule Strip with TODAY distinction and purely day names (no date numbers).
 */
@Composable
fun DayCapsuleStrip(
    dayItems: List<SpecDayItem>,
    onDaySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalAppTheme.current.isDark

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        dayItems.forEach { item ->
            val isActive = item.isSelected
            val isToday = item.isToday

            val capsuleModifier = when {
                isActive -> {
                    Modifier
                        .weight(1f)
                        .height(44.dp)
                        .shadow(
                            elevation = 4.dp,
                            shape = RoundedCornerShape(12.dp),
                            ambientColor = PrimaryBlue.copy(alpha = 0.35f),
                            spotColor = PrimaryBlue.copy(alpha = 0.45f)
                        )
                        .background(PrimaryBlue, RoundedCornerShape(12.dp))
                        .clickable { onDaySelected(item.name) }
                }
                isToday -> {
                    Modifier
                        .weight(1f)
                        .height(44.dp)
                        .background(
                            if (isDark) PrimaryBlue.copy(alpha = 0.22f) else PrimaryBlue.copy(alpha = 0.12f),
                            RoundedCornerShape(12.dp)
                        )
                        .border(
                            width = 1.5.dp,
                            color = PrimaryBlue,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onDaySelected(item.name) }
                }
                else -> {
                    Modifier
                        .weight(1f)
                        .height(44.dp)
                        .background(
                            if (isDark) DarkSurfaceCard else Color.White,
                            RoundedCornerShape(12.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = if (isDark) DarkBorderSubtle else BorderSubtleLight,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onDaySelected(item.name) }
                }
            }

            Box(
                modifier = capsuleModifier,
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.name.take(3),
                    fontSize = 13.sp,
                    fontWeight = if (isActive || isToday) FontWeight.Bold else FontWeight.Medium,
                    color = when {
                        isActive -> Color.White
                        isToday -> PrimaryBlue
                        isDark -> Color.White
                        else -> TextSecondaryLight
                    }
                )
            }
        }
    }
}

/**
 * Refined Day Capsule Strip:
 * Backward compatible index-based strip for 7 days (Mon-Sun) with distinct today color.
 */
@Composable
fun DayCapsuleStrip(
    selectedDayIndex: Int,
    onDaySelected: (Int, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val isDark = LocalAppTheme.current.isDark
    val todayIndex = run {
        val dow = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK)
        if (dow == java.util.Calendar.SUNDAY) 6 else dow - 2
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        days.forEachIndexed { index, day ->
            val isActive = selectedDayIndex == index
            val isToday = index == todayIndex

            val capsuleModifier = when {
                isActive -> {
                    Modifier
                        .weight(1f)
                        .height(44.dp)
                        .shadow(
                            elevation = 4.dp,
                            shape = RoundedCornerShape(12.dp),
                            ambientColor = PrimaryBlue.copy(alpha = 0.35f),
                            spotColor = PrimaryBlue.copy(alpha = 0.45f)
                        )
                        .background(PrimaryBlue, RoundedCornerShape(12.dp))
                        .clickable { onDaySelected(index, day) }
                }
                isToday -> {
                    Modifier
                        .weight(1f)
                        .height(44.dp)
                        .background(
                            if (isDark) PrimaryBlue.copy(alpha = 0.22f) else PrimaryBlue.copy(alpha = 0.12f),
                            RoundedCornerShape(12.dp)
                        )
                        .border(
                            width = 1.5.dp,
                            color = PrimaryBlue,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onDaySelected(index, day) }
                }
                else -> {
                    Modifier
                        .weight(1f)
                        .height(44.dp)
                        .background(
                            if (isDark) DarkSurfaceCard else Color.White,
                            RoundedCornerShape(12.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = if (isDark) DarkBorderSubtle else BorderSubtleLight,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onDaySelected(index, day) }
                }
            }

            Box(
                modifier = capsuleModifier,
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = day,
                    fontSize = 13.sp,
                    fontWeight = if (isActive || isToday) FontWeight.Bold else FontWeight.Medium,
                    color = when {
                        isActive -> Color.White
                        isToday -> PrimaryBlue
                        isDark -> Color.White
                        else -> TextSecondaryLight
                    }
                )
            }
        }
    }
}

/**
 * "Next Up" Hero Card (Interval Mode - as shown in unnamed (2).png and unnamed (3).png):
 * - NEXT UP badge (with green dot)
 * - ⏱ in 25 mins countdown
 * - Course Code & Type: SWE2102 • Theory Session
 * - Requirements Engineering + Topics
 * - Venue (FCI Lab 6) & Lecturer (Dr. Gloria Munguci) pills
 * - Action buttons: Set 15m Alarm & Quick Note
 */
@Composable
fun NextUpHeroCard(
    session: SpecClassSession,
    onSetAlarm: () -> Unit,
    onQuickNote: () -> Unit,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .shadow(elevation = 8.dp, shape = HeroCardShape, ambientColor = PrimaryBlue.copy(alpha = 0.4f))
            .background(
                brush = Brush.verticalGradient(listOf(HeroGradientStart, HeroGradientEnd)),
                shape = HeroCardShape
            )
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Column {
            // Status Header Row: NEXT UP & in 25 mins
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Next Up Pill
                Row(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(StatusGreenLive, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "NEXT UP",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                // Time delta Pill
                Row(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = session.startsInText ?: if (session.startTime.isNotBlank()) "Starts ${session.startTime}" else "Upcoming",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Course Tag Row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = session.courseCode,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "• ${session.type.label}",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title & Description
            Text(
                text = session.courseName,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            session.topicDescription?.let { desc ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = desc,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }

            val hasVenue = !session.venue.isNullOrBlank()
            val hasLecturer = !session.lecturer.isNullOrBlank()

            if (hasVenue || hasLecturer) {
                Spacer(modifier = Modifier.height(16.dp))

                // Venue & Lecturer Meta Pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (hasVenue) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .background(Color.White.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.MeetingRoom,
                                    contentDescription = "Venue",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Venue", fontSize = 10.sp, color = Color.White.copy(0.7f))
                                Text(
                                    text = session.venue!!,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    if (hasLecturer) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .background(Color.White.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Person,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Lecturer", fontSize = 10.sp, color = Color.White.copy(0.7f))
                                Text(
                                    text = session.lecturer!!,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Action Buttons: Set 15m Alarm & Quick Note
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Set Alarm Button
                Button(
                    onClick = onSetAlarm,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (session.isAlarmSet) Color(0xFFDCFCE7) else Color.White,
                        contentColor = if (session.isAlarmSet) Color(0xFF15803D) else PrimaryBlue
                    ),
                    shape = ActionButtonShape,
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(
                        imageVector = if (session.isAlarmSet) Icons.Outlined.CheckCircle else Icons.Outlined.Alarm,
                        contentDescription = if (session.isAlarmSet) "Alarm is active" else "Set 15m Alarm",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (session.isAlarmSet) "Alarm Set (15m)" else "Set 15m Alarm",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Quick Note Button
                Button(
                    onClick = onQuickNote,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.2f),
                        contentColor = Color.White
                    ),
                    shape = ActionButtonShape,
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.EditNote,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Quick Note",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * "Happening Now" Hero Card (In-Class Mode from Specification v2.0):
 * - Live Pulsing Indicator Dot + HAPPENING NOW
 * - Countdown remaining + Live Progress Bar
 * - In-Class Quick Note & Alarm / Dismiss actions
 */
@Composable
fun HappeningNowHeroCard(
    session: SpecClassSession,
    progress: Float = session.progress,
    elapsedText: String = session.elapsedText,
    remainingText: String = session.remainingText,
    onQuickNote: () -> Unit,
    onAlarmDismiss: () -> Unit,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .shadow(elevation = 8.dp, shape = HeroCardShape, ambientColor = PrimaryBlue.copy(alpha = 0.4f))
            .background(
                brush = Brush.verticalGradient(listOf(HeroGradientStart, HeroGradientEnd)),
                shape = HeroCardShape
            )
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Column {
            // Status Header: HAPPENING NOW & Countdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Live Badge
                HappeningNowLiveBadge()

                // Remaining Badge
                Row(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = remainingText,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Course Tag & Hours
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.22f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = session.courseCode,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "• ${session.type.label} Session · ${session.startTime} – ${session.endTime}",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Course Title & Description
            Text(
                text = session.courseName,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            session.topicDescription?.let { desc ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = desc,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // LIVE PROGRESS TRACK COMPONENT
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .background(ProgressIndicatorActive, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = elapsedText,
                            fontSize = 11.sp,
                            color = Color.White.copy(0.9f),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Text(
                        text = remainingText,
                        fontSize = 11.sp,
                        color = ProgressIndicatorActive,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Progress Indicator Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(ProgressTrackShape)
                        .background(ProgressTrackBackground)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = progress.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .clip(ProgressTrackShape)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(ProgressGradientStart, ProgressGradientEnd)
                                )
                            )
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${session.startTime} AM",
                        fontSize = 10.sp,
                        color = Color.White.copy(0.6f)
                    )
                    Text(
                        text = "${(progress * 100).toInt()}% completed",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(0.85f)
                    )
                    Text(
                        text = "${session.endTime} AM",
                        fontSize = 10.sp,
                        color = Color.White.copy(0.6f)
                    )
                }
            }

            val hasVenue = !session.venue.isNullOrBlank()
            val hasLecturer = !session.lecturer.isNullOrBlank()

            if (hasVenue || hasLecturer) {
                Spacer(modifier = Modifier.height(16.dp))

                // Venue & Lecturer Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (hasVenue) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .background(Color.White.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.MeetingRoom,
                                    contentDescription = "Venue",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Venue", fontSize = 10.sp, color = Color.White.copy(0.7f))
                                Text(
                                    text = session.venue!!,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    if (hasLecturer) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .background(Color.White.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Person,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Lecturer", fontSize = 10.sp, color = Color.White.copy(0.7f))
                                Text(
                                    text = session.lecturer!!,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // In-Class Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onQuickNote,
                    modifier = Modifier.weight(1f).height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = PrimaryBlue),
                    shape = ActionButtonShape,
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(Icons.Outlined.EditNote, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Quick Note", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onAlarmDismiss,
                    modifier = Modifier.weight(1f).height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(0.2f), contentColor = Color.White),
                    shape = ActionButtonShape,
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(Icons.Outlined.Alarm, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Alarm / Dismiss", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun HappeningNowLiveBadge() {
    val infiniteTransition = rememberInfiniteTransition(label = "LivePulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AlphaPulse"
    )

    Row(
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .graphicsLayer { this.alpha = alpha }
                .background(StatusGreenLive, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "HAPPENING NOW",
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}

/**
 * Docked "Up Next" Teaser Component:
 * Sits directly below HappeningNowHeroCard to preview the subsequent lecture.
 */
@Composable
fun DockedUpNextBar(
    session: SpecClassSession,
    startsInText: String = "In 35m",
    onSessionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalAppTheme.current.isDark
    val containerColor = if (isDark) DarkSurfaceCard else SurfaceContainerLowLight
    val borderSubtle = if (isDark) DarkBorderSubtle else BorderSubtleLight
    val textPrimary = if (isDark) Color.White else TextPrimaryLight

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clickable(onClick = onSessionClick),
        shape = UpNextBarShape,
        color = containerColor,
        border = BorderStroke(1.dp, borderSubtle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(PrimaryBlue.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FastForward,
                        contentDescription = "Up Next",
                        tint = PrimaryBlue,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "UP NEXT • ${session.startTime}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "• ${session.courseCode}",
                            fontSize = 11.sp,
                            color = if (isDark) Color(0xFF94A3B8) else TextMutedLight
                        )
                    }
                    Text(
                        text = session.courseName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textPrimary,
                        maxLines = 1
                    )
                }
            }

            Box(
                modifier = Modifier
                    .background(PrimaryBlue.copy(alpha = 0.08f), PillBadgeShape)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = startsInText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryBlue
                )
            }
        }
    }
}

/**
 * Timeline Class Card (Chronological timeline item from Specification):
 * - 4dp colored left category accent bar (Theory #2563EB, Lab #7C3AED, Lecture #059669)
 * - Time span & Category Badge & Bell Reminder toggle
 * - Title & Code · Department
 * - Venue, Lecturer, and live/remaining countdown badge
 * - Attached note link (if present)
 */
@Composable
fun TimelineClassCard(
    session: SpecClassSession,
    onToggleAlarm: () -> Unit,
    onAttachedNoteClick: () -> Unit,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val categoryColor = session.type.tagColor
    val isDark = LocalAppTheme.current.isDark
    val cardBg = if (isDark) DarkSurfaceCard else Color.White
    val borderSubtle = if (isDark) DarkBorderSubtle else BorderSubtleLight
    val textPrimary = if (isDark) Color.White else TextPrimaryLight
    val textSecondary = if (isDark) Color(0xFF94A3B8) else TextSecondaryLight
    val textTertiary = if (isDark) Color(0xFF64748B) else TextTertiaryLight
    val noteBg = if (isDark) DarkSurfaceContainer else SurfaceContainerLowLight

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .shadow(elevation = 2.dp, shape = TimelineCardShape)
            .background(cardBg, TimelineCardShape)
            .border(1.dp, borderSubtle, TimelineCardShape)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Left Category Bar (4dp)
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                    .background(categoryColor)
            )

            // Card Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp)
            ) {
                // Time & Type Badge Row + Alert Bell
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${session.startTime} – ${session.endTime}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(categoryColor.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = session.type.label,
                                color = categoryColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    IconButton(
                        onClick = onToggleAlarm,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = "Set Reminder",
                            tint = if (session.isAlarmSet) PrimaryBlue else textTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Title
                Text(
                    text = session.courseName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )

                // Subtitle: Code · Department
                Text(
                    text = "${session.courseCode} · ${session.department}",
                    fontSize = 12.sp,
                    color = textSecondary
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Venue & Lecturer Meta Row
                val hasVenue = !session.venue.isNullOrBlank()
                val hasLecturer = !session.lecturer.isNullOrBlank()
                val hasStartsIn = session.startsInText != null

                if (hasVenue || hasLecturer || hasStartsIn) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (hasVenue) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.MeetingRoom,
                                    contentDescription = "Venue",
                                    tint = textSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = session.venue!!,
                                    fontSize = 12.sp,
                                    color = textSecondary
                                )
                            }
                        }
                        if (hasVenue && hasLecturer) {
                            Text("•", color = textTertiary)
                        }
                        if (hasLecturer) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Person,
                                    contentDescription = "Lecturer",
                                    tint = textSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = session.lecturer!!,
                                    fontSize = 12.sp,
                                    color = textSecondary
                                )
                            }
                        }
                        if ((hasVenue || hasLecturer) && hasStartsIn) {
                            Text("•", color = textTertiary)
                        }
                        if (hasStartsIn) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .background(StatusGreenLive, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = session.startsInText!!,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = StatusGreenLive
                                )
                            }
                        }
                    }
                }

                // Attached Note Link
                session.attachedNotePreview?.takeIf { it.isNotBlank() }?.let { notePreview ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(noteBg, RoundedCornerShape(10.dp))
                            .clickable(onClick = onAttachedNoteClick)
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Description,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "1 attached note: \"$notePreview\"",
                                fontSize = 11.sp,
                                color = textSecondary,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                        Icon(
                            imageVector = Icons.Outlined.ChevronRight,
                            contentDescription = null,
                            tint = textTertiary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Offline Archive Card:
 * Fallback status card indicating cached local schedule readiness.
 */
@Composable
fun OfflineArchiveCard(
    onViewClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalAppTheme.current.isDark
    val cardBg = if (isDark) DarkSurfaceCard else Color.White
    val borderSubtle = if (isDark) DarkBorderSubtle else BorderSubtleLight
    val textPrimary = if (isDark) Color.White else TextPrimaryLight
    val textMuted = if (isDark) Color(0xFF94A3B8) else TextMutedLight

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(14.dp),
        color = cardBg,
        border = BorderStroke(1.dp, borderSubtle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Schedule thumbnail / folder icon
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(PrimaryBlue.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FolderOpen,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Offline Archive",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Original imported schedule fallback",
                        fontSize = 12.sp,
                        color = textMuted
                    )
                }
            }

            OutlinedButton(
                onClick = onViewClick,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, borderSubtle),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = PrimaryBlue
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text("View", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
