package com.mustime.features.notes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mustime.TimetableApplication
import com.mustime.features.timetable.domain.LectureNote
import com.mustime.features.timetable.ui.*
import com.mustime.ui.LocalAppTheme
import com.mustime.ui.components.AcademicProfileSheet
import com.mustime.ui.components.NotificationCenterSheet
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotesScreen(
    onBack: (() -> Unit)? = null,
    onSettingsClick: () -> Unit = {},
    onReconfigureAcademicProfile: () -> Unit = {},
    onEditingChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val app = context.applicationContext as? TimetableApplication
    val repository = app?.repository

    if (repository == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF2563EB))
        }
        return
    }

    val isDark = LocalAppTheme.current.isDark
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val viewModel: NotesViewModel = viewModel(
        factory = NotesViewModel.provideFactory(repository)
    )

    val uiState by viewModel.uiState.collectAsState()

    // State for Note Editor flow
    var isEditing by remember { mutableStateOf(false) }
    var noteToEdit by remember { mutableStateOf<LectureNote?>(null) }
    var noteToDelete by remember { mutableStateOf<LectureNote?>(null) }
    var showNotificationSheet by remember { mutableStateOf(false) }
    var showProfileSheet by remember { mutableStateOf(false) }

    LaunchedEffect(isEditing) {
        onEditingChanged(isEditing)
    }

    if (isEditing) {
        NoteEditorScreen(
            initialNote = noteToEdit,
            availableCourses = uiState.availableCourses,
            timetableClasses = uiState.timetableClasses,
            onSettingsClick = onSettingsClick,
            onReconfigureAcademicProfile = onReconfigureAcademicProfile,
            onSave = { key, course, title, content, alarm, tag, attachedClass, attachment ->
                viewModel.saveNote(
                    naturalKey = key,
                    courseCode = course,
                    title = title,
                    content = content,
                    alarmMinutes = alarm,
                    tag = tag,
                    attachedClass = attachedClass,
                    attachmentName = attachment
                )
                isEditing = false
                noteToEdit = null
            },
            onCancel = {
                isEditing = false
                noteToEdit = null
            }
        )
        return
    }

    // Main Academic Notes Dashboard Screen
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    noteToEdit = null
                    isEditing = true
                },
                containerColor = Color(0xFF0052CC),
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 96.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Note", modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Note", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isDark) DarkSurfaceBase else Color(0xFFF8FAFC))
                .padding(innerPadding)
                .statusBarsPadding()
        ) {
            // UNIFIED TOP APP BAR: Notes with active badge, programme tag, Filter, and Profile
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2563EB)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.School,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Academic Notes",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = if (isDark) Color.White else Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isDark) Color(0xFF1E3A8A) else Color(0xFFDBEAFE))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "${uiState.filteredNotes.size} active",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8)
                                )
                            }
                        }
                        if (uiState.userProgramme.isNotBlank()) {
                            Text(
                                uiState.userProgramme,
                                fontSize = 12.sp,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { showNotificationSheet = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Notifications,
                            contentDescription = "Notifications",
                            tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0052CC))
                            .clickable { showProfileSheet = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "Account",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // SEARCH BAR
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDark) DarkSurfaceCard else Color.White),
                border = BorderStroke(1.dp, if (isDark) DarkBorderSubtle else Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    TextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                        placeholder = {
                            Text(
                                "Search notes, tags, or courses...",
                                fontSize = 14.sp,
                                color = Color(0xFF94A3B8)
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                            unfocusedTextColor = if (isDark) Color.White else Color(0xFF0F172A)
                        ),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    if (uiState.searchQuery.isNotBlank()) {
                        IconButton(
                            onClick = {
                                viewModel.onSearchQueryChanged("")
                                focusManager.clearFocus()
                                keyboardController?.hide()
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = "Voice search",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // COURSE FILTER PILLS
            val filterOptions = remember(uiState.availableCourses) {
                listOf("All Notes") + uiState.availableCourses.map { it.code }.distinct()
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filterOptions.forEach { filterItem ->
                    val isSelected = uiState.selectedFilter.equals(filterItem, ignoreCase = true)
                    val unselectedPillBg = if (isDark) DarkSurfaceCard else Color(0xFFEFF6FF)
                    val unselectedPillText = if (isDark) Color(0xFFCBD5E1) else Color(0xFF1E293B)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) Color(0xFF0052CC) else unselectedPillBg)
                            .clickable { viewModel.onFilterSelected(filterItem) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = filterItem,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else unselectedPillText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // USER FEEDBACK MESSAGES
            AnimatedVisibility(visible = uiState.userMessage != null) {
                uiState.userMessage?.let { msg ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(msg, color = Color(0xFF15803D), fontSize = 13.sp, modifier = Modifier.weight(1f))
                            IconButton(onClick = { viewModel.clearFeedback() }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color(0xFF15803D), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // GROUPED NOTES LIST
            val grouped = remember(uiState.filteredNotes) {
                uiState.filteredNotes.groupBy { note ->
                    note.naturalKey.split("|").firstOrNull()?.ifBlank { "General" } ?: "General"
                }
            }

            if (uiState.filteredNotes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (uiState.searchQuery.isNotBlank() || uiState.selectedFilter != "All Notes") {
                                    Icons.Outlined.SearchOff
                                } else {
                                    Icons.Outlined.Notes
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = when {
                                uiState.searchQuery.isNotBlank() -> "No notes matching \"${uiState.searchQuery}\""
                                uiState.selectedFilter != "All Notes" -> "No notes for ${uiState.selectedFilter}"
                                else -> "No lecture notes yet"
                            },
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = when {
                                uiState.searchQuery.isNotBlank() || uiState.selectedFilter != "All Notes" ->
                                    "Try adjusting your query or resetting your course filter to find what you're looking for."
                                else ->
                                    "Tap '+ New Note' to write rich lecture summaries, link classes from your timetable, and set reminder alarms."
                            },
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 20.sp
                        )

                        if (uiState.searchQuery.isNotBlank() || uiState.selectedFilter != "All Notes") {
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedButton(
                                onClick = {
                                    viewModel.onSearchQueryChanged("")
                                    viewModel.onFilterSelected("All Notes")
                                },
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text("Clear Search & Filters", fontSize = 13.sp)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 150.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    grouped.forEach { (courseCode, notes) ->
                        val courseInfo = uiState.availableCourses.find { it.code.equals(courseCode, ignoreCase = true) }
                        val courseTitle = courseInfo?.title ?: "Academic Course"
                        val dotColor = courseInfo?.color?.let {
                            try { Color(android.graphics.Color.parseColor(it)) } catch (e: Exception) { Color(0xFF2563EB) }
                        } ?: Color(0xFF2563EB)

                        item(key = "header_$courseCode") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, bottom = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(dotColor)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "$courseCode · $courseTitle",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color.White else Color(0xFF0F172A)
                                    )
                                }
                                Text(
                                    text = "${notes.size} note${if (notes.size > 1) "s" else ""}",
                                    fontSize = 12.sp,
                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                )
                            }
                        }

                        items(notes, key = { it.naturalKey }) { note ->
                            NoteCardItem(
                                note = note,
                                courseCode = courseCode,
                                isDark = isDark,
                                onOpen = {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                    noteToEdit = note
                                    isEditing = true
                                },
                                onTogglePin = { viewModel.togglePin(note.naturalKey) },
                                onDelete = { noteToDelete = note }
                            )
                        }
                    }
                }
            }
        }
    }

    if (noteToDelete != null) {
        val target = noteToDelete!!
        AlertDialog(
            onDismissRequest = { noteToDelete = null },
            icon = {
                Icon(
                    Icons.Outlined.DeleteOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text("Delete this note?", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Are you sure you want to delete \"${target.title ?: "Untitled Note"}\"? This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteNote(target.naturalKey)
                        noteToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { noteToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showNotificationSheet) {
        NotificationCenterSheet(onDismiss = { showNotificationSheet = false })
    }

    if (showProfileSheet) {
        AcademicProfileSheet(
            programme = uiState.userProgramme,
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
fun NoteCardItem(
    note: LectureNote,
    courseCode: String,
    isDark: Boolean = false,
    onOpen: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    val categoryTag = note.tag ?: "Lecture"

    val scheduleTag = when {
        note.naturalKey.contains("Friday") -> "📅 Friday"
        note.naturalKey.contains("Wednesday") -> "🕒 Wednesday"
        note.naturalKey.contains("Monday") -> "📅 Monday"
        note.naturalKey.contains("Tuesday") -> "📅 Tuesday"
        note.naturalKey.contains("Thursday") -> "📅 Thursday"
        else -> "📅 Scheduled Class"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isDark) DarkSurfaceCard else Color.White),
        border = BorderStroke(1.dp, if (isDark) DarkBorderSubtle else Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Tag Row with Pins and Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Category Badge
                    val tagBg = when (categoryTag) {
                        "Lab Prep" -> if (isDark) Color(0xFF312E81) else Color(0xFFEEF2FF)
                        "Study Guide" -> if (isDark) Color(0xFF1E3A8A) else Color(0xFFEFF6FF)
                        "Assignment" -> if (isDark) Color(0xFF581C87) else Color(0xFFF3E8FF)
                        else -> if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                    }
                    val tagText = when (categoryTag) {
                        "Lab Prep" -> if (isDark) Color(0xFFA5B4FC) else Color(0xFF4338CA)
                        "Study Guide" -> if (isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8)
                        "Assignment" -> if (isDark) Color(0xFFD8B4FE) else Color(0xFF7E22CE)
                        else -> if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(tagBg)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(categoryTag, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = tagText)
                    }

                    // Schedule Tag
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            scheduleTag,
                            fontSize = 11.sp,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onTogglePin,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            if (note.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                            contentDescription = "Pin Note",
                            tint = if (note.isPinned) Color(0xFF2563EB) else (if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Options",
                                tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF94A3B8),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit Note") },
                                onClick = {
                                    showMenu = false
                                    onOpen()
                                },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete Note", color = Color(0xFFDC2626)) },
                                onClick = {
                                    showMenu = false
                                    onDelete()
                                },
                                leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Color(0xFFDC2626)) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Note Title
            Text(
                text = note.title ?: "Academic Note: $courseCode",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.White else Color(0xFF0F172A),
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Note Content Preview (2 lines, stripped of raw markdown symbols)
            val cleanPreview = remember(note.content) {
                MarkdownUtils.stripMarkdown(note.content).ifBlank { "Tap to add lecture notes, checklist items, or study formulas..." }
            }
            Text(
                text = cleanPreview,
                fontSize = 13.sp,
                color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )

            val checklistProgress = remember(note.content) {
                MarkdownUtils.countChecklistProgress(note.content)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom Bar: Reminder Pill / Checklist Progress & Timestamp/Attachment
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Reminder Pill
                    if (note.alarmMinutes != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.4f) else Color(0xFFEEF2FF))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFF93C5FD) else Color(0xFF2563EB),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "${note.alarmMinutes}m alert",
                                    fontSize = 11.sp,
                                    color = if (isDark) Color(0xFF93C5FD) else Color(0xFF2563EB),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Checklist Progress Pill
                    if (checklistProgress != null) {
                        val (done, total) = checklistProgress
                        val isComplete = done == total
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isComplete) Color(0xFFDCFCE7)
                                    else (if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.4f) else Color(0xFFEFF6FF))
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (isComplete) Icons.Default.CheckCircle else Icons.Default.Checklist,
                                    contentDescription = null,
                                    tint = if (isComplete) Color(0xFF15803D) else (if (isDark) Color(0xFF93C5FD) else Color(0xFF2563EB)),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "$done/$total tasks",
                                    fontSize = 11.sp,
                                    color = if (isComplete) Color(0xFF15803D) else (if (isDark) Color(0xFF93C5FD) else Color(0xFF2563EB)),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Trailing: Attachment or Relative time
                if (!note.attachmentName.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (note.attachmentName.endsWith(".m4a")) Icons.Default.Mic else Icons.Default.AttachFile,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            if (note.attachmentName.endsWith(".m4a")) "Audio" else "1 File",
                            fontSize = 11.sp,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    val relativeTime = remember(note.updatedAt) {
                        val diff = System.currentTimeMillis() - note.updatedAt
                        when {
                            diff < 0 -> "Just now"
                            diff < 60_000L -> "Just now"
                            diff < 3600_000L -> "${(diff / 60_000L).coerceAtLeast(1)}m ago"
                            diff < 86400_000L -> "${diff / 3600_000L}h ago"
                            diff < 172800_000L -> "Yesterday"
                            else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(note.updatedAt))
                        }
                    }
                    Text(
                        text = relativeTime,
                        fontSize = 11.sp,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF94A3B8)
                    )
                }
            }
        }
    }
}
