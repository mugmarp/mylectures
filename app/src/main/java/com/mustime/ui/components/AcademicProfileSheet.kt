package com.mustime.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mustime.R
import com.mustime.TimetableApplication
import com.mustime.features.onboarding.FACULTIES
import com.mustime.features.timetable.ui.*
import com.mustime.ui.LocalAppTheme
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcademicProfileSheet(
    programme: String = "",
    academicYear: String = "",
    semester: String = "",
    onDismiss: () -> Unit,
    onReconfigureProfile: () -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val app = context.applicationContext as? TimetableApplication
    val repository = app?.repository

    val savedYear by (repository?.academicYearPref ?: flowOf("2026/2027"))
        .collectAsState(initial = repository?.getAcademicYear() ?: "2026/2027")
    val savedSem by (repository?.semesterPref ?: flowOf("Semester 1"))
        .collectAsState(initial = repository?.getSemester() ?: "Semester 1")

    val scope = rememberCoroutineScope()
    var showChangeProgrammeDialog by remember { mutableStateOf(false) }

    val effectiveAcademicYear = academicYear.ifBlank { savedYear }
    val effectiveSemester = semester.ifBlank { savedSem }
    val isDark = LocalAppTheme.current.isDark
    val surfaceColor = if (isDark) DarkSurfaceCard else Color.White
    val textColor = if (isDark) Color.White else TextPrimaryLight
    val textSub = if (isDark) Color(0xFF94A3B8) else TextMutedLight
    val cardBg = if (isDark) DarkSurfaceBase else Color(0xFFF8FAFC)
    val cardBorder = if (isDark) DarkBorderSubtle else BorderSubtleLight

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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
        ) {
            // Top Header: Avatar & University
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(1.5.dp, PrimaryBlue.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.app_logo),
                        contentDescription = "Academic Emblem",
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Academic Profile",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Mbarara University of Science & Tech",
                        fontSize = 13.sp,
                        color = textSub,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Current Programme Identity Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = cardBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ENROLLED PROGRAMME",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue,
                            letterSpacing = 0.5.sp
                        )
                        Box(
                            modifier = Modifier
                                .background(StatusGreenLive.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "Active",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = StatusGreenLive
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${programme.ifBlank { "BSE II" }} $effectiveAcademicYear",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = cardBorder, thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(14.dp))

                    ProfileItemRow(icon = Icons.Outlined.LocationOn, label = "Campus", value = "Kihumuro / Main Campus", isDark = isDark)
                    Spacer(modifier = Modifier.height(12.dp))
                    ProfileItemRow(icon = Icons.Outlined.CalendarMonth, label = "Semester", value = effectiveSemester, isDark = isDark)
                    Spacer(modifier = Modifier.height(12.dp))
                    ProfileItemRow(icon = Icons.Outlined.AccessTime, label = "Timetable Mode", value = "Class Timetable Synced", isDark = isDark)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action: Switch / Reconfigure Programme
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = cardBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        showChangeProgrammeDialog = true
                    }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(PrimaryBlue.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.SwapHoriz,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Change Programme / Class Group",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = textColor
                        )
                        Text(
                            text = "Select a different programme or class group",
                            fontSize = 12.sp,
                            color = textSub
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = null,
                        tint = textSub,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action: Open Settings
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = cardBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onDismiss()
                        onOpenSettings()
                    }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color(0xFF8B5CF6).copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Settings,
                            contentDescription = null,
                            tint = Color(0xFF8B5CF6),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Settings & Appearance",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = textColor
                        )
                        Text(
                            text = "Dark theme, alerts & sync controls",
                            fontSize = 12.sp,
                            color = textSub
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = null,
                        tint = textSub,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }

    if (showChangeProgrammeDialog) {
        QuickChangeProgrammeDialog(
            currentProgramme = programme,
            isDark = isDark,
            onDismiss = { showChangeProgrammeDialog = false },
            onProgrammeSelected = { newGroup ->
                scope.launch {
                    repository?.setProgrammePref(newGroup)
                    showChangeProgrammeDialog = false
                    onDismiss()
                }
            },
            onOpenFullReconfigure = {
                showChangeProgrammeDialog = false
                onDismiss()
                onReconfigureProfile()
            }
        )
    }
}

@Composable
fun QuickChangeProgrammeDialog(
    currentProgramme: String,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onProgrammeSelected: (String) -> Unit,
    onOpenFullReconfigure: () -> Unit
) {
    val currentProgCode = currentProgramme.substringBefore(" ").trim()
    val currentYear = currentProgramme.substringAfter(" ").trim().ifBlank { "I" }

    var selectedFacultyId by remember {
        val found = FACULTIES.find { f -> f.programmes.any { it.code.equals(currentProgCode, ignoreCase = true) } }
        mutableStateOf(found?.id ?: FACULTIES.first().id)
    }

    val selectedFaculty = remember(selectedFacultyId) {
        FACULTIES.find { it.id == selectedFacultyId } ?: FACULTIES.first()
    }

    var selectedProgrammeCode by remember(selectedFacultyId) {
        val prog = selectedFaculty.programmes.find { it.code.equals(currentProgCode, ignoreCase = true) }
            ?: selectedFaculty.programmes.first()
        mutableStateOf(prog.code)
    }

    val selectedProgramme = remember(selectedFaculty, selectedProgrammeCode) {
        selectedFaculty.programmes.find { it.code == selectedProgrammeCode } ?: selectedFaculty.programmes.first()
    }

    var selectedYearGroup by remember(selectedProgramme) {
        val yr = if (selectedProgramme.defaultYears.contains(currentYear)) currentYear
        else selectedProgramme.defaultYears.firstOrNull() ?: "I"
        mutableStateOf(yr)
    }

    val primaryColor = MaterialTheme.colorScheme.primary

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = if (isDark) DarkSurfaceCard else Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 580.dp)
                .padding(horizontal = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Switch Programme",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else TextPrimaryLight
                        )
                        Text(
                            text = "Select your faculty, program and year group",
                            fontSize = 12.sp,
                            color = if (isDark) Color(0xFF94A3B8) else TextMutedLight
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Faculty Chips
                Text(
                    text = "1. Faculty",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor
                )
                Spacer(modifier = Modifier.height(6.dp))
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(FACULTIES) { faculty ->
                        val isSelected = faculty.id == selectedFacultyId
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedFacultyId = faculty.id
                                selectedProgrammeCode = faculty.programmes.first().code
                            },
                            label = { Text(faculty.name.replace("Faculty of ", ""), fontSize = 11.sp) },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Programme Selector
                Text(
                    text = "2. Degree / Diploma Programme",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor
                )
                Spacer(modifier = Modifier.height(6.dp))
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(selectedFaculty.programmes) { prog ->
                        val isSelected = prog.code == selectedProgrammeCode
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) primaryColor.copy(alpha = 0.12f) else if (isDark) DarkSurfaceBase else Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, if (isSelected) primaryColor else if (isDark) DarkBorderSubtle else BorderSubtleLight),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedProgrammeCode = prog.code }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(if (isSelected) primaryColor else primaryColor.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = prog.code,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else primaryColor
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${prog.code} · ${prog.name}",
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isDark) Color.White else TextPrimaryLight,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "${prog.years} Year Curriculum",
                                        fontSize = 10.sp,
                                        color = if (isDark) Color(0xFF94A3B8) else TextMutedLight
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = primaryColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Year Group Selector
                Text(
                    text = "3. Academic Year Group",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    selectedProgramme.defaultYears.forEach { yr ->
                        val isSelected = yr == selectedYearGroup
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedYearGroup = yr },
                            label = { Text("Year $yr", fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                val chosenGroup = "${selectedProgramme.code} $selectedYearGroup"

                Button(
                    onClick = { onProgrammeSelected(chosenGroup) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                ) {
                    Text("Switch to $chosenGroup", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = onOpenFullReconfigure,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open Full Onboarding / Reconfigure", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun ProfileItemRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    isDark: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isDark) Color(0xFF94A3B8) else TextMutedLight,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                maxLines = 1,
                color = if (isDark) Color(0xFF94A3B8) else TextMutedLight
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isDark) Color.White else TextPrimaryLight
        )
    }
}
