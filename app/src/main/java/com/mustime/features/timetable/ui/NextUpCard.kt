package com.mustime.features.timetable.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mustime.core.util.TimeUtil
import com.mustime.features.timetable.domain.TimetableEntry

@Composable
fun NextUpCard(entry: TimetableEntry, minutesUntil: Int, onClick: () -> Unit) {
    val style = sessionStyle(entry.sessionType, entry.room ?: "", entry.courseTitle)
    val progress = remember(entry.startTime, entry.endTime, entry.dayOfWeek) {
        TimeUtil.calculateProgress(entry.startTime, entry.endTime, entry.dayOfWeek)
    }

    val isLive = progress.isOngoing || minutesUntil == 0

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .background(
                Brush.horizontalGradient(
                    if (isLive) {
                        listOf(Color(0xFF1E40AF), Color(0xFF4338CA), Color(0xFF6D28D9))
                    } else {
                        listOf(MaterialTheme.colorScheme.primary, Color(0xFF6D28D9))
                    }
                )
            )
            .padding(20.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isLive) {
                    NextUpLiveBadge()
                } else {
                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("NEXT UP", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                val timeBadge = when {
                    progress.isOngoing -> "${progress.remainingMinutes}m left"
                    minutesUntil == 0 -> "Happening now"
                    minutesUntil in 1..59 -> "in ${minutesUntil}m"
                    minutesUntil >= 60 -> "in ${minutesUntil / 60}h ${minutesUntil % 60}m"
                    else -> "on ${entry.dayOfWeek}"
                }
                Text(timeBadge, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            
            Spacer(modifier = Modifier.height(14.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(entry.courseCode, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(resolveSessionTypeLabel(entry), color = Color.White, fontSize = 12.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                entry.courseTitle,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 26.sp
            )
            
            Spacer(modifier = Modifier.height(14.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Schedule, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("${entry.startTime} – ${entry.endTime}", color = Color.White, fontSize = 14.sp)
                
                val roomText = entry.room?.trim()
                if (!roomText.isNullOrEmpty() && roomText.uppercase() !in listOf("TBA", "TBD", "NONE", "N/A")) {
                    Spacer(modifier = Modifier.width(16.dp))
                    Icon(Icons.Outlined.MeetingRoom, contentDescription = "Venue", tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(roomText, color = Color.White, fontSize = 14.sp)
                }
            }

            // ONGOING PROGRESS BAR
            if (isLive) {
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.2f))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    val percent = if (progress.totalMinutes > 0) (progress.progress * 100).toInt() else 0
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Lecture Progress ($percent%)",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (progress.remainingMinutes > 0) "${progress.remainingMinutes} min left" else "Ending soon",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { if (progress.progress > 0f) progress.progress else 0.05f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFF4ADE80),
                        trackColor = Color.White.copy(alpha = 0.25f)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${entry.startTime} (${progress.elapsedMinutes}m elapsed)",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 10.sp
                        )
                        Text(
                            text = "${entry.endTime}",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 10.sp
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(18.dp))
            
            val lecturerText = entry.lecturer?.trim()?.takeIf {
                it.isNotBlank() && !it.equals("Staff", ignoreCase = true) && !it.equals("TBD", ignoreCase = true)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                if (lecturerText != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(lecturerText, color = Color.White, fontSize = 14.sp)
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }
                
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun NextUpLiveBadge() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_nextup")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "nextup_alpha"
    )
    Box(
        modifier = Modifier
            .background(Color(0xFF22C55E).copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .graphicsLayer { this.alpha = pulseAlpha }
                    .background(Color(0xFF4ADE80), CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("HAPPENING NOW", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}
