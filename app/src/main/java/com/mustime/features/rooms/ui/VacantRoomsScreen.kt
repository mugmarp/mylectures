package com.mustime.features.rooms.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mustime.TimetableApplication
import com.mustime.core.util.TimeUtil
import com.mustime.features.rooms.BuildingLevel
import com.mustime.features.rooms.Campus
import com.mustime.features.rooms.RoomType
import com.mustime.features.rooms.SuggestionValue
import com.mustime.features.rooms.RoomVacancyStatus
import com.mustime.features.rooms.UniversityDirectory
import com.mustime.features.timetable.domain.TimetableEntry
import com.mustime.features.timetable.ui.*
import com.mustime.ui.LocalAppTheme
import com.mustime.ui.components.FloorIndicatorBadge
import com.mustime.ui.components.DedicatedDayPickerDialog
import com.mustime.ui.components.DedicatedTimePickerDialog
import java.util.Calendar

/**
 * Campus Vacant Room Finder.
 *
 * Room catalog is sourced from the authoritative MUST Room Allocation page
 * (index_rooms_teaching.html). Campus is DERIVED from the faculties of the
 * cohorts that actually use each room, so filtering by campus is reliable.
 *
 * Campus is auto-detected from the student's enrolled programme on first open,
 * and can be overridden with the campus chip row.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VacantRoomsScreen(
    onBack: () -> Unit,
    enrolledProgramme: String? = null
) {
    val context = LocalContext.current
    val app = context.applicationContext as? TimetableApplication
    val repository = app?.repository

    val allEntries by (repository?.getAllEntries() ?: kotlinx.coroutines.flow.flowOf(emptyList()))
        .collectAsState(initial = emptyList())

    val customEvents by (repository?.getCustomEvents() ?: kotlinx.coroutines.flow.flowOf(emptyList()))
        .collectAsState(initial = emptyList())

    val isDark = LocalAppTheme.current.isDark
    val surfaceColor = if (isDark) DarkSurfaceCard else Color.White
    val bgColor = if (isDark) DarkSurfaceBase else SurfaceBaseLight
    val textPrimary = if (isDark) Color.White else TextPrimaryLight
    val textMuted = if (isDark) Color(0xFF94A3B8) else TextMutedLight
    val borderSubtle = if (isDark) DarkBorderSubtle else BorderSubtleLight

    // Current real-time clock defaults
    val calendar = remember { Calendar.getInstance() }
    val initialDay = remember { TimeUtil.todayName() }
    val initialHour = remember { calendar.get(Calendar.HOUR_OF_DAY) }
    val initialMinute = remember { calendar.get(Calendar.MINUTE) }
    val initialTimeStr = remember { "%02d:%02d".format(initialHour, initialMinute) }

    // Active Query Parameters
    var queryDay by remember { mutableStateOf(initialDay) }
    var queryTime by remember { mutableStateOf(initialTimeStr) }
    var isLiveNow by remember { mutableStateOf(true) }

    // ---- Faculty default: strictly derives default Faculty Building and Campus ----
    val activeProgramme = enrolledProgramme ?: repository?.getInitialProgramme()
    val defaultScope = remember(activeProgramme) {
        programmeDefaults(activeProgramme)
    }

    var selectedCampus by remember(defaultScope) {
        mutableStateOf<String?>(defaultScope.campus)
    }
    var selectedBuilding by remember(defaultScope) {
        mutableStateOf<String?>(defaultScope.buildingCode)
    }

    // Filters
    var minGapMinutes by remember { mutableIntStateOf(30) } // Default 30 min
    var selectedLevel by remember { mutableStateOf<BuildingLevel?>(null) } // null = All levels
    var studyFriendlyOnly by remember { mutableStateOf(true) }
    var showPermanentlyVacant by remember { mutableStateOf(true) }
    var hideLibrary by remember { mutableStateOf(false) }  // libraries rank last (known baseline)
    var searchQuery by remember { mutableStateOf("") }
    var freeOnly by remember { mutableStateOf(false) }

    // Pickers visibility
    var campusDropdownExpanded by remember { mutableStateOf(false) }
    var buildingDropdownExpanded by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showDayPicker by remember { mutableStateOf(false) }
    var selectedRoomForSchedule by remember { mutableStateOf<RoomVacancyStatus?>(null) }

    // Reset building when campus changes (a building belongs to one campus)
    LaunchedEffect(selectedCampus) {
        if (selectedBuilding != null) {
            val b = UniversityDirectory.BUILDINGS.firstOrNull { it.code == selectedBuilding }
            if (b != null && !b.campus.equals(selectedCampus, ignoreCase = true)) {
                // Default to first building of that campus or null
                selectedBuilding = UniversityDirectory.buildingsForCampus(selectedCampus).firstOrNull()?.code
            }
        }
    }

    val campusBuildings = remember(selectedCampus) {
        UniversityDirectory.buildingsForCampus(selectedCampus)
    }

    // Calculate Room Vacancy algorithmically (scoped by campus + building)
    val vacancyStatuses = remember(allEntries, customEvents, queryDay, queryTime, minGapMinutes,
                                   selectedCampus, selectedBuilding) {
        UniversityDirectory.calculateRoomVacancy(
            allEntries = allEntries,
            dayOfWeek = queryDay,
            queryTimeStr = queryTime,
            minGapMinutes = minGapMinutes,
            customEvents = customEvents,
            campusFilter = selectedCampus,
            buildingFilter = selectedBuilding
        )
    }

    // Filter results
    val filteredRooms = remember(vacancyStatuses, selectedLevel, studyFriendlyOnly, minGapMinutes,
                                 searchQuery, showPermanentlyVacant, hideLibrary, freeOnly) {
        val base = vacancyStatuses.filter { status ->
            val matchesFree = !freeOnly || (!status.isOccupied && (status.gapMinutes >= minGapMinutes || status.freeUntil == "Rest of day"))
            val matchesLevel = selectedLevel == null || status.room.level == selectedLevel
            val matchesStudy = !studyFriendlyOnly || status.room.isStudyFriendly
            val matchesSearch = searchQuery.isBlank() ||
                    status.room.name.contains(searchQuery, ignoreCase = true) ||
                    status.room.code.contains(searchQuery, ignoreCase = true) ||
                    status.room.aliases.any { it.contains(searchQuery, ignoreCase = true) }

            val matchesVacantFlag = showPermanentlyVacant || !status.room.isPermanentlyVacant

            // When studyFriendlyOnly and free, apply the minimum gap requirement
            val matchesGap = if (!status.isOccupied && minGapMinutes > 0 && studyFriendlyOnly) {
                status.gapMinutes >= minGapMinutes || status.freeUntil == "Rest of day"
            } else true

            matchesFree && matchesLevel && matchesStudy && matchesSearch && matchesGap && matchesVacantFlag
        }
        // Sort strictly by availability:
        // 1. Free rooms first (occupied rooms last)
        // 2. Longest uninterrupted free window first (Rest of day, 3h, 2h, 1h...)
        // 3. Class rooms & labs before libraries (where students need new info)
        // 4. Room code for stable ordering
        base
            .filter { !hideLibrary || it.room.suggestionValue != SuggestionValue.LIBRARY }
            .sortedWith(
                compareBy(
                    { if (it.isOccupied) 1 else 0 },
                    { if (it.freeUntil == "Rest of day") -99999 else -it.gapMinutes },
                    { it.room.suggestionValue.suggestionRank },
                    { it.room.code }
                )
            )
    }

    val freeCount = vacancyStatuses.count {
        !it.isOccupied && (it.gapMinutes >= minGapMinutes || it.freeUntil == "Rest of day") && it.room.isStudyFriendly
    }
    val occupiedCount = vacancyStatuses.count { it.isOccupied && it.room.isStudyFriendly }
    val permanentCount = vacancyStatuses.count { it.room.isPermanentlyVacant && it.room.isStudyFriendly }

    Scaffold(
        containerColor = bgColor,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Vacant Room Finder",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = textPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .background(StatusGreenLive.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${vacancyStatuses.size} rooms",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusGreenLive
                                )
                            }
                        }
                        Text(
                            text = buildString {
                                append(selectedCampus?.let { Campus.entries.firstOrNull { c -> c.displayName == it }?.shortName } ?: "All Campuses")
                                if (selectedBuilding != null) {
                                    val b = UniversityDirectory.BUILDINGS.firstOrNull { it.code == selectedBuilding }
                                    if (b != null) append(" · ${b.name}")
                                }
                            },
                            fontSize = 12.sp,
                            color = textMuted
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = textPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showFilterSheet = true }) {
                        BadgedBox(
                            badge = {
                                if (!isLiveNow || minGapMinutes != 30 || selectedLevel != null || !studyFriendlyOnly || freeOnly) {
                                    Badge(containerColor = PrimaryBlue)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Tune,
                                contentDescription = "Advanced Filter & Time Options",
                                tint = if (!isLiveNow || minGapMinutes != 30 || selectedLevel != null || freeOnly) PrimaryBlue else textPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = surfaceColor)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "header_spacer") { Spacer(modifier = Modifier.height(2.dp)) }

            // SECTION 1: LOCATION DROPDOWNS (Campus on left, Building on right)
            item(key = "location_dropdowns") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Campus Dropdown (Left)
                        val campusDisplayText = selectedCampus?.let { cName ->
                            Campus.entries.firstOrNull { it.displayName == cName }?.shortName ?: cName
                        } ?: "All Campuses"

                        LocationDropdownSelector(
                            label = "Campus",
                            value = campusDisplayText,
                            icon = Icons.Outlined.School,
                            expanded = campusDropdownExpanded,
                            onExpandedChange = { campusDropdownExpanded = it },
                            modifier = Modifier.weight(1f),
                            isDark = isDark,
                            badgeText = if (selectedCampus == defaultScope.campus) "Default" else null
                        ) {
                            Campus.entries.forEach { campus ->
                                val isSelected = selectedCampus == campus.displayName
                                val isDefault = campus.displayName.equals(defaultScope.campus, ignoreCase = true)
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = campus.displayName,
                                                    fontSize = 13.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) PrimaryBlue else textPrimary
                                                )
                                                Text(
                                                    text = if (campus == Campus.KIHUMURO) "FAST, FCI · 3 Buildings" else "Medicine, Science, Business · 4 Buildings",
                                                    fontSize = 10.5.sp,
                                                    color = textMuted
                                                )
                                            }
                                            if (isDefault) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .background(PrimaryBlue.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "Default",
                                                        fontSize = 10.sp,
                                                        color = PrimaryBlue,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                            if (isSelected) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = "Selected",
                                                    tint = PrimaryBlue,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        selectedCampus = campus.displayName
                                        if (campus.displayName.equals(defaultScope.campus, ignoreCase = true)) {
                                            selectedBuilding = defaultScope.buildingCode
                                        } else {
                                            selectedBuilding = UniversityDirectory.buildingsForCampus(campus.displayName).firstOrNull()?.code
                                        }
                                        campusDropdownExpanded = false
                                    }
                                )
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "All Campuses",
                                            fontSize = 13.sp,
                                            fontWeight = if (selectedCampus == null) FontWeight.Bold else FontWeight.Normal,
                                            color = if (selectedCampus == null) PrimaryBlue else textPrimary
                                        )
                                        if (selectedCampus == null) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = PrimaryBlue,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    selectedCampus = null
                                    selectedBuilding = null
                                    campusDropdownExpanded = false
                                }
                            )
                        }

                        // Building Dropdown (Right)
                        val currentBuildingObj = UniversityDirectory.BUILDINGS.firstOrNull { it.code == selectedBuilding }
                        val displayBuildingName = currentBuildingObj?.name ?: "All Buildings"
                        val isFacultyBuildingSelected = selectedBuilding == defaultScope.buildingCode && selectedCampus == defaultScope.campus

                        LocationDropdownSelector(
                            label = "Building",
                            value = displayBuildingName,
                            icon = Icons.Outlined.Domain,
                            expanded = buildingDropdownExpanded,
                            onExpandedChange = { buildingDropdownExpanded = it },
                            modifier = Modifier.weight(1f),
                            isDark = isDark,
                            badgeText = if (isFacultyBuildingSelected) "Faculty" else null,
                            menuAlignmentEnd = true
                        ) {
                            campusBuildings.forEach { b ->
                                val isSelected = selectedBuilding == b.code
                                val isMyFaculty = b.code == defaultScope.buildingCode && (selectedCampus == null || selectedCampus == defaultScope.campus)
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = b.name,
                                                    fontSize = 13.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) PrimaryBlue else textPrimary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${b.totalRooms} rooms on catalog",
                                                    fontSize = 10.5.sp,
                                                    color = textMuted
                                                )
                                            }
                                            if (isMyFaculty) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .background(PrimaryBlue.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "My Faculty ★",
                                                        fontSize = 10.sp,
                                                        color = PrimaryBlue,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                            if (isSelected) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = "Selected",
                                                    tint = PrimaryBlue,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        selectedBuilding = b.code
                                        if (selectedCampus == null) {
                                            selectedCampus = b.campus
                                        }
                                        buildingDropdownExpanded = false
                                    }
                                )
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (selectedCampus != null) "All ${selectedCampus} Buildings" else "All Buildings",
                                            fontSize = 13.sp,
                                            fontWeight = if (selectedBuilding == null) FontWeight.Bold else FontWeight.Normal,
                                            color = if (selectedBuilding == null) PrimaryBlue else textPrimary
                                        )
                                        if (selectedBuilding == null) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = PrimaryBlue,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    selectedBuilding = null
                                    buildingDropdownExpanded = false
                                }
                            )
                        }
                    }

                    // Reset to Faculty Default indicator bar (if overridden)
                    val isAwayFromDefaults = defaultScope.campus != selectedCampus ||
                            (defaultScope.buildingCode != null && selectedBuilding != defaultScope.buildingCode)
                    if (isAwayFromDefaults) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = PrimaryBlue.copy(alpha = if (isDark) 0.18f else 0.08f),
                            border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.25f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedCampus = defaultScope.campus
                                    selectedBuilding = defaultScope.buildingCode
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.RestartAlt,
                                    contentDescription = null,
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Reset to faculty default: ",
                                    fontSize = 11.5.sp,
                                    color = textPrimary
                                )
                                Text(
                                    text = "${defaultScope.buildingName} (${if (defaultScope.campus.contains("Kihumuro", ignoreCase = true)) "Kihumuro" else "Town"})",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryBlue
                                )
                            }
                        }
                    }
                }
            }

            // SECTION 2: SEARCH BAR & QUICK STATUS
            item(key = "room_search_and_status") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search room (e.g. L1, LR2, Lab...)", fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = textMuted, modifier = Modifier.size(20.dp))
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear search", tint = textMuted, modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = surfaceColor,
                            unfocusedContainerColor = surfaceColor,
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = borderSubtle
                        )
                    )

                    // Compact Quick Status Summary & Free-Only Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(if (isLiveNow) StatusGreenLive else Color(0xFFF59E0B), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isLiveNow) "$freeCount free now" else "$queryDay $queryTime · $freeCount free",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF86EFAC) else Color(0xFF15803D)
                            )
                            if (occupiedCount > 0) {
                                Text(
                                    text = " · $occupiedCount in use",
                                    fontSize = 12.sp,
                                    color = textMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        FilterChip(
                            selected = freeOnly,
                            onClick = { freeOnly = !freeOnly },
                            label = {
                                Text(
                                    text = "Free only",
                                    fontSize = 11.sp,
                                    fontWeight = if (freeOnly) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            leadingIcon = if (freeOnly) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp)) }
                            } else null,
                            shape = RoundedCornerShape(8.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryBlue.copy(alpha = 0.15f),
                                selectedLabelColor = PrimaryBlue,
                                selectedLeadingIconColor = PrimaryBlue
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = freeOnly,
                                borderColor = if (freeOnly) PrimaryBlue else borderSubtle
                            )
                        )
                    }
                }
            }

            // SECTION 3: ROOM CARDS LIST
            if (filteredRooms.isEmpty()) {
                item(key = "empty_rooms") {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = surfaceColor,
                        border = BorderStroke(1.dp, borderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Outlined.MeetingRoom,
                                contentDescription = null,
                                tint = textMuted,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No Rooms Match the Selected Filter",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = textPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Try lowering the minimum study gap, clearing search, or switching building.",
                                fontSize = 12.sp,
                                color = textMuted,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            if (searchQuery.isNotBlank() || freeOnly || selectedLevel != null) {
                                Spacer(modifier = Modifier.height(14.dp))
                                OutlinedButton(
                                    onClick = {
                                        searchQuery = ""
                                        freeOnly = false
                                        selectedLevel = null
                                    },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Clear Search & Filters", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            } else {
                items(filteredRooms, key = { it.room.id }) { status ->
                    RoomVacancyCard(
                        status = status,
                        isDark = isDark,
                        showCampus = selectedCampus == null,
                        onClick = { selectedRoomForSchedule = status }
                    )
                }
            }

            // Footer: data provenance
            item(key = "data_provenance_notice") {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = surfaceColor.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, borderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Info,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Room catalog and campus assignment sourced from the MUST Room Allocation " +
                                    "timetable (2026/2027 Semester I). ${UniversityDirectory.ALL_ROOMS.size} rooms " +
                                    "across ${UniversityDirectory.BUILDINGS.size} buildings. Verify venue changes on " +
                                    "the official MUST website (timetable.must.ac.ug).",
                            fontSize = 12.sp,
                            color = textMuted
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Advanced Filters Sheet
    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            containerColor = surfaceColor
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Filter & Schedule Options",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    TextButton(
                        onClick = {
                            val now = Calendar.getInstance()
                            queryDay = TimeUtil.todayName()
                            queryTime = "%02d:%02d".format(now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE))
                            isLiveNow = true
                            minGapMinutes = 30
                            selectedLevel = null
                            studyFriendlyOnly = true
                            showPermanentlyVacant = true
                            hideLibrary = false
                            freeOnly = false
                        }
                    ) {
                        Text("Reset All", color = PrimaryBlue, fontSize = 13.sp)
                    }
                }

                // Query Time & Day Section
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Query Time",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        FilterChip(
                            selected = isLiveNow,
                            onClick = {
                                val now = Calendar.getInstance()
                                queryDay = TimeUtil.todayName()
                                queryTime = "%02d:%02d".format(now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE))
                                isLiveNow = true
                            },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(if (isLiveNow) StatusGreenLive else textMuted, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Live Now", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showDayPicker = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Outlined.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(queryDay, fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                isLiveNow = false
                                showTimePicker = true
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Outlined.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(queryTime, fontSize = 13.sp)
                        }
                    }
                }

                // Minimum Study Gap
                Column {
                    Text(
                        text = "Minimum Study Gap",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val gapOptions = listOf(
                            "Any free (0m)" to 0,
                            "15 min" to 15,
                            "30 min (Default)" to 30,
                            "45 min" to 45,
                            "1 hour" to 60
                        )
                        items(gapOptions) { (label, value) ->
                            FilterChip(
                                selected = minGapMinutes == value,
                                onClick = { minGapMinutes = value },
                                label = { Text(label, fontSize = 12.sp) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                // Floor / Level
                Column {
                    Text(
                        text = "Floor / Level",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = selectedLevel == null,
                                onClick = { selectedLevel = null },
                                label = { Text("All Floors", fontSize = 12.sp) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                        items(BuildingLevel.entries.toTypedArray()) { level ->
                            FilterChip(
                                selected = selectedLevel == level,
                                onClick = { selectedLevel = level },
                                label = { Text(level.shortName, fontSize = 12.sp) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                // Study Friendly & Library Toggles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !studyFriendlyOnly,
                        onClick = { studyFriendlyOnly = !studyFriendlyOnly },
                        label = { Text(if (studyFriendlyOnly) "Classrooms Only" else "Include Offices", fontSize = 12.sp) },
                        shape = RoundedCornerShape(8.dp)
                    )
                    FilterChip(
                        selected = hideLibrary,
                        onClick = { hideLibrary = !hideLibrary },
                        label = { Text("Hide Library", fontSize = 12.sp) },
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                Button(
                    onClick = { showFilterSheet = false },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Apply & View Rooms")
                }
            }
        }
    }

    // Modal Pickers
    if (showTimePicker) {
        DedicatedTimePickerDialog(
            initialTime = queryTime,
            title = "Set Vacancy Query Time",
            onTimeSelected = { newTime ->
                queryTime = newTime
                isLiveNow = false
            },
            onDismiss = { showTimePicker = false }
        )
    }

    if (showDayPicker) {
        DedicatedDayPickerDialog(
            selectedDay = queryDay,
            onDaySelected = { newDay ->
                queryDay = newDay
                isLiveNow = false
            },
            onDismiss = { showDayPicker = false }
        )
    }

    selectedRoomForSchedule?.let { status ->
        RoomScheduleDetailDialog(
            status = status,
            dayOfWeek = queryDay,
            isDark = isDark,
            onDismiss = { selectedRoomForSchedule = null }
        )
    }
}

/**
 * Encapsulates the default location preference derived strictly from a student's faculty.
 */
data class ProgrammeLocationDefault(
    val campus: String,        // e.g. "Kihumuro Campus" or "Town Campus"
    val buildingCode: String?, // e.g. "FCI", "FAST", "SCI", "PHA", "IMS", or null
    val buildingName: String   // e.g. "FCI Building", "FAST Building"
)

/**
 * Resolves default Faculty Building and Campus strictly according to MUST's academic structure:
 *  - Faculty of Computing & Informatics (FCI): BCS, BIT, BSE -> Kihumuro Campus, FCI Building
 *  - Faculty of Applied Sciences & Tech (FAST): BME, EEE, PEEM, CVE, MIE -> Kihumuro Campus, FAST Building
 *  - Faculty of Science (FOS): BS, DLT -> Town Campus, Science Block
 *  - Faculty of Medicine / Pharmacy (FOM): PHA, PHS, MBR, BNS, MLS, BSP, DCM, DEM, DCAM -> Town Campus, Pharmacy / Health Complex
 *  - Faculty of Business & Management Sciences (FBMS): BBA, BSAF, ECO, BPSM -> Town Campus, IMS / Business Building
 *  - Faculty of Interdisciplinary Studies (FIS): BSAL, BGWH, BPCD -> Town Campus, IMS / Business Building
 */
fun programmeDefaults(programme: String?): ProgrammeLocationDefault {
    if (programme.isNullOrBlank()) {
        return ProgrammeLocationDefault(
            campus = Campus.KIHUMURO.displayName,
            buildingCode = null,
            buildingName = "All Buildings"
        )
    }
    // Robustly resolve programme code via TimetableMatcher (supporting track labels like "BS BIOLOGICAL I", "DLT CHEMISTRY II")
    val parsed = com.mustime.features.timetable.domain.TimetableMatcher.parseGroupParts(programme)
    val head = programme.trim().split(Regex("[\\s\\-_]+")).firstOrNull()?.uppercase() ?: ""
    val alias = mapOf(
        "MLC" to "MLS", "BNC" to "BNS", "BSPC" to "BSP", "PEM" to "PEEM",
        "CIV" to "CVE", "BAF" to "BSAF"
    )
    val code = parsed?.code ?: alias[head] ?: head

    return when (code) {
        // FCI: Computing & Informatics -> Kihumuro Campus, FCI Building
        "BCS", "BIT", "BSE" -> ProgrammeLocationDefault(
            campus = Campus.KIHUMURO.displayName,
            buildingCode = "FCI",
            buildingName = "FCI Building"
        )

        // FAST: Applied Sciences & Technology -> Kihumuro Campus, FAST Building
        "BME", "EEE", "PEEM", "CVE", "MIE" -> ProgrammeLocationDefault(
            campus = Campus.KIHUMURO.displayName,
            buildingCode = "FAST",
            buildingName = "FAST Building"
        )

        // Science -> Town Campus, Science Block
        "BS", "DLT" -> ProgrammeLocationDefault(
            campus = Campus.TOWN.displayName,
            buildingCode = "SCI",
            buildingName = "Science Block"
        )

        // Medicine & Pharmacy -> Town Campus, Pharmacy / Health Complex
        "PHA", "PHS", "MBR", "BNS", "MLS", "BSP", "DCM", "DEM", "DCAM" -> ProgrammeLocationDefault(
            campus = Campus.TOWN.displayName,
            buildingCode = "PHA",
            buildingName = "Pharmacy / Health Complex"
        )

        // Business & Management Sciences -> Town Campus, IMS / Business Building
        "BBA", "BSAF", "ECO", "BPSM" -> ProgrammeLocationDefault(
            campus = Campus.TOWN.displayName,
            buildingCode = "IMS",
            buildingName = "IMS / Business Building"
        )

        // Interdisciplinary Studies -> Town Campus, IMS / Business Building
        "BSAL", "BGWH", "BPCD" -> ProgrammeLocationDefault(
            campus = Campus.TOWN.displayName,
            buildingCode = "IMS",
            buildingName = "IMS / Business Building"
        )

        else -> ProgrammeLocationDefault(
            campus = Campus.KIHUMURO.displayName,
            buildingCode = null,
            buildingName = "All Buildings"
        )
    }
}

/**
 * Maps an enrolled programme group (e.g. "BCS II", "MBR III") to a campus display name.
 */
fun campusForProgramme(programme: String?): String? = programmeDefaults(programme).campus

/**
 * Dedicated M3 Dropdown Selector card for Campus and Building filtering.
 * Replaces bulky textfields with a sleek, responsive selector card and full-width menu.
 */
@Composable
fun LocationDropdownSelector(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    isDark: Boolean,
    badgeText: String? = null,
    menuAlignmentEnd: Boolean = false,
    menuContent: @Composable ColumnScope.() -> Unit
) {
    val surfaceColor = if (isDark) DarkSurfaceCard else Color.White
    val textPrimary = if (isDark) Color.White else TextPrimaryLight
    val textMuted = if (isDark) Color(0xFF94A3B8) else TextMutedLight
    val borderSubtle = if (isDark) DarkBorderSubtle else BorderSubtleLight

    Box(modifier = modifier) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = surfaceColor,
            border = BorderStroke(1.2.dp, if (expanded) PrimaryBlue else borderSubtle),
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .clickable { onExpandedChange(!expanded) }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(18.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = label.uppercase(),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = textMuted,
                            letterSpacing = 0.5.sp
                        )
                        if (badgeText != null) {
                            Box(
                                modifier = Modifier
                                    .background(PrimaryBlue.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = badgeText,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryBlue
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = value,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse $label" else "Expand $label",
                    tint = textMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            offset = if (menuAlignmentEnd) androidx.compose.ui.unit.DpOffset(x = (-60).dp, y = 4.dp) else androidx.compose.ui.unit.DpOffset(x = 0.dp, y = 4.dp),
            modifier = Modifier
                .widthIn(min = 220.dp, max = 275.dp)
                .background(surfaceColor, RoundedCornerShape(14.dp)),
            properties = androidx.compose.ui.window.PopupProperties(focusable = true)
        ) {
            menuContent()
        }
    }
}

/**
 * Formats room code for display, stripping redundant suffixes and library redundancies.
 */
fun formatRoomDisplayCode(room: com.mustime.features.rooms.RoomItem): String {
    val raw = room.code
    return when {
        raw.contains("Kihumuro Library Room 1", ignoreCase = true) -> "Study Room 1"
        raw.contains("Kihumuro Library Room 2", ignoreCase = true) -> "Study Room 2"
        raw.contains("Kihumuro Library Room 3", ignoreCase = true) -> "Study Room 3"
        raw.contains("Kihumuro Library Room 4", ignoreCase = true) -> "Study Room 4"
        raw.contains("FCI Library 1", ignoreCase = true) -> "Library Room 1"
        raw.contains("FCI Library 2", ignoreCase = true) -> "Library Room 2"
        else -> raw
    }
}

/**
 * Visual Room Card displaying availability status, "Free until X", gap minutes, and level tag.
 */
@Composable
fun RoomVacancyCard(
    status: RoomVacancyStatus,
    isDark: Boolean,
    showCampus: Boolean = false,
    onClick: () -> Unit
) {
    val room = status.room
    val surfaceColor = if (isDark) DarkSurfaceCard else Color.White
    val textPrimary = if (isDark) Color.White else TextPrimaryLight
    val textMuted = if (isDark) Color(0xFF94A3B8) else TextMutedLight
    val borderSubtle = if (isDark) DarkBorderSubtle else BorderSubtleLight

    val statusColor = when {
        !room.isStudyFriendly -> Color(0xFF8B5CF6)
        status.isOccupied -> Color(0xFFEF4444)
        room.isPermanentlyVacant -> Color(0xFF0EA5E9)
        status.gapMinutes >= 60 || status.freeUntil == "Rest of day" -> Color(0xFF16A34A)
        else -> Color(0xFFF59E0B)
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = surfaceColor,
        border = BorderStroke(1.dp, borderSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val displayCode = formatRoomDisplayCode(room)
                    Text(
                        text = displayCode,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    // Dedicated floor level badge directly beside Room Identifier
                    FloorIndicatorBadge(
                        level = room.level,
                        compact = true,
                        isDark = isDark
                    )

                    // Suppress redundant type badges: Libraries and Classrooms don't need a duplicate tag
                    val isLibrary = room.type == RoomType.LIBRARY || room.buildingName.contains("Library", ignoreCase = true)
                    val showTypeBadge = !isLibrary && room.type != RoomType.CLASSROOM
                    if (showTypeBadge) {
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isDark) Color(0xFF334155).copy(alpha = 0.5f) else Color(0xFFF1F5F9),
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = room.type.displayName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = textMuted,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = if (showCampus) "${room.buildingName} · ${room.campus}" else room.buildingName,
                    fontSize = 13.sp,
                    color = textMuted,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(statusColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = status.formattedAvailability,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }

                if (!status.isOccupied && status.nextSession != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Next: ${status.nextSession.courseCode} · ${status.nextSession.courseTitle}",
                        fontSize = 11.sp,
                        color = textMuted,
                        maxLines = 1
                    )
                } else if (status.isOccupied && status.currentSession != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "In session: ${status.currentSession.courseCode} (${status.currentSession.programmeGroup})",
                        fontSize = 11.sp,
                        color = textMuted,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = "View Day Schedule",
                tint = textMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Dialog showing complete schedule of all classes scheduled in this room on the query day.
 */
@Composable
fun RoomScheduleDetailDialog(
    status: RoomVacancyStatus,
    dayOfWeek: String,
    isDark: Boolean,
    onDismiss: () -> Unit
) {
    val room = status.room
    val sessions = status.allDaySessions
    val textPrimary = if (isDark) Color.White else TextPrimaryLight
    val textMuted = if (isDark) Color(0xFF94A3B8) else TextMutedLight

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = if (isDark) DarkSurfaceCard else Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp)
                .padding(horizontal = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = formatRoomDisplayCode(room),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                            FloorIndicatorBadge(
                                level = room.level,
                                compact = false,
                                isDark = isDark
                            )
                        }
                        Text(
                            text = "${room.buildingName} · $dayOfWeek",
                            fontSize = 12.sp,
                            color = textMuted
                        )
                        if (room.usedByFaculties.isNotEmpty()) {
                            Text(
                                text = "Used by: ${room.usedByFaculties.joinToString(", ")}",
                                fontSize = 11.sp,
                                color = PrimaryBlue
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
                    }
                }

                if (room.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF8B5CF6).copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = room.notes,
                            fontSize = 12.sp,
                            color = Color(0xFF8B5CF6),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "All Scheduled Sessions (${sessions.size})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (sessions.isEmpty()) {
                    val isLibraryVenue = room.type == RoomType.LIBRARY || room.buildingName.contains("Library", ignoreCase = true)
                    val isPermanentlyVacant = room.isPermanentlyVacant || room.sessionsThisSemester == 0

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isDark) DarkSurfaceBase else Color(0xFFF0FDF4),
                        border = BorderStroke(1.dp, if (isDark) DarkBorderSubtle else Color(0xFFBBF7D0)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(StatusGreenLive.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isLibraryVenue) Icons.Outlined.LocalLibrary else Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    tint = StatusGreenLive,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = when {
                                    isLibraryVenue -> "Open for Self-Study & Reading"
                                    isPermanentlyVacant -> "Unallocated Classroom · Free All Day"
                                    else -> "No Scheduled Lectures on $dayOfWeek"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.5.sp,
                                color = textPrimary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = when {
                                    isLibraryVenue -> "This is a dedicated academic library / study facility. No lectures are timetable-allocated here, so it is freely accessible for individual study, reading, and research throughout $dayOfWeek."
                                    isPermanentlyVacant -> "This venue has zero scheduled teaching sessions in the 2026/2027 Semester I timetable. You can freely use it for quiet revision, coursework, or group discussions."
                                    else -> "No teaching sessions are allocated to this room on $dayOfWeek. It is open and available for student study, revision, or coursework."
                                },
                                fontSize = 12.sp,
                                color = textMuted,
                                textAlign = TextAlign.Center,
                                lineHeight = 17.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = StatusGreenLive.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = if (isLibraryVenue) "✓ Quiet Study Friendly" else "✓ Full Day Free",
                                        color = StatusGreenLive,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = PrimaryBlue.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = room.level.displayName,
                                        color = PrimaryBlue,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(sessions) { session: TimetableEntry ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDark) DarkSurfaceBase else Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, if (isDark) DarkBorderSubtle else BorderSubtleLight),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.width(68.dp)
                                    ) {
                                        Text(
                                            text = session.startTime,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = PrimaryBlue
                                        )
                                        Text(
                                            text = session.endTime,
                                            fontSize = 11.sp,
                                            color = textMuted
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))
                                    VerticalDivider(
                                        modifier = Modifier.height(34.dp),
                                        color = textMuted.copy(alpha = 0.3f)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = session.courseCode,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = textPrimary
                                        )
                                        Text(
                                            text = session.courseTitle,
                                            fontSize = 12.sp,
                                            color = textMuted,
                                            maxLines = 1
                                        )
                                        if (session.programmeGroup.isNotBlank()) {
                                            Text(
                                                text = "Group: ${session.programmeGroup}",
                                                fontSize = 11.sp,
                                                color = PrimaryBlue
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Close")
                }
            }
        }
    }
}
