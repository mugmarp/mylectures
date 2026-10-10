package com.mustime.features.notes

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mustime.features.timetable.domain.LectureNote
import com.mustime.features.timetable.domain.TimetableEntry
import com.mustime.ui.components.AcademicProfileSheet
import com.mustime.ui.components.RoomFloorBadge
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class NoteEditorTab {
    WRITE,
    PREVIEW
}

@OptIn(ExperimentalMaterial3Api::class)
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
        ?: "GEN101"
    val initialTitleVal = initialNote?.title ?: ""
    val initialContentVal = initialNote?.content ?: ""
    val initialAlarm = initialNote?.alarmMinutes ?: 15
    val initialTag = initialNote?.tag ?: "Lecture"
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

    // Editor tab: Write vs Live Preview
    var currentTab by remember { mutableStateOf(NoteEditorTab.WRITE) }

    val context = androidx.compose.ui.platform.LocalContext.current

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val name = try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx != -1) cursor.getString(idx) else null
                    } else null
                }
            } catch (_: Exception) { null }
            attachmentName = name ?: "Whiteboard_${System.currentTimeMillis() % 10000}.jpg"
        }
    }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val name = try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx != -1) cursor.getString(idx) else null
                    } else null
                }
            } catch (_: Exception) { null }
            attachmentName = name ?: "Slides_${selectedCourse.ifBlank { "Lecture" }}.pdf"
        }
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val name = try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx != -1) cursor.getString(idx) else null
                    } else null
                }
            } catch (_: Exception) { null }
            attachmentName = name ?: "Audio_Memo_${System.currentTimeMillis() % 10000}.m4a"
        }
    }

    // Modals
    var showClassPicker by remember { mutableStateOf(false) }
    var showCoursePickerModal by remember { mutableStateOf(false) }
    var showTagPicker by remember { mutableStateOf(false) }
    var showTemplatesSheet by remember { mutableStateOf(false) }
    var showHelpSheet by remember { mutableStateOf(false) }
    var showDiscardConfirmDialog by remember { mutableStateOf(false) }
    var showProfileSheet by remember { mutableStateOf(false) }
    var showCustomReminderDialog by remember { mutableStateOf(false) }
    var customReminderText by remember { mutableStateOf(selectedAlarmMinutes?.toString() ?: "45") }

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

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val handleBackPress: () -> Unit = {
        focusManager.clearFocus()
        keyboardController?.hide()
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

    val insertSnippet: (String) -> Unit = { snippet ->
        val text = contentValue.text
        val selection = contentValue.selection
        val insertPos = selection.start
        val needsLeadingNewline = insertPos > 0 && text[insertPos - 1] != '\n' && !snippet.startsWith("\n")
        val insertion = if (needsLeadingNewline) "\n$snippet" else snippet
        val newText = text.substring(0, insertPos) + insertion + text.substring(insertPos)
        val newCursor = insertPos + insertion.length
        contentValue = TextFieldValue(
            text = newText,
            selection = TextRange(newCursor)
        )
    }

    // Insert current date stamp
    val insertDateStamp: () -> Unit = {
        val formatter = SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault())
        val dateStr = "📅 **${formatter.format(Date())}**\n"
        insertSnippet(dateStr)
    }

    // Toggle checklist in preview mode
    val toggleChecklistInPreview: (Int) -> Unit = { lineIndex ->
        val updatedText = MarkdownUtils.toggleChecklistAt(contentValue.text, lineIndex)
        contentValue = contentValue.copy(text = updatedText)
    }

    // Calculate word & char count & checklist progress
    val wordsCount = remember(contentValue.text) {
        contentValue.text.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }.size
    }
    val charsCount = remember(contentValue.text) { contentValue.text.length }
    val checklistProgress = remember(contentValue.text) {
        MarkdownUtils.countChecklistProgress(contentValue.text)
    }

    val scrollState = rememberScrollState()

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

    // Course Selection Modal
    if (showCoursePickerModal) {
        var customCourseCode by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCoursePickerModal = false },
            title = { Text("Select Course", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Choose from your enrolled courses or enter a custom course code:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    availableCourses.forEach { course ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedCourse = course.code
                                    showCoursePickerModal = false
                                },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedCourse.equals(course.code, ignoreCase = true))
                                    MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(
                                            try { Color(android.graphics.Color.parseColor(course.color)) } catch (e: Exception) { Color(0xFF2563EB) }
                                        )
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(course.code, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(course.title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Custom Course Code:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customCourseCode,
                            onValueChange = { customCourseCode = it.uppercase() },
                            placeholder = { Text("e.g. CSC3100") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                if (customCourseCode.isNotBlank()) {
                                    selectedCourse = customCourseCode.trim()
                                    showCoursePickerModal = false
                                }
                            },
                            enabled = customCourseCode.isNotBlank()
                        ) {
                            Text("Set")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCoursePickerModal = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Tag Selection Modal
    if (showTagPicker) {
        val standardTags = listOf(
            "Lecture" to "General classroom notes & discussions",
            "Lab Prep" to "Practical assignments, code logs & experiments",
            "Study Guide" to "Comprehensive summaries & textbook synthesis",
            "Assignment" to "Coursework breakdown, research & references",
            "Revision" to "High-yield exam prep, formulas & confidence tracker",
            "Exam Prep" to "Targeted past papers & test solutions",
            "Quick Note" to "Lightweight thoughts & immediate reminders"
        )
        var customTagInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showTagPicker = false },
            title = { Text("Select Note Category", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    standardTags.forEach { (tagName, desc) ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    tag = tagName
                                    showTagPicker = false
                                },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (tag == tagName)
                                    MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("🏷️ $tagName", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Custom Tag:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customTagInput,
                            onValueChange = { customTagInput = it },
                            placeholder = { Text("e.g. Fieldwork") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                if (customTagInput.isNotBlank()) {
                                    tag = customTagInput.trim()
                                    showTagPicker = false
                                }
                            },
                            enabled = customTagInput.isNotBlank()
                        ) {
                            Text("Set")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTagPicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Class selection modal
    if (showClassPicker) {
        AlertDialog(
            onDismissRequest = { showClassPicker = false },
            title = { Text("Attach Timetable Class", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (attachedClassText.isNotBlank()) {
                        OutlinedButton(
                            onClick = {
                                attachedClassText = ""
                                attachedLocation = ""
                                lecturer = ""
                                showClassPicker = false
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Detach Current Class")
                        }
                    }

                    val matchingClasses = timetableClasses.filter {
                        it.courseCode.equals(selectedCourse, ignoreCase = true)
                    }.ifEmpty { timetableClasses }

                    if (matchingClasses.isEmpty()) {
                        Text("No specific timetable classes found. You can still write notes without an attached class.")
                    } else {
                        matchingClasses.forEach { entry ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        attachedClassText = "${entry.courseCode}: ${entry.courseTitle} · ${entry.day}, ${entry.startTime} – ${entry.endTime}"
                                        attachedLocation = if (!entry.room.isNullOrBlank() && entry.room.trim().uppercase() !in listOf("TBA", "TBD", "NONE", "N/A")) "📍 ${entry.room.trim()}" else "📍 Lecture Room"
                                        lecturer = entry.lecturer ?: "Course Lecturer"
                                        showClassPicker = false
                                    },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("${entry.courseCode} · ${entry.courseTitle}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    val roomPart = if (!entry.room.isNullOrBlank() && entry.room.trim().uppercase() !in listOf("TBA", "TBD", "NONE", "N/A")) " · ${entry.room.trim()}" else ""
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("${entry.day} ${entry.startTime} - ${entry.endTime}$roomPart", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        if (!entry.room.isNullOrBlank() && entry.room.trim().uppercase() !in listOf("TBA", "TBD", "NONE", "N/A")) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            RoomFloorBadge(roomName = entry.room, compact = true)
                                        }
                                    }
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

    // Templates BottomSheet
    if (showTemplatesSheet) {
        NoteTemplatesBottomSheet(
            onDismiss = { showTemplatesSheet = false },
            onSelectTemplate = { tpl ->
                if (title.isBlank() || title == "New Note") {
                    title = tpl.defaultTitle.replace("[Course Code]", selectedCourse)
                }
                tag = tpl.tag
                val filledContent = tpl.content
                    .replace("[Course Code]", selectedCourse)
                    .replace("[Date]", SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date()))
                contentValue = TextFieldValue(text = filledContent, selection = TextRange(filledContent.length))
            }
        )
    }

    // Markdown Guide BottomSheet
    if (showHelpSheet) {
        MarkdownHelpBottomSheet(
            onDismiss = { showHelpSheet = false },
            onInsertSnippet = { snippet ->
                insertSnippet(snippet)
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
            ) {
                // Unified Header: Back, Title, Mode Selector, Save Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = handleBackPress) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = if (initialNote == null) "New Note" else "Edit Note",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Write vs Preview Segmented Control
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Row(modifier = Modifier.padding(2.dp)) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (currentTab == NoteEditorTab.WRITE) MaterialTheme.colorScheme.primary
                                            else Color.Transparent
                                        )
                                        .clickable {
                                            focusManager.clearFocus()
                                            keyboardController?.hide()
                                            currentTab = NoteEditorTab.WRITE
                                        }
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = null,
                                            tint = if (currentTab == NoteEditorTab.WRITE) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            "Write",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (currentTab == NoteEditorTab.WRITE) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (currentTab == NoteEditorTab.PREVIEW) MaterialTheme.colorScheme.primary
                                            else Color.Transparent
                                        )
                                        .clickable {
                                            focusManager.clearFocus()
                                            keyboardController?.hide()
                                            currentTab = NoteEditorTab.PREVIEW
                                        }
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Visibility,
                                            contentDescription = null,
                                            tint = if (currentTab == NoteEditorTab.PREVIEW) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            "Preview",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (currentTab == NoteEditorTab.PREVIEW) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
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
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text("Save ✓", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
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
                .imePadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 18.dp, vertical = 12.dp)
        ) {
            // 1. TITLE INPUT
            BasicTextField(
                value = title,
                onValueChange = { title = it },
                textStyle = TextStyle(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                decorationBox = { innerTextField ->
                    if (title.isEmpty()) {
                        Text(
                            "Enter note title or lecture topic...",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                        )
                    }
                    innerTextField()
                }
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 6.dp)
            )

            // 2. METADATA PILLS STRIP
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Course Pill
                item {
                    AssistChip(
                        onClick = { showCoursePickerModal = true },
                        label = {
                            Text(
                                text = if (selectedCourse.isNotBlank()) "📚 $selectedCourse" else "📚 Select Course",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        trailingIcon = {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Category Tag Pill
                item {
                    AssistChip(
                        onClick = { showTagPicker = true },
                        label = { Text("🏷️ $tag", fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                        trailingIcon = {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(14.dp))
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Quick Templates Button
                item {
                    AssistChip(
                        onClick = { showTemplatesSheet = true },
                        leadingIcon = {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
                        },
                        label = { Text("Templates", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary) },
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Formatting Guide Pill
                item {
                    AssistChip(
                        onClick = { showHelpSheet = true },
                        leadingIcon = {
                            Icon(Icons.Outlined.HelpOutline, contentDescription = null, modifier = Modifier.size(14.dp))
                        },
                        label = { Text("Guide", fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Attached Class Pill
                item {
                    AssistChip(
                        onClick = { showClassPicker = true },
                        label = {
                            val labelText = if (attachedClassText.isNotBlank()) {
                                "📍 ${attachedClassText.substringBefore(" · ").take(18)}"
                            } else {
                                "📍 Attach Class"
                            }
                            Text(labelText, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Pre-class Alert Pill
                item {
                    AssistChip(
                        onClick = { showCustomReminderDialog = true },
                        label = {
                            val alertText = if (selectedAlarmMinutes != null) "⏰ ${selectedAlarmMinutes}m alert" else "⏰ Add Alert"
                            Text(alertText, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3. MAIN EDITOR CARD OR PREVIEW CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    if (currentTab == NoteEditorTab.WRITE) {
                        // VISUAL FORMATTING TOOLBAR (WYSIWYG Helpers for everyone)
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Bold
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { applyMarkdown("**", "**", "Bold text") }
                                        .padding(horizontal = 6.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("B", fontWeight = FontWeight.Black, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                                }

                                // Italic
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { applyMarkdown("*", "*", "Italic text") }
                                        .padding(horizontal = 6.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("I", fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                                }

                                // Heading 1
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { insertLinePrefix("# ") }
                                        .padding(horizontal = 6.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("H1", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                                }

                                // Heading 2
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { insertLinePrefix("## ") }
                                        .padding(horizontal = 6.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("H2", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                }

                                // Checklist / To-Do
                                IconButton(
                                    onClick = { insertLinePrefix("- [ ] ") },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Checklist,
                                        contentDescription = "Checklist item",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }

                                // Bullet List
                                IconButton(
                                    onClick = { insertLinePrefix("- ") },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.FormatListBulleted,
                                        contentDescription = "Bullet List",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }

                                // Numbered List
                                IconButton(
                                    onClick = { insertLinePrefix("1. ") },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.FormatListNumbered,
                                        contentDescription = "Numbered List",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }

                                // Quote / Callout
                                IconButton(
                                    onClick = { insertLinePrefix("> ") },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.FormatQuote,
                                        contentDescription = "Quote / Callout",
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }

                                // Code / Formula
                                IconButton(
                                    onClick = { applyMarkdown("`", "`", "formula") },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Code,
                                        contentDescription = "Code or Formula",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // Divider Line
                                IconButton(
                                    onClick = { insertSnippet("\n---\n") },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.HorizontalRule,
                                        contentDescription = "Divider",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // Insert Date Stamp
                                IconButton(
                                    onClick = insertDateStamp,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.Today,
                                        contentDescription = "Insert Date Stamp",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

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
                                .heightIn(min = 180.dp),
                            decorationBox = { innerTextField ->
                                if (contentValue.text.isEmpty()) {
                                    Text(
                                        "Start taking lecture notes, revision points, or checklists...\n• Tap formatting buttons above to style without syntax\n• Tap 'Templates' for full academic note outlines\n• Switch to 'Preview' tab anytime to view formatted notes",
                                        fontSize = 14.sp,
                                        lineHeight = 22.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                }
                                innerTextField()
                            }
                        )
                    } else {
                        // PREVIEW TAB (Rendered Rich Markdown)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "RICH PREVIEW",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 0.5.sp
                            )
                            if (checklistProgress != null) {
                                val (done, total) = checklistProgress
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (done == total) Color(0xFFDCFCE7) else MaterialTheme.colorScheme.primaryContainer
                                        )
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        "☑ $done / $total completed",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (done == total) Color(0xFF15803D) else MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(10.dp))

                        MarkdownViewer(
                            markdown = contentValue.text,
                            onToggleChecklist = toggleChecklistInPreview
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Markdown & Visual formatting",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            "$wordsCount words · $charsCount chars",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ATTACHED CLASS CARD (If attached)
            if (attachedClassText.isNotBlank()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showClassPicker = true },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .matchParentSize()
                                .width(6.dp)
                                .background(MaterialTheme.colorScheme.primary)
                        )

                        Column(modifier = Modifier.padding(start = 20.dp, top = 14.dp, end = 16.dp, bottom = 14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Outlined.EventNote,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "ATTACHED TIMETABLE CLASS",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        attachedLocation,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.tertiary,
                                        fontWeight = FontWeight.Medium
                                    )
                                    val roomClean = attachedLocation.removePrefix("📍").trim()
                                    if (roomClean.isNotBlank()) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        RoomFloorBadge(roomName = roomClean, compact = true)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = attachedClassText.substringBefore(" · "),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = attachedClassText.substringAfter(" · ", ""),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // PRE-CLASS ALERT REMINDER CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
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
                            Text(
                                if (selectedAlarmMinutes != null) "${selectedAlarmMinutes}m Chime" else "Disabled",
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val reminderOptions = listOf(
                            "None" to null,
                            "15m" to 15,
                            "30m" to 30,
                            "1h" to 60,
                            (if (selectedAlarmMinutes != null && selectedAlarmMinutes !in listOf(15, 30, 60)) "${selectedAlarmMinutes}m" else "Custom") to selectedAlarmMinutes
                        )

                        reminderOptions.forEachIndexed { idx, (label, minutes) ->
                            val isSelected = (idx == 4 && selectedAlarmMinutes != null && selectedAlarmMinutes !in listOf(15, 30, 60)) || (idx < 4 && selectedAlarmMinutes == minutes)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                    )
                                    .clickable {
                                        if (idx == 4) {
                                            showCustomReminderDialog = true
                                        } else {
                                            selectedAlarmMinutes = minutes
                                        }
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // QUICK ATTACHMENTS (Voice memo, Lecture slide, Whiteboard)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "ATTACHMENTS & RECORDINGS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = 0.5.sp
                )
                Text(
                    if (attachmentName.isNotBlank()) "1 file attached" else "None",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Record Audio / Audio Memo
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            if (isRecordingAudio) {
                                isRecordingAudio = false
                                attachmentName = "VoiceMemo_${selectedCourse.ifBlank { "Lecture" }}_${recordingDurationSeconds}s.m4a"
                            } else {
                                isRecordingAudio = true
                            }
                        },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isRecordingAudio) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isRecordingAudio) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
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
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isRecordingAudio) "Stop (${recordingDurationSeconds}s)" else "Voice Memo",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (isRecordingAudio) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Lecture Slide PDF (via real System Document Picker)
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { documentPickerLauncher.launch("application/pdf") },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Slide PDF", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }

                // Whiteboard Photo (via zero-permission Android Photo Picker)
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.tertiaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Whiteboard", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            // Attached file item badge
            if (attachmentName.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier
                            .padding(10.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (attachmentName.endsWith(".m4a")) Icons.Default.Mic else Icons.Default.Description,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = attachmentName,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                                Text(
                                    if (attachmentName.endsWith(".m4a")) "Audio Memo · Attached" else "Attached Document",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(
                            onClick = { attachmentName = "" },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove attachment",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
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

    if (showCustomReminderDialog) {
        AlertDialog(
            onDismissRequest = { showCustomReminderDialog = false },
            title = { Text("Custom Pre-Class Alert", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Minutes before class begins:", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customReminderText,
                        onValueChange = { customReminderText = it.filter { ch -> ch.isDigit() }.take(4) },
                        label = { Text("Minutes") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val parsed = customReminderText.toIntOrNull()?.coerceIn(1, 1440)
                    if (parsed != null) selectedAlarmMinutes = parsed
                    showCustomReminderDialog = false
                }) {
                    Text("Apply", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomReminderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
