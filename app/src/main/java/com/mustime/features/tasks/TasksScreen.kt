package com.mustime.features.tasks

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mustime.TimetableApplication
import com.mustime.core.alarm.TaskAlarmScheduler
import com.mustime.core.alarm.TaskDateTimeParser
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.style.TextDecoration
import com.mustime.features.timetable.domain.TaskCategory
import java.util.Calendar
import java.util.Locale
import com.mustime.features.timetable.domain.Assignment
import com.mustime.features.timetable.ui.*
import com.mustime.ui.LocalAppTheme
import com.mustime.ui.components.*
import com.mustime.ui.components.AcademicProfileSheet
import com.mustime.ui.components.NotificationCenterSheet

enum class TaskDueStatus {
    OVERDUE,
    DUE_TODAY,
    DUE_TOMORROW,
    UPCOMING,
    NO_DATE
}

fun getTaskDueStatus(dueDate: String, isCompleted: Boolean): TaskDueStatus {
    if (isCompleted) return TaskDueStatus.UPCOMING
    val millis = TaskDateTimeParser.calculateDueMillis(dueDate) ?: return TaskDueStatus.NO_DATE
    val now = Calendar.getInstance()
    val dueCal = Calendar.getInstance().apply { timeInMillis = millis }

    if (millis < now.timeInMillis) {
        return TaskDueStatus.OVERDUE
    }

    val isSameDay = now.get(Calendar.YEAR) == dueCal.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == dueCal.get(Calendar.DAY_OF_YEAR)
    if (isSameDay) return TaskDueStatus.DUE_TODAY

    val tomorrowCal = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
    val isTomorrow = tomorrowCal.get(Calendar.YEAR) == dueCal.get(Calendar.YEAR) &&
            tomorrowCal.get(Calendar.DAY_OF_YEAR) == dueCal.get(Calendar.DAY_OF_YEAR)
    if (isTomorrow) return TaskDueStatus.DUE_TOMORROW

    return TaskDueStatus.UPCOMING
}

fun resolveTaskCategoryAndTitle(title: String): Pair<TaskCategory, String> {
    val trimmed = title.trim()
    if (trimmed.startsWith("[") && trimmed.contains("]")) {
        val closeBracket = trimmed.indexOf("]")
        val categoryStr = trimmed.substring(1, closeBracket).trim()
        val cleanTitle = trimmed.substring(closeBracket + 1).trim()
        val cat = TaskCategory.fromName(categoryStr)
        return cat to cleanTitle
    }
    val lower = trimmed.lowercase(Locale.ROOT)
    val inferredCat = when {
        lower.contains("lab report") || lower.contains("practical") -> TaskCategory.LAB_REPORT
        lower.contains("project") || lower.contains("capstone") -> TaskCategory.PROJECT
        lower.contains("quiz") || lower.contains("exam") || lower.contains("test") -> TaskCategory.EXAM_PREP
        lower.contains("reading") || lower.contains("chapter") -> TaskCategory.READING
        else -> TaskCategory.ASSIGNMENT
    }
    return inferredCat to trimmed
}

@Composable
fun TasksScreen(
    onBack: (() -> Unit)? = null,
    onSettingsClick: () -> Unit = {},
    onReconfigureAcademicProfile: () -> Unit = {}
) {
    val context = LocalContext.current
    val app = context.applicationContext as? TimetableApplication
    val repository = app?.repository

    if (repository == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    val isDark = LocalAppTheme.current.isDark
    val bg = if (isDark) DarkSurfaceBase else SurfaceBaseLight
    val cardBg = if (isDark) DarkSurfaceCard else Color.White
    val textPrimary = if (isDark) Color.White else TextPrimaryLight
    val textSecondary = if (isDark) Color(0xFF94A3B8) else TextMutedLight
    val borderColor = if (isDark) DarkBorderSubtle else Color(0xFFE2E8F0)
    val primaryColor = MaterialTheme.colorScheme.primary

    val savedProgramme by repository.programmePref.collectAsState(initial = repository.getInitialProgramme())
    val allEntries by repository.getAllEntries().collectAsState(initial = emptyList())
    val availableCoursePairs = remember(allEntries) {
        allEntries.map { it.courseCode to it.courseTitle }.distinctBy { it.first }
    }
    val alarmScheduler = remember(context) { TaskAlarmScheduler(context) }

    val viewModel: TasksViewModel = viewModel(
        factory = TasksViewModel.provideFactory(repository, alarmScheduler)
    )

    val uiState by viewModel.uiState.collectAsState()
    val assignments = uiState.assignments

    var showAddDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<Assignment?>(null) }
    var showNotificationSheet by remember { mutableStateOf(false) }
    var showProfileSheet by remember { mutableStateOf(false) }

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    // Check notification permission on launch for Android 13+
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    var taskToDelete by remember { mutableStateOf<Assignment?>(null) }

    val filteredTasks = remember(assignments, uiState.searchQuery, uiState.selectedFilter) {
        assignments.filter { task ->
            val status = getTaskDueStatus(task.dueDate, task.completed)
            val matchesFilter = when (uiState.selectedFilter) {
                "Pending" -> !task.completed
                "Overdue" -> !task.completed && status == TaskDueStatus.OVERDUE
                "Completed" -> task.completed
                "High Priority" -> !task.completed && task.priority.equals("High", ignoreCase = true)
                else -> true
            }
            val query = uiState.searchQuery.trim()
            val matchesSearch = query.isBlank() ||
                    task.title.contains(query, ignoreCase = true) ||
                    task.courseCode.contains(query, ignoreCase = true) ||
                    task.notes.contains(query, ignoreCase = true)
            matchesFilter && matchesSearch
        }.sortedWith(
            compareBy<Assignment> { it.completed }
                .thenByDescending { getTaskDueStatus(it.dueDate, it.completed) == TaskDueStatus.OVERDUE }
                .thenBy { TaskDateTimeParser.calculateDueMillis(it.dueDate) ?: Long.MAX_VALUE }
                .thenByDescending { it.priority.equals("High", ignoreCase = true) }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = bg,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    taskToEdit = null
                    showAddDialog = true
                },
                containerColor = primaryColor,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                icon = {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                },
                text = {
                    Text(
                        text = "Add Task",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                },
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 96.dp)
                    .testTag("add_task_fab")
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(bg)
                .padding(innerPadding)
                .statusBarsPadding()
        ) {
            // Top Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = textPrimary
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(primaryColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.TaskAlt,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Tasks & Deadlines",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Text(
                        "Reminders & Deadlines",
                        fontSize = 11.sp,
                        color = textSecondary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isDark) DarkSurfaceCard else Color(0xFFEFF6FF))
                            .clickable { showProfileSheet = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "Academic Profile",
                            tint = primaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

        // Notification Permission Prompt (if revoked on Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF78350F).copy(alpha = 0.4f) else Color(0xFFFEF3C7)
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFFFCD34D) else Color(0xFFD97706)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "Enable notifications to receive deadline alarms.",
                            fontSize = 12.sp,
                            color = if (isDark) Color(0xFFFDE68A) else Color(0xFF92400E)
                        )
                    }
                    TextButton(onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                        Text(
                            "Allow",
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309)
                        )
                    }
                }
            }
        }

        // Message Banner
        AnimatedVisibility(
            visible = uiState.messageSnackbar != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            uiState.messageSnackbar?.let { msg ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF334155) else Color(0xFF1E293B)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(msg, color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = { viewModel.clearMessage() }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }

        if (uiState.isLoading && assignments.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = primaryColor,
                        modifier = Modifier.size(36.dp),
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Loading tasks...", color = textSecondary, fontSize = 14.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 150.dp)
            ) {
                // 1. Visual Progress Dashboard for Total Tasks Completed vs Pending
                item(key = "task_progress_dashboard_item") {
                    TaskProgressDashboard(
                        assignments = assignments,
                        currentFilter = uiState.selectedFilter,
                        onFilterChange = { viewModel.setFilter(it) }
                    )
                }

                // 2. Search Field
                item(key = "search_field_item") {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("Search by title or course...", fontSize = 14.sp, color = textSecondary) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = textSecondary, modifier = Modifier.size(20.dp)) },
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = borderColor,
                            focusedContainerColor = cardBg,
                            unfocusedContainerColor = cardBg,
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary
                        )
                    )
                }

                // 3. Filter Chips
                item(key = "filter_chips_item") {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val filters = listOf("All", "Pending", "Overdue", "Completed", "High Priority")
                        items(filters) { filter ->
                            val isSelected = uiState.selectedFilter == filter
                            val count = when (filter) {
                                "Pending" -> assignments.count { !it.completed }
                                "Overdue" -> assignments.count { !it.completed && getTaskDueStatus(it.dueDate, it.completed) == TaskDueStatus.OVERDUE }
                                "Completed" -> assignments.count { it.completed }
                                "High Priority" -> assignments.count { !it.completed && it.priority.equals("High", ignoreCase = true) }
                                else -> assignments.size
                            }
                            val isOverdueFilter = filter == "Overdue"
                            val chipBg = when {
                                isSelected && isOverdueFilter -> Color(0xFFDC2626)
                                isSelected -> primaryColor
                                isOverdueFilter && count > 0 -> if (isDark) Color(0xFF7F1D1D).copy(alpha = 0.4f) else Color(0xFFFEE2E2)
                                else -> cardBg
                            }
                            val chipBorder = when {
                                isSelected && isOverdueFilter -> Color(0xFFDC2626)
                                isSelected -> primaryColor
                                isOverdueFilter && count > 0 -> if (isDark) Color(0xFFB91C1C) else Color(0xFFFCA5A5)
                                else -> borderColor
                            }
                            val chipTextColor = when {
                                isSelected -> Color.White
                                isOverdueFilter && count > 0 -> if (isDark) Color(0xFFFCA5A5) else Color(0xFFDC2626)
                                else -> textSecondary
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(chipBg)
                                    .border(1.dp, chipBorder, RoundedCornerShape(16.dp))
                                    .clickable { viewModel.setFilter(filter) }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isOverdueFilter && count > 0 && !isSelected) {
                                        Icon(
                                            Icons.Default.WarningAmber,
                                            contentDescription = null,
                                            tint = chipTextColor,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = "$filter ($count)",
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = chipTextColor
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. Task items or Empty State
                if (filteredTasks.isEmpty()) {
                    item(key = "empty_tasks_state") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .background(primaryColor.copy(alpha = 0.12f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Outlined.Assignment,
                                        contentDescription = null,
                                        tint = primaryColor,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    if (uiState.searchQuery.isNotBlank()) "No tasks match '${uiState.searchQuery}'" else "No ${uiState.selectedFilter.lowercase()} tasks",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "Click the '+' button to schedule an assignment with an exact AlarmManager reminder.",
                                    fontSize = 13.sp,
                                    color = textSecondary,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                } else {
                    items(filteredTasks, key = { it.id }) { task ->
                        TaskCard(
                            task = task,
                            isDark = isDark,
                            onToggleComplete = { viewModel.toggleComplete(task) },
                            onEdit = {
                                taskToEdit = task
                                showAddDialog = true
                            },
                            onDelete = { taskToDelete = task },
                            onTestAlarm = { viewModel.triggerTestReminder(task, delaySeconds = 3) }
                        )
                    }
                }
            }
        }
    }
}

    if (showAddDialog) {
        AddTaskDialog(
            isDark = isDark,
            initialTask = taskToEdit,
            availableCourses = availableCoursePairs,
            onDismiss = {
                showAddDialog = false
                taskToEdit = null
            },
            onConfirm = { title, course, dueDate, priority, reminderMinutes, notes, category ->
                val fullTitle = if (category != TaskCategory.GENERAL) {
                    "[${category.displayName}] ${title.trim()}"
                } else {
                    title.trim()
                }
                val currentTask = taskToEdit
                if (currentTask != null) {
                    viewModel.updateAssignment(
                        currentTask.copy(
                            title = fullTitle,
                            courseCode = course,
                            dueDate = dueDate,
                            priority = priority,
                            reminderMinutes = reminderMinutes,
                            notes = notes
                        )
                    )
                } else {
                    viewModel.saveAssignment(
                        title = fullTitle,
                        courseCode = course,
                        dueDate = dueDate,
                        priority = priority,
                        reminderMinutes = reminderMinutes,
                        notes = notes
                    )
                }
                showAddDialog = false
                taskToEdit = null
            }
        )
    }

    if (taskToDelete != null) {
        val task = taskToDelete!!
        AlertDialog(
            onDismissRequest = { taskToDelete = null },
            icon = {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text("Delete Task?", fontWeight = FontWeight.Bold, color = textPrimary)
            },
            text = {
                Text(
                    "Are you sure you want to delete '${task.title}'? Any scheduled AlarmManager reminders for this task will be cancelled.",
                    color = textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAssignment(task.id)
                        taskToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { taskToDelete = null }) {
                    Text("Cancel", color = textSecondary)
                }
            }
        )
    }

    if (showNotificationSheet) {
        NotificationCenterSheet(onDismiss = { showNotificationSheet = false })
    }

    if (showProfileSheet) {
        AcademicProfileSheet(
            programme = savedProgramme ?: "",
            onDismiss = { showProfileSheet = false },
            onReconfigureProfile = {
                showProfileSheet = false
                onReconfigureAcademicProfile()
            },
            onOpenSettings = {
                showProfileSheet = false
                onSettingsClick()
            }
        )
    }
}

@Composable
fun TaskCard(
    task: Assignment,
    isDark: Boolean,
    onToggleComplete: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTestAlarm: () -> Unit
) {
    val cardBg = if (isDark) DarkSurfaceCard else Color.White
    val textPrimary = if (isDark) Color.White else TextPrimaryLight
    val textSecondary = if (isDark) Color(0xFF94A3B8) else TextMutedLight
    val primaryColor = MaterialTheme.colorScheme.primary

    val (category, cleanTitle) = remember(task.title) { resolveTaskCategoryAndTitle(task.title) }
    val dueStatus = remember(task.dueDate, task.completed) { getTaskDueStatus(task.dueDate, task.completed) }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (isDark) {
                    if (!task.completed && dueStatus == TaskDueStatus.OVERDUE) Color(0xFF991B1B)
                    else DarkBorderSubtle
                } else {
                    if (!task.completed && dueStatus == TaskDueStatus.OVERDUE) Color(0xFFFCA5A5)
                    else Color(0xFFE2E8F0)
                }
            )
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(24.dp)
                            .clip(CircleShape)
                            .border(
                                2.dp,
                                if (task.completed) primaryColor else (if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1)),
                                CircleShape
                            )
                            .background(if (task.completed) primaryColor else Color.Transparent)
                            .clickable(onClick = onToggleComplete),
                        contentAlignment = Alignment.Center
                    ) {
                        if (task.completed) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Completed",
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        // Category Pill
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isDark) category.color.copy(alpha = 0.22f) else category.lightContainerColor,
                            border = BorderStroke(1.dp, category.color.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    category.icon,
                                    contentDescription = null,
                                    tint = category.color,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    category.displayName,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = category.color
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = cleanTitle,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (task.completed) textSecondary else textPrimary,
                            textDecoration = if (task.completed) TextDecoration.LineThrough else null,
                            lineHeight = 20.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Edit / Reschedule button
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit or Reschedule Task",
                            tint = primaryColor,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(2.dp))

                    // Test reminder trigger button
                    IconButton(
                        onClick = onTestAlarm,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.NotificationsActive,
                            contentDescription = "Test Alarm (3s)",
                            tint = primaryColor,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(2.dp))

                    // Delete button
                    IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Delete",
                            tint = textSecondary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }

            // Task Notes Preview (if user added notes)
            if (task.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDark) DarkSurfaceBase else Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, if (isDark) DarkBorderSubtle else Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 36.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.Description,
                            contentDescription = null,
                            tint = textSecondary,
                            modifier = Modifier
                                .padding(top = 1.dp)
                                .size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = task.notes,
                            fontSize = 12.sp,
                            color = textSecondary,
                            maxLines = 3,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Metadata & Badges row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 36.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Status Chip (Overdue, Due Today, Due Tomorrow, Done)
                if (task.completed) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isDark) Color(0xFF064E3B).copy(alpha = 0.5f) else Color(0xFFDCFCE7)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                null,
                                tint = if (isDark) Color(0xFF6EE7B7) else Color(0xFF16A34A),
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                "Done",
                                color = if (isDark) Color(0xFF6EE7B7) else Color(0xFF15803D),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                } else when (dueStatus) {
                    TaskDueStatus.OVERDUE -> {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isDark) Color(0xFF7F1D1D).copy(alpha = 0.6f) else Color(0xFFFEE2E2),
                            border = BorderStroke(1.dp, if (isDark) Color(0xFFB91C1C) else Color(0xFFFCA5A5))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.WarningAmber,
                                    null,
                                    tint = if (isDark) Color(0xFFFCA5A5) else Color(0xFFDC2626),
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(Modifier.width(3.dp))
                                Text(
                                    "Overdue",
                                    color = if (isDark) Color(0xFFFCA5A5) else Color(0xFFB91C1C),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    TaskDueStatus.DUE_TODAY -> {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isDark) Color(0xFF78350F).copy(alpha = 0.5f) else Color(0xFFFEF3C7)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.AccessTime,
                                    null,
                                    tint = if (isDark) Color(0xFFFCD34D) else Color(0xFFD97706),
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(Modifier.width(3.dp))
                                Text(
                                    "Today",
                                    color = if (isDark) Color(0xFFFCD34D) else Color(0xFFB45309),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    TaskDueStatus.DUE_TOMORROW -> {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.4f) else Color(0xFFEFF6FF)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CalendarToday,
                                    null,
                                    tint = primaryColor,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(Modifier.width(3.dp))
                                Text(
                                    "Tomorrow",
                                    color = primaryColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    else -> {}
                }

                // 2. Priority Badge
                val (badgeBg, badgeText) = when (task.priority.lowercase(Locale.ROOT)) {
                    "high" -> {
                        if (isDark) Color(0xFF7F1D1D).copy(alpha = 0.5f) to Color(0xFFFCA5A5)
                        else Color(0xFFFFE4E6) to Color(0xFFE11D48)
                    }
                    "medium" -> {
                        if (isDark) Color(0xFF78350F).copy(alpha = 0.5f) to Color(0xFFFCD34D)
                        else Color(0xFFFEF3C7) to Color(0xFFD97706)
                    }
                    else -> {
                        if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.5f) to Color(0xFF93C5FD)
                        else Color(0xFFEFF6FF) to Color(0xFF2563EB)
                    }
                }

                Box(
                    modifier = Modifier
                        .background(badgeBg, RoundedCornerShape(6.dp))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        task.priority.replaceFirstChar { it.uppercase() },
                        color = badgeText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 3. Course Code
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isDark) DarkSurfaceBase else Color(0xFFF1F5F9)
                ) {
                    Text(
                        task.courseCode,
                        fontSize = 11.sp,
                        color = textSecondary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 4. Due Date
                Icon(
                    Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706),
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(task.dueDate, fontSize = 12.sp, color = textSecondary)

                // 5. Alarm Pill
                if (task.reminderMinutes != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                if (task.completed) {
                                    if (isDark) DarkSurfaceBase else Color(0xFFF1F5F9)
                                } else {
                                    if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.4f) else Color(0xFFEFF6FF)
                                },
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = null,
                                tint = if (task.completed) textSecondary else primaryColor,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                if (task.completed) "Off" else "${task.reminderMinutes}m",
                                fontSize = 10.sp,
                                color = if (task.completed) textSecondary else primaryColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddTaskDialog(
    isDark: Boolean,
    initialTask: Assignment? = null,
    availableCourses: List<Pair<String, String>> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (title: String, course: String, dueDate: String, priority: String, reminder: Int?, notes: String, category: TaskCategory) -> Unit
) {
    val initialCatAndTitle = remember(initialTask) {
        if (initialTask != null) resolveTaskCategoryAndTitle(initialTask.title)
        else TaskCategory.ASSIGNMENT to ""
    }

    var title by remember(initialTask) { mutableStateOf(initialCatAndTitle.second) }
    var category by remember(initialTask) { mutableStateOf(initialCatAndTitle.first) }
    var course by remember(initialTask) { mutableStateOf(initialTask?.courseCode ?: "") }
    var dueDate by remember(initialTask) {
        mutableStateOf(TaskDateTimeParser.normalizeDueDate(initialTask?.dueDate))
    }
    var priority by remember(initialTask) { mutableStateOf(initialTask?.priority ?: "Medium") }
    var reminderMinutes by remember(initialTask) { mutableStateOf<Int?>(initialTask?.reminderMinutes ?: 1440) }
    var notes by remember(initialTask) { mutableStateOf(initialTask?.notes ?: "") }

    var showCoursePicker by remember { mutableStateOf(false) }
    var showDueCalendarPicker by remember { mutableStateOf(false) }
    var titleTouched by remember { mutableStateOf(false) }
    var courseTouched by remember { mutableStateOf(false) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val textPrimary = if (isDark) Color.White else TextPrimaryLight
    val textSecondary = if (isDark) Color(0xFF94A3B8) else TextMutedLight

    val schedulePreview = remember(dueDate, reminderMinutes) {
        TaskDateTimeParser.formatSchedulePreview(dueDate, reminderMinutes)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (initialTask != null) Icons.Outlined.Edit else Icons.Default.Alarm,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (initialTask != null) "Edit & Reschedule Task" else "Add Academic Task",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = textPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        titleTouched = true
                    },
                    label = { Text("Task title *") },
                    placeholder = { Text("e.g. Lab report submission") },
                    isError = titleTouched && title.isBlank(),
                    supportingText = if (titleTouched && title.isBlank()) {
                        { Text("Title is required", color = MaterialTheme.colorScheme.error, fontSize = 11.sp) }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Task Category Selector (Compact)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(TaskCategory.entries) { cat ->
                        val isSelected = category == cat
                        Surface(
                            onClick = { category = cat },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) cat.color else (if (isDark) DarkSurfaceBase else cat.lightContainerColor.copy(alpha = 0.5f)),
                            border = BorderStroke(1.dp, if (isSelected) cat.color else cat.color.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    cat.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else cat.color,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    cat.displayName,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else (if (isDark) Color.White else Color(0xFF1E293B))
                                )
                            }
                        }
                    }
                }

                // Course Code input with picker
                OutlinedTextField(
                    value = course,
                    onValueChange = {
                        course = it
                        courseTouched = true
                    },
                    label = { Text("Course code *") },
                    placeholder = { Text("e.g. PHA3102") },
                    isError = courseTouched && course.isBlank(),
                    supportingText = if (courseTouched && course.isBlank()) {
                        { Text("Course code is required", color = MaterialTheme.colorScheme.error, fontSize = 11.sp) }
                    } else null,
                    trailingIcon = {
                        IconButton(onClick = { showCoursePicker = true }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Pick Course", tint = primaryColor)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Quick course selection chips from student's enrolled courses
                if (availableCourses.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(availableCourses.take(5)) { (cCode, _) ->
                            val isSelected = course.equals(cCode, ignoreCase = true)
                            SuggestionChip(
                                onClick = {
                                    course = cCode
                                    courseTouched = false
                                },
                                label = {
                                    Text(
                                        cCode,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                // Target Due Day Selector: Direct Quick Picks (Today, Tomorrow) + Visual Calendar Button
                Text(
                    text = "Due Day *",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textSecondary,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isDark) DarkSurfaceCard else Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, if (isDark) DarkBorderSubtle else Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
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
                                .background(if (isDark) DarkSurfaceBase else Color.White)
                                .border(1.dp, if (isDark) DarkBorderSubtle else Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = primaryColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = dueDate.ifBlank { "Tomorrow" },
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color.White else Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "Target day (time discarded)",
                                        fontSize = 10.sp,
                                        color = textSecondary
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(primaryColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    Icons.Default.DateRange,
                                    contentDescription = "Open Calendar",
                                    tint = primaryColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Calendar",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryColor
                                )
                            }
                        }

                        // Quick Day Selection: strictly "Today" and "Tomorrow"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val isToday = dueDate.equals("Today", ignoreCase = true)
                            val isTomorrow = dueDate.equals("Tomorrow", ignoreCase = true)

                            FilterChip(
                                selected = isToday,
                                onClick = { dueDate = "Today" },
                                label = {
                                    Text(
                                        "Today",
                                        fontSize = 12.sp,
                                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                leadingIcon = if (isToday) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(13.dp)) }
                                } else null,
                                shape = RoundedCornerShape(9.dp),
                                modifier = Modifier.weight(1f)
                            )

                            FilterChip(
                                selected = isTomorrow,
                                onClick = { dueDate = "Tomorrow" },
                                label = {
                                    Text(
                                        "Tomorrow",
                                        fontSize = 12.sp,
                                        fontWeight = if (isTomorrow) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                leadingIcon = if (isTomorrow) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(13.dp)) }
                                } else null,
                                shape = RoundedCornerShape(9.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Priority & Reminder in compact rows
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Priority:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = textSecondary)
                    listOf("High", "Medium", "Low").forEach { p ->
                        val isSelected = priority == p
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) primaryColor else (if (isDark) DarkSurfaceBase else Color(0xFFF1F5F9))
                                )
                                .clickable { priority = p }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                p,
                                color = if (isSelected) Color.White else textSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                // Alarm Reminder (AlarmManager)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Alert: ", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = textSecondary)
                    val reminderOptions = listOf(
                        0 to "On due date",
                        1440 to "1d before",
                        2880 to "2d before",
                        60 to "1h before",
                        null to "None"
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(reminderOptions) { (mins, label) ->
                            val isSelected = reminderMinutes == mins
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) primaryColor else (if (isDark) DarkSurfaceBase else Color(0xFFF1F5F9))
                                    )
                                    .clickable { reminderMinutes = mins }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    label,
                                    color = if (isSelected) Color.White else textSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                // Notes / Instructions (Compact by default, expands as typed)
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Instructions (Optional)") },
                    placeholder = { Text("e.g. MueLE submission, rubric...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                // Compact Live Schedule Preview
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isDark) Color(0xFF064E3B).copy(alpha = 0.4f) else Color(0xFFF0FDF4),
                            RoundedCornerShape(8.dp)
                        )
                        .border(
                            1.dp,
                            if (isDark) Color(0xFF065F46) else Color(0xFFBBF7D0),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = if (isDark) Color(0xFF34D399) else Color(0xFF16A34A),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        schedulePreview,
                        fontSize = 11.sp,
                        color = if (isDark) Color(0xFFA7F3D0) else Color(0xFF166534),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    titleTouched = true
                    courseTouched = true
                    if (title.isNotBlank() && course.isNotBlank()) {
                        onConfirm(
                            title.trim(),
                            course.trim(),
                            dueDate.ifBlank { "Soon" },
                            priority,
                            reminderMinutes,
                            notes.trim(),
                            category
                        )
                    }
                },
                enabled = title.isNotBlank() && course.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
            ) {
                Text(if (initialTask != null) "Save & Reschedule" else "Schedule & Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    if (showCoursePicker) {
        DedicatedCoursePickerDialog(
            selectedCourse = course,
            availableCourses = availableCourses,
            onCourseSelected = { code, _ ->
                course = code
                courseTouched = false
            },
            onDismiss = { showCoursePicker = false }
        )
    }

    if (showDueCalendarPicker) {
        DedicatedCalendarDatePickerDialog(
            initialDate = dueDate,
            title = "Select Task Due Date",
            onDateSelected = { picked ->
                dueDate = picked
                showDueCalendarPicker = false
            },
            onDismiss = { showDueCalendarPicker = false }
        )
    }
}
