package com.mustime.features.tasks

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mustime.core.alarm.TaskDateTimeParser
import com.mustime.features.timetable.domain.Assignment
import com.mustime.features.timetable.ui.DarkBorderSubtle
import com.mustime.features.timetable.ui.DarkSurfaceBase
import com.mustime.features.timetable.ui.DarkSurfaceCard
import com.mustime.ui.LocalAppTheme
import java.text.SimpleDateFormat
import java.util.*

data class DayProgress(
    val dayOfWeek: Int,
    val dayLabel: String,
    val isToday: Boolean,
    val completedCount: Int,
    val pendingCount: Int
)

data class TaskDashboardMetrics(
    val rangeLabel: String,
    val totalCount: Int,
    val completedCount: Int,
    val pendingCount: Int,
    val completionPercentage: Int,
    val days: List<DayProgress>,
    val highPriorityPending: Int
)

object TaskMetricsCalculator {

    fun calculateWeeklyMetrics(
        assignments: List<Assignment>,
        scopeThisWeekOnly: Boolean = true,
        calendar: Calendar = Calendar.getInstance()
    ): TaskDashboardMetrics {
        val startOfWeek = calendar.clone() as Calendar
        startOfWeek.firstDayOfWeek = Calendar.MONDAY
        startOfWeek.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        startOfWeek.set(Calendar.HOUR_OF_DAY, 0)
        startOfWeek.set(Calendar.MINUTE, 0)
        startOfWeek.set(Calendar.SECOND, 0)
        startOfWeek.set(Calendar.MILLISECOND, 0)

        val endOfWeek = startOfWeek.clone() as Calendar
        endOfWeek.add(Calendar.DAY_OF_WEEK, 7)

        val monthFormat = SimpleDateFormat("MMM d", Locale.getDefault())
        val endDisplay = endOfWeek.clone() as Calendar
        endDisplay.add(Calendar.MILLISECOND, -1000)
        val rangeLabel = if (scopeThisWeekOnly) {
            "${monthFormat.format(startOfWeek.time)} – ${monthFormat.format(endDisplay.time)}"
        } else {
            "All-Time Academic Tasks"
        }

        val todayDow = calendar.get(Calendar.DAY_OF_WEEK)

        val daysOrder = listOf(
            Calendar.MONDAY to "Mon",
            Calendar.TUESDAY to "Tue",
            Calendar.WEDNESDAY to "Wed",
            Calendar.THURSDAY to "Thu",
            Calendar.FRIDAY to "Fri",
            Calendar.SATURDAY to "Sat",
            Calendar.SUNDAY to "Sun"
        )

        val relevantTasks = if (scopeThisWeekOnly) {
            assignments.filter { task ->
                isTaskInWindow(task, startOfWeek.timeInMillis, endOfWeek.timeInMillis)
            }
        } else {
            assignments
        }

        val completedByDay = mutableMapOf<Int, Int>()
        val pendingByDay = mutableMapOf<Int, Int>()

        relevantTasks.forEach { task ->
            val dow = resolveTaskDayOfWeek(task, calendar)
            if (task.completed) {
                completedByDay[dow] = (completedByDay[dow] ?: 0) + 1
            } else {
                pendingByDay[dow] = (pendingByDay[dow] ?: 0) + 1
            }
        }

        val dayBreakdown = daysOrder.map { (dow, label) ->
            DayProgress(
                dayOfWeek = dow,
                dayLabel = label,
                isToday = dow == todayDow,
                completedCount = completedByDay[dow] ?: 0,
                pendingCount = pendingByDay[dow] ?: 0
            )
        }

        val total = relevantTasks.size
        val completed = relevantTasks.count { it.completed }
        val pending = total - completed
        val pct = if (total > 0) (completed * 100) / total else 0
        val highPriority = relevantTasks.count { !it.completed && it.priority.equals("High", ignoreCase = true) }

        return TaskDashboardMetrics(
            rangeLabel = rangeLabel,
            totalCount = total,
            completedCount = completed,
            pendingCount = pending,
            completionPercentage = pct,
            days = dayBreakdown,
            highPriorityPending = highPriority
        )
    }

    private fun isTaskInWindow(task: Assignment, startMillis: Long, endMillis: Long): Boolean {
        val lower = task.dueDate.lowercase(Locale.getDefault())
        if (lower.contains("today") || lower.contains("tomorrow")) return true
        val dayNames = listOf("mon", "tue", "wed", "thu", "fri", "sat", "sun")
        if (dayNames.any { lower.contains(it) }) return true

        val millis = TaskDateTimeParser.calculateDueMillis(task.dueDate)
        if (millis != null) {
            return millis in startMillis until endMillis
        }
        return true
    }

    private fun resolveTaskDayOfWeek(task: Assignment, currentCal: Calendar): Int {
        val millis = TaskDateTimeParser.calculateDueMillis(task.dueDate)
        if (millis != null) {
            val c = Calendar.getInstance().apply { timeInMillis = millis }
            return c.get(Calendar.DAY_OF_WEEK)
        }
        val lower = task.dueDate.lowercase(Locale.getDefault())
        if (lower.contains("today")) return currentCal.get(Calendar.DAY_OF_WEEK)
        if (lower.contains("tomorrow")) {
            val c = currentCal.clone() as Calendar
            c.add(Calendar.DAY_OF_YEAR, 1)
            return c.get(Calendar.DAY_OF_WEEK)
        }
        if (lower.contains("mon")) return Calendar.MONDAY
        if (lower.contains("tue")) return Calendar.TUESDAY
        if (lower.contains("wed")) return Calendar.WEDNESDAY
        if (lower.contains("thu")) return Calendar.THURSDAY
        if (lower.contains("fri")) return Calendar.FRIDAY
        if (lower.contains("sat")) return Calendar.SATURDAY
        if (lower.contains("sun")) return Calendar.SUNDAY

        return currentCal.get(Calendar.DAY_OF_WEEK)
    }
}

/**
 * Visual Progress Dashboard for Academic Tasks
 * Displays:
 * 1. Progress Bar for Total Tasks Completed vs Pending (animated dual-tone segmented bar)
 * 2. Weekly summary metrics with interactive filter triggers
 * 3. Daily task distribution chart (Monday through Sunday) inspired by Recharts/D3 styling
 */
@Composable
fun TaskProgressDashboard(
    assignments: List<Assignment>,
    currentFilter: String,
    onFilterChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalAppTheme.current.isDark
    val cardBg = if (isDark) DarkSurfaceCard else Color.White
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val slotBg = if (isDark) DarkSurfaceBase else Color(0xFFF1F5F9)
    val slotActive = if (isDark) Color(0xFF334155) else Color.White

    var scopeThisWeekOnly by remember { mutableStateOf(true) }

    val metrics = remember(assignments, scopeThisWeekOnly) {
        TaskMetricsCalculator.calculateWeeklyMetrics(assignments, scopeThisWeekOnly)
    }

    val animatedProgress by animateFloatAsState(
        targetValue = if (metrics.totalCount > 0) metrics.completedCount.toFloat() / metrics.totalCount else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "task_progress_animation"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("task_progress_dashboard"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(
                if (isDark) listOf(DarkBorderSubtle, DarkBorderSubtle)
                else listOf(Color(0xFFE2E8F0), Color(0xFFEFF6FF))
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header Row: Title & Scope Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(if (isDark) DarkSurfaceBase else Color(0xFFEFF6FF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.BarChart,
                            contentDescription = "Weekly Progress",
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (scopeThisWeekOnly) "Current Week Progress" else "Total Tasks Progress",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Text(
                            text = metrics.rangeLabel,
                            fontSize = 11.sp,
                            color = textSecondary
                        )
                    }
                }

                // Scope Switcher (This Week vs All Time)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(slotBg)
                        .padding(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (scopeThisWeekOnly) slotActive else Color.Transparent)
                            .clickable { scopeThisWeekOnly = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "Week",
                            fontSize = 11.sp,
                            fontWeight = if (scopeThisWeekOnly) FontWeight.Bold else FontWeight.Medium,
                            color = if (scopeThisWeekOnly) Color(0xFF2563EB) else textSecondary
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (!scopeThisWeekOnly) slotActive else Color.Transparent)
                            .clickable { scopeThisWeekOnly = false }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "All",
                            fontSize = 11.sp,
                            fontWeight = if (!scopeThisWeekOnly) FontWeight.Bold else FontWeight.Medium,
                            color = if (!scopeThisWeekOnly) Color(0xFF2563EB) else textSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Primary Stat Headline & Percentage Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${metrics.completedCount}",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textPrimary
                    )
                    Text(
                        text = " / ${metrics.totalCount} completed",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textSecondary,
                        modifier = Modifier.padding(bottom = 3.dp, start = 4.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (metrics.highPriorityPending > 0) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isDark) Color(0xFF7F1D1D).copy(alpha = 0.4f) else Color(0xFFFEF2F2),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF991B1B) else Color(0xFFFCA5A5))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.WarningAmber,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFFF87171) else Color(0xFFDC2626),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    "${metrics.highPriorityPending} High",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFFFCA5A5) else Color(0xFFB91C1C)
                                )
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (metrics.completionPercentage >= 75) {
                            if (isDark) Color(0xFF064E3B).copy(alpha = 0.5f) else Color(0xFFDCFCE7)
                        } else {
                            if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.5f) else Color(0xFFEFF6FF)
                        }
                    ) {
                        Text(
                            text = "${metrics.completionPercentage}% Done",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (metrics.completionPercentage >= 75) {
                                if (isDark) Color(0xFF6EE7B7) else Color(0xFF15803D)
                            } else {
                                if (isDark) Color(0xFF93C5FD) else Color(0xFF2563EB)
                            },
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Dual-Tone Progress Bar (Completed vs Pending)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                    .testTag("tasks_completed_vs_pending_progress_bar")
            ) {
                if (metrics.totalCount > 0) {
                    // Pending background fill
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF60A5FA), Color(0xFF3B82F6))
                                )
                            )
                    )

                    // Completed foreground fill with animated width
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animatedProgress)
                            .clip(
                                if (animatedProgress >= 0.99f) RoundedCornerShape(7.dp)
                                else RoundedCornerShape(topStart = 7.dp, bottomStart = 7.dp)
                            )
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF10B981), Color(0xFF059669))
                                )
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Interactive Legend Row (Clicking changes task filter below)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Completed indicator
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onFilterChange("Completed") }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(Color(0xFF10B981), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Completed: ${metrics.completedCount} (${metrics.completionPercentage}%)",
                        fontSize = 12.sp,
                        fontWeight = if (currentFilter == "Completed") FontWeight.Bold else FontWeight.Medium,
                        color = if (currentFilter == "Completed") Color(0xFF10B981) else textSecondary
                    )
                }

                // Pending indicator
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onFilterChange("Pending") }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(Color(0xFF3B82F6), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    val pendingPct = if (metrics.totalCount > 0) 100 - metrics.completionPercentage else 0
                    Text(
                        text = "Pending: ${metrics.pendingCount} ($pendingPct%)",
                        fontSize = 12.sp,
                        fontWeight = if (currentFilter == "Pending") FontWeight.Bold else FontWeight.Medium,
                        color = if (currentFilter == "Pending") Color(0xFF60A5FA) else textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = if (isDark) DarkBorderSubtle else Color(0xFFF1F5F9), thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // D3 / Recharts-style Daily Breakdown Bar Chart
            Text(
                text = "Daily Breakdown (Mon – Sun)",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = textSecondary
            )

            Spacer(modifier = Modifier.height(10.dp))

            WeeklyDayBarChart(
                days = metrics.days,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
            )
        }
    }
}

/**
 * Recharts/D3-inspired custom Canvas bar chart component for weekly daily breakdown.
 * Renders stacked vertical bars for completed (green) and pending (blue) tasks per day.
 */
@Composable
fun WeeklyDayBarChart(
    days: List<DayProgress>,
    modifier: Modifier = Modifier
) {
    val maxDayTotal = remember(days) {
        val highest = days.maxOfOrNull { it.completedCount + it.pendingCount } ?: 0
        if (highest < 3) 3 else highest
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        days.forEach { day ->
            DayColumnItem(
                day = day,
                maxTotal = maxDayTotal,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }
    }
}

@Composable
private fun DayColumnItem(
    day: DayProgress,
    maxTotal: Int,
    modifier: Modifier = Modifier
) {
    val isDark = LocalAppTheme.current.isDark
    val total = day.completedCount + day.pendingCount

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (day.isToday) {
                    if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.4f) else Color(0xFFEFF6FF)
                } else Color.Transparent
            )
            .padding(horizontal = 2.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        // Task count badge if > 0
        if (total > 0) {
            Text(
                text = "$total",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (day.isToday) Color(0xFF60A5FA) else (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
            )
        } else {
            Spacer(modifier = Modifier.height(12.dp))
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Custom Canvas Stacked Bar
        Canvas(
            modifier = Modifier
                .width(18.dp)
                .height(48.dp)
        ) {
            val canvasW = size.width
            val canvasH = size.height

            // Background baseline slot
            drawRoundRect(
                color = if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9),
                topLeft = Offset(0f, 0f),
                size = Size(canvasW, canvasH),
                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
            )

            if (total > 0) {
                val totalFraction = (total.toFloat() / maxTotal).coerceIn(0.18f, 1f)
                val barTotalH = canvasH * totalFraction
                val barTopY = canvasH - barTotalH

                val completedFraction = day.completedCount.toFloat() / total
                val completedH = barTotalH * completedFraction
                val pendingH = barTotalH - completedH

                // Pending segment (Blue - on top)
                if (pendingH > 0) {
                    drawRoundRect(
                        color = Color(0xFF3B82F6),
                        topLeft = Offset(0f, barTopY),
                        size = Size(canvasW, pendingH),
                        cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                    )
                }

                // Completed segment (Green - on bottom)
                if (completedH > 0) {
                    drawRoundRect(
                        color = Color(0xFF10B981),
                        topLeft = Offset(0f, canvasH - completedH),
                        size = Size(canvasW, completedH),
                        cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                    )
                }
            } else {
                // Subtle dash for zero tasks on that day
                drawRoundRect(
                    color = if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1),
                    topLeft = Offset(canvasW * 0.2f, canvasH - 6.dp.toPx()),
                    size = Size(canvasW * 0.6f, 3.dp.toPx()),
                    cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Day label (Mon, Tue, etc.)
        Text(
            text = day.dayLabel,
            fontSize = 11.sp,
            fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Medium,
            color = if (day.isToday) Color(0xFF60A5FA) else (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
        )

        // Today indicator dot
        if (day.isToday) {
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .background(Color(0xFF3B82F6), CircleShape)
            )
        } else {
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}
