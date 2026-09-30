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
import com.mustime.features.timetable.domain.Assignment
import com.mustime.features.timetable.ui.*
import com.mustime.ui.LocalAppTheme
import com.mustime.ui.components.*
import com.mustime.ui.components.AcademicProfileSheet
import com.mustime.ui.components.NotificationCenterSheet

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

    val filteredTasks = remember(assignments, uiState.searchQuery, uiState.selectedFilter) {
        assignments.filter { task ->
            val matchesFilter = when (uiState.selectedFilter) {
                "Pending" -> !task.completed
                "Completed" -> task.completed
                else -> true
            }
            val matchesSearch = task.title.contains(uiState.searchQuery, ignoreCase = true) ||
                    task.courseCode.contains(uiState.searchQuery, ignoreCase = true)
            matchesFilter && matchesSearch
        }
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
                    .padding(bottom = 76.dp)
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
                        val filters = listOf("All", "Pending", "Completed")
                        items(filters) { filter ->
                            val isSelected = uiState.selectedFilter == filter
                            val count = when (filter) {
                                "Pending" -> assignments.count { !it.completed }
                                "Completed" -> assignments.count { it.completed }
                                else -> assignments.size
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) primaryColor else cardBg)
                                    .border(1.dp, if (isSelected) primaryColor else borderColor, RoundedCornerShape(16.dp))
                                    .clickable { viewModel.setFilter(filter) }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$filter ($count)",
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else textSecondary
                                )
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
                            onDelete = { viewModel.deleteAssignment(task.id) },
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
            onConfirm = { title, course, dueDate, priority, reminderMinutes ->
                val currentTask = taskToEdit
                if (currentTask != null) {
                    viewModel.updateAssignment(
                        currentTask.copy(
                            title = title,
                            courseCode = course,
                            dueDate = dueDate,
                            priority = priority,
                            reminderMinutes = reminderMinutes
                        )
                    )
                } else {
                    viewModel.saveAssignment(
                        title = title,
                        courseCode = course,
                        dueDate = dueDate,
                        priority = priority,
                        reminderMinutes = reminderMinutes
                    )
                }
                showAddDialog = false
                taskToEdit = null
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

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (isDark) DarkBorderSubtle else Color(0xFFE2E8F0)
            )
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .border(2.dp, if (task.completed) primaryColor else (if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1)), CircleShape)
                            .background(if (task.completed) primaryColor else Color.Transparent)
                            .clickable(onClick = onToggleComplete),
                        contentAlignment = Alignment.Center
                    ) {
                        if (task.completed) {
                            Icon(Icons.Default.Check, contentDescription = "Completed", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = task.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (task.completed) textSecondary else textPrimary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Edit / Reschedule button
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit or Reschedule Task",
                            tint = primaryColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Test reminder trigger button
                    IconButton(
                        onClick = onTestAlarm,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.NotificationsActive,
                            contentDescription = "Test Alarm (3s)",
                            tint = primaryColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Delete",
                            tint = textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Badges row
            val (badgeBg, badgeText) = when (task.priority.lowercase()) {
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 38.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(badgeBg, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        task.priority.replaceFirstChar { it.uppercase() },
                        color = badgeText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))
                Text(task.courseCode, fontSize = 13.sp, color = textSecondary, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.width(10.dp))
                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706), modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(task.dueDate, fontSize = 13.sp, color = textSecondary)

                if (task.reminderMinutes != null) {
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                if (task.completed) {
                                    if (isDark) DarkSurfaceBase else Color(0xFFF1F5F9)
                                } else {
                                    if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.4f) else Color(0xFFEFF6FF)
                                },
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = null,
                                tint = if (task.completed) textSecondary else primaryColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                if (task.completed) "Off" else "${task.reminderMinutes}m",
                                fontSize = 11.sp,
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
    onConfirm: (title: String, course: String, dueDate: String, priority: String, reminder: Int?) -> Unit
) {
    var title by remember(initialTask) { mutableStateOf(initialTask?.title ?: "") }
    var course by remember(initialTask) { mutableStateOf(initialTask?.courseCode ?: "") }
    var dueDate by remember(initialTask) { mutableStateOf(initialTask?.dueDate ?: "Tomorrow • 17:00") }
    var priority by remember(initialTask) { mutableStateOf(initialTask?.priority ?: "Medium") }
    var reminderMinutes by remember(initialTask) { mutableStateOf<Int?>(initialTask?.reminderMinutes ?: 30) }

    var showCoursePicker by remember { mutableStateOf(false) }
    var showDueSchedulePicker by remember { mutableStateOf(false) }
    var showDueDayPicker by remember { mutableStateOf(false) }
    var showDueTimePicker by remember { mutableStateOf(false) }
    var tempDueDay by remember { mutableStateOf("Tomorrow") }

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
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task title *") },
                    placeholder = { Text("e.g. Lab report submission") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = course,
                    onValueChange = { course = it },
                    label = { Text("Course code *") },
                    placeholder = { Text("e.g. PHA3102") },
                    trailingIcon = {
                        IconButton(onClick = { showCoursePicker = true }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Pick Course", tint = primaryColor)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Due Date & Time Scheduler Card (Google Clock inspired)
                Text(
                    text = "Due Date & Time *",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    onClick = { showDueSchedulePicker = true },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) DarkSurfaceCard else Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, if (isDark) DarkBorderSubtle else Color(0xFFCBD5E1)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
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
                                    text = dueDate.ifBlank { "Tomorrow • 17:00" },
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else Color(0xFF0F172A)
                                )
                                Text(
                                    text = "Managed by Time Scheduler",
                                    fontSize = 11.sp,
                                    color = textSecondary
                                )
                            }
                        }
                        Icon(
                            Icons.Default.AccessTime,
                            contentDescription = "Change Schedule",
                            tint = primaryColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Quick Due Date Presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Today • 17:00", "Tomorrow • 12:00", "Friday • 17:00").forEach { preset ->
                        val isSelected = dueDate == preset
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) {
                                        if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.5f) else Color(0xFFEFF6FF)
                                    } else {
                                        if (isDark) DarkSurfaceBase else Color(0xFFF1F5F9)
                                    }
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) primaryColor else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { dueDate = preset }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                preset,
                                fontSize = 11.sp,
                                color = if (isSelected) primaryColor else textSecondary
                            )
                        }
                    }
                }

                Text("Priority", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = textSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("High", "Medium", "Low").forEach { p ->
                        val isSelected = priority == p
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) primaryColor else (if (isDark) DarkSurfaceBase else Color(0xFFF1F5F9))
                                )
                                .clickable { priority = p }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                p,
                                color = if (isSelected) Color.White else textSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // AlarmManager Reminder Setting
                Text("Alarm Reminder (AlarmManager)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
                val reminderOptions = listOf(
                    15 to "15m before",
                    30 to "30m before",
                    60 to "1h before",
                    120 to "2h before",
                    0 to "At due time",
                    null to "None"
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(reminderOptions) { (mins, label) ->
                        val isSelected = reminderMinutes == mins
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) primaryColor else (if (isDark) DarkSurfaceBase else Color(0xFFF1F5F9))
                                )
                                .clickable { reminderMinutes = mins }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
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

                // Live Schedule Preview Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isDark) Color(0xFF064E3B).copy(alpha = 0.4f) else Color(0xFFF0FDF4),
                            RoundedCornerShape(10.dp)
                        )
                        .border(
                            1.dp,
                            if (isDark) Color(0xFF065F46) else Color(0xFFBBF7D0),
                            RoundedCornerShape(10.dp)
                        )
                        .padding(10.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFF34D399) else Color(0xFF16A34A),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Scheduled Alarm Trigger",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF6EE7B7) else Color(0xFF15803D)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            schedulePreview,
                            fontSize = 11.sp,
                            color = if (isDark) Color(0xFFA7F3D0) else Color(0xFF166534)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && course.isNotBlank()) {
                        onConfirm(title.trim(), course.trim(), dueDate.ifBlank { "Soon" }, priority, reminderMinutes)
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
            onCourseSelected = { code, _ -> course = code },
            onDismiss = { showCoursePicker = false }
        )
    }

    if (showDueSchedulePicker) {
        DedicatedSchedulePickerDialog(
            initialSchedule = dueDate,
            title = "Set Task Schedule",
            onScheduleSelected = { picked ->
                dueDate = picked
                showDueSchedulePicker = false
            },
            onDismiss = { showDueSchedulePicker = false }
        )
    }

    if (showDueDayPicker) {
        DedicatedDayPickerDialog(
            selectedDay = tempDueDay,
            onDaySelected = { day ->
                tempDueDay = day
                showDueDayPicker = false
                showDueTimePicker = true
            },
            onDismiss = { showDueDayPicker = false }
        )
    }

    if (showDueTimePicker) {
        DedicatedTimePickerDialog(
            initialTime = "17:00",
            title = "Set Due Time ($tempDueDay)",
            onTimeSelected = { time ->
                dueDate = "$tempDueDay • $time"
                showDueTimePicker = false
            },
            onDismiss = { showDueTimePicker = false }
        )
    }
}
