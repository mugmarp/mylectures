package com.mustime.features.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.mustime.ui.components.FloatingIslandTopBar
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mustime.TimetableApplication
import com.mustime.core.alarm.AlarmScheduler
import com.mustime.core.alarm.ReminderScheduler
import com.mustime.core.alarm.TaskAlarmScheduler
import com.mustime.features.onboarding.FacultyCatalog
import com.mustime.ui.AccentColorNames
import com.mustime.ui.AccentColors
import com.mustime.ui.LocalAppTheme
import com.mustime.ui.ThemeMode
import kotlinx.coroutines.Dispatchers

@Composable
fun SettingsScreen(
    currentProgramme: String = "",
    onReconfigureAcademicProfile: () -> Unit = {},
    onResetOnboarding: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val app = context.applicationContext as? TimetableApplication
    val repository = app?.repository
    val dataLoader = app?.dataLoader
    val alarmScheduler = remember(context) { TaskAlarmScheduler(context) }
    val classAlarmScheduler = remember(context) { AlarmScheduler(context) }
    val reminderScheduler = remember(context) { ReminderScheduler(context) }

    if (repository == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    val viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.provideFactory(
            repository = repository,
            dataLoader = dataLoader,
            alarmScheduler = alarmScheduler,
            classAlarmScheduler = classAlarmScheduler,
            reminderScheduler = reminderScheduler,
            ioDispatcher = Dispatchers.IO,
            application = context.applicationContext as android.app.Application
        )
    )

    val uiState by viewModel.uiState.collectAsState()
    val appTheme = LocalAppTheme.current
    val isDark = appTheme.isDark

    val onSelectThemeMode: (ThemeMode) -> Unit = { mode ->
        viewModel.setThemeMode(mode)
        appTheme.onThemeModeChange(mode)
    }

    var showResetNotesDialog by remember { mutableStateOf(false) }
    var showResetTasksDialog by remember { mutableStateOf(false) }
    var showDisclaimerDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showDeveloperModal by remember { mutableStateOf(false) }
    var showFeedbackModal by remember { mutableStateOf(false) }
    var showCustomLeadDialog by remember { mutableStateOf(false) }
    var customLeadInput by remember { mutableStateOf("") }
    var showVersionDetailsDialog by remember { mutableStateOf(false) }

    // Custom Alarm Lead Time Dialog
    if (showCustomLeadDialog) {
        AlertDialog(
            onDismissRequest = { showCustomLeadDialog = false },
            icon = { Icon(Icons.Default.Alarm, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Custom Lecture Reminder", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Enter how many minutes before class start you want to be reminded (e.g. 20, 30, 40):",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = customLeadInput,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() } && input.length <= 3) {
                                customLeadInput = input
                            }
                        },
                        placeholder = { Text("30") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val mins = customLeadInput.toIntOrNull()
                        if (mins != null && mins > 0) {
                            viewModel.setClassAlarmLeadMinutes(mins)
                        }
                        showCustomLeadDialog = false
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomLeadDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Disclaimer Dialog
    if (showDisclaimerDialog) {
        AlertDialog(
            onDismissRequest = { showDisclaimerDialog = false },
            icon = { Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Institutional Non-Affiliation Disclaimer", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "1. Independent Development",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Lectures is an independent academic timetable and study companion designed to help students organize classes, study notes, and coursework tasks.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "2. No University Affiliation or Endorsement",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Lectures is NOT an official application of, nor is it endorsed by, sponsored by, affiliated with, or in any way officially associated with Mbarara University of Science and Technology (MUST) or any of its constituent faculties, departments, or administrative offices.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "3. Informational & Convenience Purpose",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "All course codes, faculty designations, module names, and timetable slots are referenced solely for descriptive and schedule organization purposes for students. Students are advised to consult official university online portals (timetable.must.ac.ug) and administrative systems for official announcements, venue changes, and examination schedules.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "4. Academic Schedule Accuracy",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Timetable data is maintained offline and locally on your device. Students should verify critical announcements and examinations with their respective departments.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showDisclaimerDialog = false },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("I Understand")
                }
            }
        )
    }

    // Terms & Regulations Dialog
    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = { showTermsDialog = false },
            icon = { Icon(Icons.Default.Gavel, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Disclaimer & Terms", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "1. Permitted Use",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Lectures is provided for personal, non-commercial academic management and learning assistance. Users agree to use the application lawfully and ethically.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "2. 'As Is' Disclaimer of Warranty",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "The software is provided on an 'AS IS' basis without warranties of any kind. While every effort is made to maintain schedule accuracy, the developer is not responsible for missed classes, test clashes, device alarm failures, or timetable discrepancies.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "3. Developer Details",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Developed by MUGENDAWALA MARK PAUL as an independent student tool for academic schedule tracking, vacant study hall finding, and offline coursework productivity.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "4. User Data & Notes",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "All notes, summaries, checklists, and task entries are authored by the student and stored privately on the local device.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "5. Data Privacy & Local-First Storage",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "All student coursework, personal lecture notes, revision tasks, and schedule preferences are stored locally on your device in secure SQLite database tables.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showTermsDialog = false },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("I accept the Terms of Service & Disclaimer.")
                }
            }
        )
    }

    // Privacy Policy Dialog
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            icon = { Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Privacy Policy", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "1. Local-First Student Data Storage",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Lectures is designed local-first. Your timetable schedules, lecture notes, tasks, alarms, and personal checklists are stored securely on your device in SQLite database tables.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "2. Pseudonymous Service Diagnostics",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "To ensure timetable schedule reliability, monitor cohort coverage, and diagnose device compatibility across MUST campuses, the app transmits pseudonymous technical telemetry (an anonymous installation UUID, device model, Android OS version, launch count, and enrolled academic programme) to our secure database. We do not collect, monetize, or sell names, email addresses, phone numbers, or lecture notes.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "3. Permissions Usage",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Notification and exact alarm permissions are requested solely to ring local alerts before scheduled lectures and task deadlines.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showPrivacyDialog = false },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Done")
                }
            }
        )
    }

    val activeProgramme = if (uiState.currentProgramme.isNotBlank()) uiState.currentProgramme else currentProgramme
    val progCode = activeProgramme.substringBefore(" ").trim()
    val faculty = remember(progCode) {
        FacultyCatalog.faculties.find { f -> f.programmes.any { it.code.equals(progCode, ignoreCase = true) } }
    }
    val programme = remember(progCode, faculty) {
        faculty?.programmes?.find { it.code.equals(progCode, ignoreCase = true) }
    }
    val programmeDisplayName = programme?.name ?: "Academic Programme ($activeProgramme)"
    val facultyDisplayName = faculty?.name ?: "Mbarara University of Science and Technology"

    // Reset Notes Confirmation Dialog
    if (showResetNotesDialog) {
        AlertDialog(
            onDismissRequest = { showResetNotesDialog = false },
            icon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Color(0xFFDC2626)) },
            title = { Text("Reset All Notes?", fontWeight = FontWeight.Bold) },
            text = { Text("This will permanently delete all your lecture notes and attached class alarm intervals. This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetAllNotes {
                            showResetNotesDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Delete Notes", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetNotesDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Reset Tasks Confirmation Dialog
    if (showResetTasksDialog) {
        AlertDialog(
            onDismissRequest = { showResetTasksDialog = false },
            icon = { Icon(Icons.Default.AlarmOff, contentDescription = null, tint = Color(0xFFDC2626)) },
            title = { Text("Reset Academic Tasks?", fontWeight = FontWeight.Bold) },
            text = { Text("This will remove all saved assignments and cancel any scheduled reminder notifications.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetAllTasks {
                            showResetTasksDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Clear Tasks & Alarms", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetTasksDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }



    // Developer Profile Bottom Sheet
    if (showDeveloperModal) {
        com.mustime.features.developer.DeveloperProfileSheet(
            onDismiss = { showDeveloperModal = false }
        )
    }

    // Anonymous Feedback Bottom Sheet
    if (showFeedbackModal) {
        val feedbackMgr = app?.feedbackManager ?: remember(context) {
            val store = app?.etagStore ?: com.mustime.features.timetable.data.ETagStore(context)
            com.mustime.features.feedback.FeedbackManager(context, store)
        }
        com.mustime.features.feedback.AnonymousFeedbackSheet(
            feedbackManager = feedbackMgr,
            onDismiss = { showFeedbackModal = false }
        )
    }

    if (showVersionDetailsDialog) {
        val meta = uiState.metadata
        val isFinal = uiState.isFinal || uiState.timetableVersion.contains("Final", ignoreCase = true)
        val stageLabel = if (isFinal) "Official Final Release" else "Active Draft ${meta?.draftNumber ?: ""}".trim()
        val badgeBg = if (isFinal) Color(0xFFDCFCE7) else Color(0xFFFEF3C7)
        val badgeColor = if (isFinal) Color(0xFF15803D) else Color(0xFFB45309)

        AlertDialog(
            onDismissRequest = { showVersionDetailsDialog = false },
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(badgeBg, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Verified,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(26.dp)
                    )
                }
            },
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        uiState.timetableVersion,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .background(badgeBg, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = stageLabel.uppercase(),
                            color = badgeColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = meta?.releaseNotes ?: if (isFinal) {
                            "Authoritative teaching schedule approved and ratified by MUST Academic Registrar."
                        } else {
                            "Draft schedule subject to departmental lecture and lab reviews."
                        },
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Academic Session", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${meta?.academicYear ?: "2026/2027"} · ${meta?.semester ?: "Semester 1"}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Issuing Authority", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(meta?.source ?: "MUST Academic Registrar", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Scheduled Sessions", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${meta?.totalSessions ?: 1304} Sessions · 90 Rooms", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Draft Lifecycle", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(if (isFinal) "Final Ratified" else "Dynamic Review", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showVersionDetailsDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Close")
                }
            }
        )
    }

    val scrollState = rememberScrollState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (onBack != null) {
                FloatingIslandTopBar {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Settings",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // User Feedback: Error Message Banner
            AnimatedVisibility(
                visible = uiState.errorMessage != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                uiState.errorMessage?.let { error ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                        border = BorderStroke(1.dp, Color(0xFFFECACA))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = Color(0xFFDC2626))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(error, color = Color(0xFF991B1B), fontSize = 13.sp, modifier = Modifier.weight(1f))
                            IconButton(onClick = { viewModel.clearFeedbackMessages() }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // User Feedback: Success Message Banner
            AnimatedVisibility(
                visible = uiState.successMessage != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                uiState.successMessage?.let { success ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                        border = BorderStroke(1.dp, Color(0xFFBBF7D0))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(success, color = Color(0xFF166534), fontSize = 13.sp, modifier = Modifier.weight(1f))
                            IconButton(onClick = { viewModel.clearFeedbackMessages() }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color(0xFF16A34A), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ACADEMIC PROFILE
            SectionHeader("ACADEMIC PROFILE")
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(onClick = onReconfigureAcademicProfile),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier.padding(18.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(programmeDisplayName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 2)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("$activeProgramme • $facultyDisplayName", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Change profile", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // NOTIFICATIONS & ALARMS
            SectionHeader("NOTIFICATIONS & ALARMS")

            val areNotificationsAllowed = remember(context) {
                NotificationManagerCompat.from(context).areNotificationsEnabled()
            }
            var isPermissionBannerDismissed by rememberSaveable { mutableStateOf(false) }

            // Dismissible banner when notifications are disabled in system settings
            if (!areNotificationsAllowed && !isPermissionBannerDismissed) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF78350F).copy(alpha = 0.35f) else Color(0xFFFEF3C7)
                    ),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFFB45309).copy(alpha = 0.5f) else Color(0xFFFDE68A))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(if (isDark) Color(0xFFB45309).copy(alpha = 0.3f) else Color(0xFFFDE68A), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.NotificationsActive,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Notifications Paused",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isDark) Color(0xFFFDE68A) else Color(0xFF92400E)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Lectures notifications are currently paused — enable permissions so you receive class alarms and coursework reminders.",
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                color = if (isDark) Color(0xFFFDE68A).copy(alpha = 0.9f) else Color(0xFF78350F)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    }
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = android.net.Uri.fromParts("package", context.packageName, null)
                                        }
                                        try {
                                            context.startActivity(fallback)
                                        } catch (_: Exception) {}
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isDark) Color(0xFFD97706) else Color(0xFFB45309),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.defaultMinSize(minHeight = 32.dp)
                            ) {
                                Text("Enable in System Settings", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { isPermissionBannerDismissed = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = if (isDark) Color(0xFFFDE68A) else Color(0xFF92400E),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }


            // Card 2: Lecture & Class Alarms Configuration
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFF2563EB).copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Alarm, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Lecture & Class Reminders",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Choose lead time before each lecture or lab starts",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val classLeadOptions = listOf(
                        10 to "10m",
                        15 to "15m",
                        30 to "30m",
                        45 to "45m",
                        60 to "60m"
                    )
                    val isCustomActive = uiState.classAlarmLeadMinutes !in listOf(10, 15, 30, 45, 60)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        classLeadOptions.forEach { (mins, label) ->
                            val isSelected = uiState.classAlarmLeadMinutes == mins
                            val chipBg = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            val chipText = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(chipBg)
                                    .clickable { viewModel.setClassAlarmLeadMinutes(mins) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = chipText
                                )
                            }
                        }

                        // Custom Chip
                        val customChipBg = if (isCustomActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        val customChipText = if (isCustomActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        Box(
                            modifier = Modifier
                                .weight(1.3f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(customChipBg)
                                .clickable {
                                    customLeadInput = uiState.classAlarmLeadMinutes.toString()
                                    showCustomLeadDialog = true
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isCustomActive) "${uiState.classAlarmLeadMinutes}m" else "Custom",
                                fontSize = 11.sp,
                                fontWeight = if (isCustomActive) FontWeight.Bold else FontWeight.Medium,
                                color = customChipText
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "• Default is 30 minutes before lecture start. Reminders include lecture hall venue, course code, and lecturer.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Card 4: Audio & Haptic Feedback
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            putExtra(Settings.EXTRA_CHANNEL_ID, com.mustime.core.notification.NotificationHelper.CHANNEL_CLASS_ALARMS)
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            val fallback = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            }
                            try {
                                context.startActivity(fallback)
                            } catch (_: Exception) {}
                        }
                    },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Notification Sounds, Ringtones & Vibration",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "Configure tone, vibration, pop-on-screen & alert priority in Android System Settings",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Open System Sound Settings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // APPEARANCE & THEME
            SectionHeader("APPEARANCE & THEME")
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Header text with active mode pill
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Theme Mode",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Customize look & feel across all screens",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Box(
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    RoundedCornerShape(20.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (appTheme.isDark) "Dark Active" else "Light Active",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 3 Interactive Theme Mode Cards
                    val activeMode = uiState.themeMode
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ThemeSelectionCard(
                            title = "Light",
                            subtitle = "Bright & crisp",
                            icon = Icons.Default.LightMode,
                            isSelected = activeMode == ThemeMode.LIGHT,
                            modifier = Modifier.weight(1f),
                            previewBgColor = Color(0xFFF8FAFC),
                            previewCardColor = Color.White,
                            previewTextColor = Color(0xFF0F172A),
                            onClick = { onSelectThemeMode(ThemeMode.LIGHT) }
                        )

                        ThemeSelectionCard(
                            title = "Dark",
                            subtitle = "Deep comfort",
                            icon = Icons.Default.DarkMode,
                            isSelected = activeMode == ThemeMode.DARK,
                            modifier = Modifier.weight(1f),
                            previewBgColor = Color(0xFF101216),
                            previewCardColor = Color(0xFF181B22),
                            previewTextColor = Color(0xFFF8FAFC),
                            onClick = { onSelectThemeMode(ThemeMode.DARK) }
                        )

                        ThemeSelectionCard(
                            title = "System",
                            subtitle = "Auto sync",
                            icon = Icons.Default.BrightnessAuto,
                            isSelected = activeMode == ThemeMode.SYSTEM,
                            modifier = Modifier.weight(1f),
                            previewBgColor = if (appTheme.isDark) Color(0xFF101216) else Color(0xFFF8FAFC),
                            previewCardColor = if (appTheme.isDark) Color(0xFF181B22) else Color.White,
                            previewTextColor = if (appTheme.isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A),
                            onClick = { onSelectThemeMode(ThemeMode.SYSTEM) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // DATA & SYNC
            SectionHeader("DATA & SYNCHRONIZATION")
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (uiState.isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.5.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Icon(Icons.Default.Sync, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Synchronize Timetable", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text(uiState.lastSyncTime, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(
                            onClick = { viewModel.syncTimetable() },
                            enabled = !uiState.isSyncing,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                if (uiState.isSyncing) "Syncing..." else "Sync Now",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    val isFinalRelease = uiState.isFinal || uiState.timetableVersion.contains("Final", ignoreCase = true)
                    val badgeText = uiState.metadata?.displayBadge ?: if (isFinalRelease) "FINAL" else uiState.timetableStatus.uppercase()
                    val badgeBg = if (isFinalRelease) Color(0xFFDCFCE7) else Color(0xFFFEF3C7)
                    val badgeColor = if (isFinalRelease) Color(0xFF15803D) else Color(0xFFB45309)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showVersionDetailsDialog = true }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(badgeBg, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Verified, contentDescription = null, tint = badgeColor, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Timetable Version & Mode", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text("${uiState.timetableVersion} · ${uiState.metadata?.semester ?: "Semester 1"}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Box(
                            modifier = Modifier
                                .background(badgeBg, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(badgeText, color = badgeColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // DANGER ZONE
            SectionHeader("DANGER ZONE", color = Color(0xFFDC2626))
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, Color(0xFFFECACA)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Reset Notes Action
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showResetNotesDialog = true }
                            .padding(vertical = 8.dp, horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Reset All Lecture Notes", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFFDC2626))
                            Text("Permanently delete saved notes and annotations", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    // Reset Tasks Action
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showResetTasksDialog = true }
                            .padding(vertical = 8.dp, horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AlarmOff, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Reset Academic Tasks & Alarms", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFFDC2626))
                            Text("Cancel all scheduled alarms and clear coursework", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ABOUT, DEVELOPER & REGULATIONS SECTION
            SectionHeader("DEVELOPER, REGULATIONS & DISCLAIMER", MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // App & Developer Branding (Clickable to open full developer profile)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showDeveloperModal = true }
                            .padding(vertical = 4.dp, horizontal = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.School, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Lectures", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text("Developer", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                            Text("MUGENDAWALA MARK PAUL - Student at MUST", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "View Developer Profile", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    // Anonymous Feedback & Bug Reports Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showFeedbackModal = true }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Feedback, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Anonymous Feedback & Bugs", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF10B981).copy(alpha = 0.15f))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text("Stateless", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                                }
                            }
                            Text("Report timetable errors, venue bugs, or suggestions anonymously", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    // 1. Non-Affiliation Disclaimer Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showDisclaimerDialog = true }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Non-Affiliation Disclaimer", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Independent app — not affiliated with Mbarara University (MUST)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    // 2. Disclaimer & Terms of Service Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showTermsDialog = true }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Gavel, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Disclaimer & Terms", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Permitted use, warranty disclaimers & academic policies", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    // 3. Privacy Policy Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showPrivacyDialog = true }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Privacy Policy", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Local-first SQLite storage & privacy policy", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String, color: Color = MaterialTheme.colorScheme.primary) {
    Text(
        text = title,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
    )
}

@Composable
private fun ThemeSelectionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    previewBgColor: Color,
    previewCardColor: Color,
    previewTextColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    }
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    }

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 2.dp else 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Miniature UI canvas preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(previewBgColor)
                    .border(1.dp, previewTextColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                    .padding(6.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Mini header bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .height(5.dp)
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(3.dp))
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(previewTextColor.copy(alpha = 0.3f), CircleShape)
                        )
                    }
                    // Mini card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(18.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(previewCardColor)
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(32.dp)
                                .height(4.dp)
                                .background(previewTextColor.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = title,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
