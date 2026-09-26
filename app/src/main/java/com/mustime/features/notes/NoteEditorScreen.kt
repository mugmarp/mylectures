package com.mustime.features.notes

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mustime.features.timetable.domain.LectureNote
import com.mustime.features.timetable.domain.TimetableEntry
import com.mustime.ui.components.AcademicProfileSheet
import kotlinx.coroutines.delay

@Composable
fun NoteEditorScreen(
    initialNote: LectureNote? = null,
    availableCourses: List<CourseOption>,
    timetableClasses: List<TimetableEntry>,
    onSettingsClick: () -> Unit = {},
    onReconfigureAcademicProfile: () -> Unit = {},
    onSave: (
        naturalKey: String?,
        courseCode: String,
        title: String,
        content: String,
        alarmMinutes: Int?,
        tag: String?,
        attachedClass: String?,
        attachmentName: String?
    ) -> Unit,
    onCancel: () -> Unit
) {
    val initialCourse = initialNote?.naturalKey?.split("|")?.firstOrNull()
        ?: availableCourses.firstOrNull()?.code
        ?: ""
    val initialTitleVal = initialNote?.title ?: ""
    val initialContentVal = initialNote?.content ?: ""
    val initialAlarm = initialNote?.alarmMinutes ?: 15
    val initialTag = initialNote?.tag ?: "General"
    val initialClassText = initialNote?.attachedClass ?: ""
    val initialAttachment = initialNote?.attachmentName ?: ""

    var selectedCourse by remember { mutableStateOf(initialCourse) }
    var title by remember { mutableStateOf(initialTitleVal) }
    var contentValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = initialContentVal,
                selection = TextRange(initialContentVal.length)
            )
        )
    }
    var selectedAlarmMinutes by remember { mutableStateOf<Int?>(initialAlarm) }
    var tag by remember { mutableStateOf(initialTag) }
    var attachedClassText by remember { mutableStateOf(initialClassText) }
    var attachedLocation by remember { mutableStateOf("📍 Lecture Room") }
    var lecturer by remember { mutableStateOf("Course Lecturer") }
    var attachmentName by remember { mutableStateOf(initialAttachment) }

    var showClassPicker by remember { mutableStateOf(false) }
    var showDiscardConfirmDialog by remember { mutableStateOf(false) }
    var showProfileSheet by remember { mutableStateOf(false) }

    // Audio recording state
    var isRecordingAudio by remember { mutableStateOf(false) }
    var recordingDurationSeconds by remember { mutableIntStateOf(0) }

    LaunchedEffect(isRecordingAudio) {
        if (isRecordingAudio) {
            recordingDurationSeconds = 0
            while (isRecordingAudio) {
                delay(1000)
                recordingDurationSeconds++
            }
        }
    }

    // Determine if form has unsaved modifications
    val isModified = selectedCourse != initialCourse ||
            title != initialTitleVal ||
            contentValue.text != initialContentVal ||
            selectedAlarmMinutes != initialAlarm ||
            tag != initialTag ||
            attachedClassText != initialClassText ||
            attachmentName != initialAttachment

    val handleBackPress: () -> Unit = {
        if (isModified) {
            showDiscardConfirmDialog = true
        } else {
            onCancel()
        }
    }

    BackHandler(enabled = true, onBack = handleBackPress)

    // Markdown insertion helper supporting selection wrapping
    val applyMarkdown: (prefix: String, suffix: String, defaultPlaceholder: String) -> Unit = { prefix, suffix, defaultPlaceholder ->
        val text = contentValue.text
        val selection = contentValue.selection
        if (selection.start != selection.end) {
            val selectedText = text.substring(selection.start, selection.end)
            val newText = text.substring(0, selection.start) + prefix + selectedText + suffix + text.substring(selection.end)
            val newCursor = selection.end + prefix.length + suffix.length
            contentValue = TextFieldValue(
                text = newText,
                selection = TextRange(newCursor)
            )
        } else {
            val newText = text.substring(0, selection.start) + prefix + defaultPlaceholder + suffix + text.substring(selection.start)
            val newStart = selection.start + prefix.length
            val newEnd = newStart + defaultPlaceholder.length
            contentValue = TextFieldValue(
                text = newText,
                selection = TextRange(newStart, newEnd)
            )
        }
    }

    val insertLinePrefix: (prefix: String) -> Unit = { linePrefix ->
        val text = contentValue.text
        val selection = contentValue.selection
        val insertPos = selection.start
        val needsLeadingNewline = insertPos > 0 && text[insertPos - 1] != '\n'
        val insertion = if (needsLeadingNewline) "\n$linePrefix" else linePrefix
        val newText = text.substring(0, insertPos) + insertion + text.substring(insertPos)
        val newCursor = insertPos + insertion.length
        contentValue = TextFieldValue(
            text = newText,
            selection = TextRange(newCursor)
        )
    }

    // Discard Confirmation Dialog
    if (showDiscardConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirmDialog = false },
            icon = {
                Icon(
                    Icons.Outlined.WarningAmber,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    text = "Discard unsaved changes?",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "You have unsaved changes to this note. Leaving now will cause edits to be lost.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardConfirmDialog = false
                        onCancel()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Discard", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirmDialog = false }) {
                    Text("Keep Editing")
                }
            }
        )
    }

    // Calculate word & char count
    val wordsCount = remember(contentValue.text) {
        contentValue.text.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }.size
    }
    val charsCount = remember(contentValue.text) { contentValue.text.length }

    val scrollState = rememberScrollState()

    // Class selection modal
    if (showClassPicker) {
        AlertDialog(
            onDismissRequest = { showClassPicker = false },
            title = { Text("Attach Timetable Class", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    val matchingClasses = timetableClasses.filter {
                        it.courseCode.equals(selectedCourse, ignoreCase = true)
                    }.ifEmpty { timetableClasses.take(5) }

                    if (matchingClasses.isEmpty()) {
                        Text("No specific timetable classes found for $selectedCourse.")
                    } else {
                        matchingClasses.forEach { entry ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        attachedClassText = "${entry.courseCode}: ${entry.courseTitle} · ${entry.day}, ${entry.startTime} – ${entry.endTime}"
                                        attachedLocation = if (!entry.room.isNullOrBlank() && entry.room.trim().uppercase() !in listOf("TBA", "TBD", "NONE", "N/A")) "📍 ${entry.room.trim()}" else ""
                                        lecturer = entry.lecturer ?: ""
                                        showClassPicker = false
                                    },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("${entry.courseCode} · ${entry.courseTitle}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    val roomPart = if (!entry.room.isNullOrBlank() && entry.room.trim().uppercase() !in listOf("TBA", "TBD", "NONE", "N/A")) " · ${entry.room.trim()}" else ""
                                    Text("${entry.day} ${entry.startTime} - ${entry.endTime}$roomPart", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showClassPicker = false }) {
                    Text("Close")
                }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
            ) {
                // Top row with Back, Title, Profile
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = handleBackPress) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = if (initialNote == null) "Add Note" else "Note Editor",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable { showProfileSheet = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Subheader Action Bar (Cancel, Auto-saving, Save Note)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = handleBackPress) {
                        Text(
                            "Cancel",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    AutoSavingIndicator()

                    Button(
                        onClick = {
                            onSave(
                                initialNote?.naturalKey,
                                selectedCourse,
                                title.ifBlank { "Academic Note: $selectedCourse" },
                                contentValue.text,
                                selectedAlarmMinutes,
                                tag,
                                attachedClassText,
                                attachmentName
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
                    ) {
                        Text("Save Note ✓", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), thickness = 1.dp)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // COURSE CODE SECTION
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.BookmarkBorder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "COURSE CODE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = 0.5.sp
                    )
                }
                Text("Required", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Horizontal Course Chips (if available)
            if (availableCourses.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val courses = availableCourses.take(4)

                    courses.forEach { course ->
                        val isSelected = selectedCourse.equals(course.code, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                )
                                .clickable {
                                    selectedCourse = course.code
                                    val matched = timetableClasses.find { it.courseCode.equals(course.code, ignoreCase = true) }
                                    if (matched != null) {
                                        attachedClassText = "${matched.courseCode}: ${matched.courseTitle} · ${matched.day}, ${matched.startTime} – ${matched.endTime}"
                                        attachedLocation = if (!matched.room.isNullOrBlank() && matched.room.trim().uppercase() !in listOf("TBA", "TBD", "NONE", "N/A")) "📍 ${matched.room.trim()}" else ""
                                        lecturer = matched.lecturer ?: "Course Lecturer"
                                    } else {
                                        attachedClassText = "${course.code}: ${course.title} · Weekly Session"
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${course.code} · ${course.title.take(8)}",
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                                if (isSelected) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                OutlinedTextField(
                    value = selectedCourse,
                    onValueChange = { selectedCourse = it },
                    placeholder = { Text("e.g. Course Code") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // TITLE INPUT
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "TITLE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    BasicTextField(
                        value = title,
                        onValueChange = { title = it },
                        textStyle = TextStyle(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { innerTextField ->
                            if (title.isEmpty()) {
                                Text(
                                    "e.g. Lecture Notes & Key Concepts",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                            innerTextField()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ATTACHED CLASS CARD
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showClassPicker = true },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            ) {
                Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                    // Left primary accent strip
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(6.dp)
                            .background(MaterialTheme.colorScheme.primary)
                    )

                    Column(modifier = Modifier.padding(16.dp).weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Outlined.EventNote,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        "Attached Class",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    attachedLocation,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Icon(
                                Icons.Default.SwapHoriz,
                                contentDescription = "Change Class",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = attachedClassText.substringBefore(" · ").ifBlank { if (selectedCourse.isNotBlank()) "$selectedCourse: Class Session" else "Class Session" },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = attachedClassText.substringAfter(" · ", "Scheduled Session"),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = lecturer,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // PRE-CLASS ALERT REMINDER
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.NotificationsActive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "PRE-CLASS ALERT REMINDER",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Push + Sound",
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Reminder intervals
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val reminderOptions = listOf(
                            "None" to null,
                            "15m" to 15,
                            "30m" to 30,
                            "1h" to 60,
                            "Custom" to 45
                        )

                        reminderOptions.forEach { (label, minutes) ->
                            val isSelected = selectedAlarmMinutes == minutes
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                    )
                                    .clickable { selectedAlarmMinutes = minutes }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (selectedAlarmMinutes != null) {
                                "Will chime an audio notification at ${selectedAlarmMinutes}m before your lecture begins."
                            } else {
                                "No pre-class reminder alarm will be scheduled."
                            },
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // MARKDOWN TOOLBAR & TEXT AREA
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Formatting Toolbar (Selection-Aware)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            // Bold
                            Text(
                                "B",
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { applyMarkdown("**", "**", "Bold text") }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                            // Italic
                            Text(
                                "I",
                                fontStyle = FontStyle.Italic,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { applyMarkdown("*", "*", "Italic text") }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                            // Bullet List
                            Icon(
                                Icons.Default.FormatListBulleted,
                                contentDescription = "Bullet List",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { insertLinePrefix("• ") }
                            )
                            // Numbered List
                            Icon(
                                Icons.Default.FormatListNumbered,
                                contentDescription = "Numbered List",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { insertLinePrefix("1. ") }
                            )
                            // Checklist
                            Icon(
                                Icons.Default.Checklist,
                                contentDescription = "Checklist",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { insertLinePrefix("- [ ] ") }
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            // Attachment Pin
                            Icon(
                                Icons.Outlined.AttachFile,
                                contentDescription = "Attach File",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable {
                                        attachmentName = "Lecture_Notes_Revision.pdf"
                                    }
                            )
                            // Link
                            Icon(
                                Icons.Outlined.Link,
                                contentDescription = "Insert Link",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { applyMarkdown("[", "](https://moodle.must.ac.ug)", "Link Title") }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Text Area with selection tracking
                    BasicTextField(
                        value = contentValue,
                        onValueChange = { contentValue = it },
                        textStyle = TextStyle(
                            fontSize = 15.sp,
                            lineHeight = 24.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 140.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Markdown supported", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$wordsCount words · $charsCount chars", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // QUICK ATTACHMENTS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "QUICK ATTACHMENTS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = 0.5.sp
                )
                Text(
                    if (attachmentName.isNotBlank()) "1 file attached" else "No files attached",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3 Action Cards: Record Audio, Lecture Slide, Whiteboard
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Record Audio (with recording toggle)
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            if (isRecordingAudio) {
                                isRecordingAudio = false
                                attachmentName = "Audio_VoiceMemo_${System.currentTimeMillis() % 1000}.m4a"
                            } else {
                                isRecordingAudio = true
                            }
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isRecordingAudio) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isRecordingAudio) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isRecordingAudio) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (isRecordingAudio) Icons.Default.Stop else Icons.Default.Mic,
                                contentDescription = if (isRecordingAudio) "Stop Recording" else "Record Audio",
                                tint = if (isRecordingAudio) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isRecordingAudio) "Stop (${recordingDurationSeconds}s)" else "Record Audio",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (isRecordingAudio) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isRecordingAudio) "Tap to attach" else "Voice memo",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Lecture Slide
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { attachmentName = "Lecture_Slides_Notes.pdf" },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Lecture Slide", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text("PDF / PPTX", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Whiteboard
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { attachmentName = "Whiteboard_Photo.jpg" },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.tertiaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Whiteboard", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text("Snap photo", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Attached file item
            if (attachmentName.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (attachmentName.endsWith(".m4a")) Icons.Default.Mic else Icons.Default.Description,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = attachmentName,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                                Text(
                                    if (attachmentName.endsWith(".m4a")) "Audio Memo · Attached just now" else "1.4 MB · Uploaded just now",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(
                            onClick = { attachmentName = "" },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove attachment",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))
        }
    }

    if (showProfileSheet) {
        AcademicProfileSheet(
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
private fun AutoSavingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "autosave")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .graphicsLayer { this.alpha = alphaAnim }
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            "AUTO-SAVING",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 0.5.sp
        )
    }
}
