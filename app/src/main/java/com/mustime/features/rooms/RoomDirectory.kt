package com.mustime.features.rooms

import com.mustime.core.util.TimeUtil
import com.mustime.features.timetable.domain.CustomEvent
import com.mustime.features.timetable.domain.TimetableEntry

/**
 * Room catalog for the Vacant Room Finder.
 *
 * GENERATED from the authoritative MUST Room Allocation page:
 *   https://timetable.must.ac.ug/index_rooms_teaching.html
 *   2026/2027 SEMESTER I — 90 rooms, 892 scheduled sessions
 *
 * Campus assignment is DERIVED, not guessed: each room's campus comes from the
 * faculties of the cohorts that actually use it (campusSource = "COHORT_DERIVED").
 * Only the 17 rooms with no scheduled sessions fall back to name inference
 * (campusSource = "NAME_INFERRED").
 *
 * Do not hand-edit. Regenerate from rooms_resolved.json.
 */

enum class RoomType(val displayName: String, val isStudyFriendlyDefault: Boolean) {
    CLASSROOM("Classroom", true),
    COMPUTER_LAB("Computer Lab", true),
    LIBRARY("Library / Study Area", true),
    SCIENCE_LAB("Science / Clinical Lab", true),
    STUDIO_ROOM("Design Studio / Workspace", true),
    MEETING_ROOM("Meeting Room / Board Room", false),
    POSTGRADUATE("Postgraduate Room", false),
    OFFICE("Staff Office / Lounge", false),
    CLINICAL("Clinical / Ward Venue", false)
}

enum class BuildingLevel(val displayName: String, val shortName: String) {
    GROUND("Ground Floor", "Ground"),
    FIRST("First Floor", "1st Floor"),
    SECOND("Second Floor", "2nd Floor"),
    THIRD("Third Floor", "3rd Floor"),
    FOURTH("Fourth Floor", "4th Floor")
}

enum class Campus(val displayName: String, val shortName: String) {
    KIHUMURO("Kihumuro Campus", "Kihumuro"),
    TOWN("Town Campus", "Town")
}

/**
 * How much NEW information a room suggestion carries.
 *
 * The app's value is surfacing rooms whose availability a student would not
 * otherwise know. Everyone already assumes the library is free — telling them
 * so adds nothing. "GFS02 is free until 14:00" is genuinely new information,
 * which is why class rooms rank ABOVE the library.
 *
 * Lower rank = suggested earlier.
 */
enum class SuggestionValue(val displayName: String, val suggestionRank: Int) {
    CLASS_ROOM("Lecture room / lab", 0),   // availability is the unknown - highest value
    STUDY_SPACE("Study space", 1),         // skills / practice / studio rooms
    LIBRARY("Library", 2),                 // known baseline - lowest novelty
    RESTRICTED("Not for student study", 3) // filtered out of suggestions
}

data class Building(
    val code: String,
    val name: String,
    val campus: String,
    val studyFriendlyRooms: Int,
    val totalRooms: Int
)

data class RoomItem(
    val id: String,
    val code: String,
    val name: String,
    val buildingCode: String,
    val buildingName: String,
    val campus: String,
    val level: BuildingLevel,
    val type: RoomType,
    val isStudyFriendly: Boolean = type.isStudyFriendlyDefault,
    val aliases: List<String> = emptyList(),
    val sessionsThisSemester: Int = 0,
    val campusSource: String = "COHORT_DERIVED",
    val suggestionValue: SuggestionValue = SuggestionValue.CLASS_ROOM,
    val usedByFaculties: List<String> = emptyList(),
    val notes: String = ""
) {
    /** True when the source data lists this room but schedules nothing in it. */
    val isPermanentlyVacant: Boolean get() = sessionsThisSemester == 0

    fun matchesRoomString(queryRoom: String?): Boolean {
        if (queryRoom.isNullOrBlank()) return false
        val clean = queryRoom.trim()
        if (clean.equals(code.trim(), ignoreCase = true)) return true
        if (clean.equals(name.trim(), ignoreCase = true)) return true
        if (aliases.any { it.trim().equals(clean, ignoreCase = true) }) return true

        val cleanNorm = normalize(clean)
        if (cleanNorm == normalize(code) || cleanNorm == normalize(name)) return true

        for (alias in aliases) {
            val aliasNorm = normalize(alias)
            if (cleanNorm == aliasNorm) return true
            val parts = cleanNorm.split("/", ",", ";", "&").map { it.trim() }
            if (parts.any { it == aliasNorm || it == normalize(code) }) return true
        }
        return false
    }

    private fun normalize(s: String): String =
        s.lowercase().replace("-", " ").replace(Regex("\\s+"), " ").trim()
}

data class RoomVacancyStatus(
    val room: RoomItem,
    val isOccupied: Boolean,
    val currentSession: TimetableEntry?,
    val occupiedUntil: String?,
    val nextSession: TimetableEntry?,
    val freeUntil: String?,
    val gapMinutes: Int,
    val allDaySessions: List<TimetableEntry>
) {
    val isUsableStudyGap: Boolean
        get() = !isOccupied && (gapMinutes >= 30 || freeUntil == "Rest of day")

    val formattedAvailability: String
        get() = if (isOccupied) {
            if (!occupiedUntil.isNullOrBlank()) "Occupied until $occupiedUntil" else "Currently Occupied"
        } else {
            if (room.isPermanentlyVacant) "Free all semester (no scheduled classes)"
            else if (freeUntil == "Rest of day" || nextSession == null) "Free for rest of day"
            else "Free until $freeUntil (${formatMinutes(gapMinutes)})"
        }

    private fun formatMinutes(minutes: Int): String {
        if (minutes < 60) return "${minutes}m"
        val h = minutes / 60
        val m = minutes % 60
        return if (m == 0) "${h}h" else "${h}h ${m}m"
    }
}

object UniversityDirectory {

    // ============================================================
    // BUILDING CATALOG  (6 buildings, 2 campuses)
    // ============================================================
    val BUILDINGS = listOf(
        Building("FCI", "FCI Building", Campus.KIHUMURO.displayName, 14, 14),
        Building("FAST", "FAST Building", Campus.KIHUMURO.displayName, 13, 13),
        Building("KLIB", "Kihumuro Library", Campus.KIHUMURO.displayName, 4, 4),
        Building("SCI", "Science Block", Campus.TOWN.displayName, 13, 13),
        Building("PHA", "Pharmacy / Health Complex", Campus.TOWN.displayName, 13, 18),
        Building("IMS", "IMS / Business Building", Campus.TOWN.displayName, 11, 13),
        Building("CLIN", "Clinical / Hospital", Campus.TOWN.displayName, 0, 17)
    )

    // ============================================================
    // ALL ROOMS — 90 rooms from the authoritative Room Allocation page
    // ============================================================
    val ALL_ROOMS: List<RoomItem> = listOf(
        RoomItem(
            id = "Biochem Lab",
            code = "Biochem Lab",
            name = "Biochem Lab",
            buildingCode = "PHA",
            buildingName = "Pharmacy / Health Complex",
            campus = "Town Campus",
            level = BuildingLevel.GROUND,
            type = RoomType.SCIENCE_LAB,
            aliases = listOf("Biochem Lab"),
            sessionsThisSemester = 19,
            campusSource = "COHORT_DERIVED",
            suggestionValue = SuggestionValue.CLASS_ROOM,
            usedByFaculties = listOf("FOM"),
            notes = "19 scheduled session(s) in 2026/2027 Sem I."
        ),
        RoomItem(
            id = "Bloodbank and Hospital",
            code = "Bloodbank and Hospital",
            name = "Bloodbank and Hospital",
            buildingCode = "CLIN",
            buildingName = "Clinical / Hospital",
            campus = "Town Campus",
            level = BuildingLevel.GROUND,
            type = RoomType.CLINICAL,
            isStudyFriendly = false,
            aliases = listOf("Bloodbank and Hospital"),
            sessionsThisSemester = 0,
            campusSource = "NAME_INFERRED",
            suggestionValue = SuggestionValue.RESTRICTED,
            notes = "No scheduled sessions - listed in Room Allocation but unused this semester. NOTE: duplicate listing in source data."
        ),
        RoomItem(
            id = "Community",
            code = "Community",
            name = "Community",
            buildingCode = "CLIN",
            buildingName = "Clinical / Hospital",
            campus = "Town Campus",
            level = BuildingLevel.GROUND,
            type = RoomType.CLINICAL,
            isStudyFriendly = false,
            aliases = listOf("Community"),
            sessionsThisSemester = 1,
            campusSource = "COHORT_DERIVED",
            suggestionValue = SuggestionValue.RESTRICTED,
            usedByFaculties = listOf("FIS"),
            notes = "1 scheduled session(s) in 2026/2027 Sem I."
        ),
        RoomItem(
            id = "DS LAB IV",
            code = "DS LAB IV",
            name = "DS LAB IV",
            buildingCode = "IMS",
            buildingName = "IMS / Business Building",
            campus = "Town Campus",
            level = BuildingLevel.FIRST,
            type = RoomType.COMPUTER_LAB,
            aliases = listOf("DS LAB IV", "DS LAB 4", "DSLABIV"),
            sessionsThisSemester = 17,
            campusSource = "COHORT_DERIVED",
            suggestionValue = SuggestionValue.CLASS_ROOM,
            usedByFaculties = listOf("FBMS", "FIS", "FOM"),
            notes = "17 scheduled session(s) in 2026/2027 Sem I."
        ),
        RoomItem(
            id = "Dissection Room",
            code = "Dissection Room",
            name = "Dissection Room",
            buildingCode = "CLIN",
            buildingName = "Clinical / Hospital",
            campus = "Town Campus",
            level = BuildingLevel.GROUND,
            type = RoomType.CLINICAL,
            isStudyFriendly = false,
            aliases = listOf("Dissection Room"),
            sessionsThisSemester = 2,
            campusSource = "COHORT_DERIVED",
            suggestionValue = SuggestionValue.RESTRICTED,
            usedByFaculties = listOf("FOM"),
            notes = "2 scheduled session(s) in 2026/2027 Sem I."
        ),
        RoomItem(
            id = "ESS CLINIC",
            code = "ESS CLINIC",
            name = "ESS CLINIC",
            buildingCode = "CLIN",
            buildingName = "Clinical / Hospital",
            campus = "Town Campus",
            level = BuildingLevel.GROUND,
            type = RoomType.CLINICAL,
            isStudyFriendly = false,
            aliases = listOf("ESS CLINIC"),
            sessionsThisSemester = 0,
            campusSource = "NAME_INFERRED",
            suggestionValue = SuggestionValue.RESTRICTED,
            notes = "No scheduled sessions - listed in Room Allocation but unused this semester."
        ),
        RoomItem(
            id = "FCI L1",
            code = "FCI L1",
            name = "FCI L1",
            buildingCode = "FCI",
            buildingName = "FCI Building",
            campus = "Kihumuro Campus",
            level = BuildingLevel.GROUND,
            type = RoomType.CLASSROOM,
            aliases = listOf("FCI L1", "Lecture Room 1", "LR1", "FCI LR1", "FCI-LR1"),
            sessionsThisSemester = 13,
            campusSource = "COHORT_DERIVED",
            suggestionValue = SuggestionValue.CLASS_ROOM,
            usedByFaculties = listOf("FAST", "FCI"),
            notes = "13 scheduled session(s) in 2026/2027 Sem I."
        ),
        RoomItem(
            id = "FCI L2",
            code = "FCI L2",
            name = "FCI L2",
            buildingCode = "FCI",
            buildingName = "FCI Building",
            campus = "Kihumuro Campus",
            level = BuildingLevel.GROUND,
            type = RoomType.CLASSROOM,
            aliases = listOf("FCI L2", "Lecture Room 2", "LR2", "FCI LR2", "FCI-LR2"),
            sessionsThisSemester = 11,
            campusSource = "COHORT_DERIVED",
            suggestionValue = SuggestionValue.CLASS_ROOM,
            usedByFaculties = listOf("FAST", "FCI"),
            notes = "11 scheduled session(s) in 2026/2027 Sem I."
        ),
        RoomItem(
            id = "FCI L3",
            code = "FCI L3",
            name = "FCI L3",
            buildingCode = "FCI",
            buildingName = "FCI Building",
            campus = "Kihumuro Campus",
            level = BuildingLevel.FIRST,
            type = RoomType.CLASSROOM,
            aliases = listOf("FCI L3", "Lecture Room 3", "LR3", "FCI LR3", "FCI-LR3"),
            sessionsThisSemester = 9,
            campusSource = "COHORT_DERIVED",
            suggestionValue = SuggestionValue.CLASS_ROOM,
            usedByFaculties = listOf("FAST", "FCI"),
            notes = "9 scheduled session(s) in 2026/2027 Sem I."
        ),
        RoomItem(
            id = "FCI L4",
            code = "FCI L4",
            name = "FCI L4",
            buildingCode = "FCI",
            buildingName = "FCI Building",
            campus = "Kihumuro Campus",
            level = BuildingLevel.FIRST,
            type = RoomType.CLASSROOM,
            aliases = listOf("FCI L4", "Lecture Room 4", "LR4", "FCI LR4", "FCI-LR4"),
            sessionsThisSemester = 11,
            campusSource = "COHORT_DERIVED",
            suggestionValue = SuggestionValue.CLASS_ROOM,
            usedByFaculties = listOf("FAST", "FCI"),
            notes = "11 scheduled session(s) in 2026/2027 Sem I."
        ),
        RoomItem(
            id = "FCI L5",
            code = "FCI L5",
            name = "FCI L5",
            buildingCode = "FCI",
            buildingName = "FCI Building",
            campus = "Kihumuro Campus",
            level = BuildingLevel.SECOND,
            type = RoomType.CLASSROOM,
            aliases = listOf("FCI L5", "Lecture Room 5", "LR5", "FCI LR5", "FCI-LR5"),
            sessionsThisSemester = 9,
            campusSource = "COHORT_DERIVED",
            suggestionValue = SuggestionValue.CLASS_ROOM,
            usedByFaculties = listOf("FAST", "FCI"),
            notes = "9 scheduled session(s) in 2026/2027 Sem I."
        ),
        RoomItem(
            id = "FCI L6",
            code = "FCI L6",
            name = "FCI L6",
            buildingCode = "FCI",
            buildingName = "FCI Building",
            campus = "Kihumuro Campus",
            level = BuildingLevel.SECOND,
            type = RoomType.CLASSROOM,
            aliases = listOf("FCI L6", "Lecture Room 6", "LR6", "FCI LR6", "FCI-LR6"),
            sessionsThisSemester = 10,
            campusSource = "COHORT_DERIVED",
            suggestionValue = SuggestionValue.CLASS_ROOM,
            usedByFaculties = listOf("FAST", "FCI"),
            notes = "10 scheduled session(s) in 2026/2027 Sem I."
        ),
        RoomItem(
            id = "FCI LAB 1",
            code = "FCI LAB 1",
            name = "FCI LAB 1",
            buildingCode = "FCI",
            buildingName = "FCI Building",
            campus = "Kihumuro Campus",
            level = BuildingLevel.GROUND,
            type = RoomType.COMPUTER_LAB,
            aliases = listOf("FCI LAB 1", "Computer Room 1", "CR1", "FCI CR1", "FCI-LR1"),
            sessionsThisSemester = 0,
            campusSource = "NAME_INFERRED",
            suggestionValue = SuggestionValue.CLASS_ROOM,
            notes = "No scheduled sessions - listed in Room Allocation but unused this semester. NOTE: exists in FCI directory (Computer Rooms 1-6) but no classes scheduled this semester."
        ),
        RoomItem(
            id = "FCI LAB 2",
            code = "FCI LAB 2",
            name = "FCI LAB 2",
            buildingCode = "FCI",
            buildingName = "FCI Building",
            campus = "Kihumuro Campus",
            level = BuildingLevel.GROUND,
            type = RoomType.COMPUTER_LAB,
            aliases = listOf("FCI LAB 2", "Computer Room 2", "CR2", "FCI CR2", "FCI-LR2"),
            sessionsThisSemester = 0,
            campusSource = "NAME_INFERRED",
            suggestionValue = SuggestionValue.CLASS_ROOM,
            notes = "No scheduled sessions - listed in Room Allocation but unused this semester. NOTE: exists in FCI directory (Computer Rooms 1-6) but no classes scheduled this semester."
        ),
        RoomItem(
            id = "FCI LAB 3",
            code = "FCI LAB 3",
            name = "FCI LAB 3",
            buildingCode = "FCI",
            buildingName = "FCI Building",
            campus = "Kihumuro Campus",
            level = BuildingLevel.FIRST,
            type = RoomType.COMPUTER_LAB,
            aliases = listOf("FCI LAB 3", "Computer Room 3", "CR3", "FCI CR3", "FCI-LR3"),
            sessionsThisSemester = 4,
            campusSource = "COHORT_DERIVED",
            suggestionValue = SuggestionValue.CLASS_ROOM,
            usedByFaculties = listOf("FCI"),
            notes = "4 scheduled session(s) in 2026/2027 Sem I."
        ),
        RoomItem(
            id = "FCI LAB 4",
            code = "FCI LAB 4",
            name = "FCI LAB 4",
            buildingCode = "FCI",
            buildingName = "FCI Building",
            campus = "Kihumuro Campus",
            level = BuildingLevel.FIRST,
            type = RoomType.COMPUTER_LAB,
            aliases = listOf("FCI LAB 4", "Computer Room 4", "CR4", "FCI CR4", "FCI-LR4"),
            sessionsThisSemester = 11,
            campusSource = "COHORT_DERIVED",
            suggestionValue = SuggestionValue.CLASS_ROOM,
            usedByFaculties = listOf("FCI"),
            notes = "11 scheduled session(s) in 2026/2027 Sem I."
        ),
        RoomItem(
            id = "FCI LAB 5",
            code = "FCI LAB 5",
            name = "FCI LAB 5",
            buildingCode = "FCI",
            buildingName = "FCI Building",
            campus = "Kihumuro Campus",
            level = BuildingLevel.SECOND,
            type = RoomType.COMPUTER_LAB,
            aliases = listOf("FCI LAB 5", "Computer Room 5", "CR5", "FCI CR5", "FCI-LR5"),
            sessionsThisSemester = 1,
            campusSource = "COHORT_DERIVED",
            suggestionValue = SuggestionValue.CLASS_ROOM,
            usedByFaculties = listOf("FCI"),
            notes = "1 scheduled session(s) in 2026/2027 Sem I."
        ),
        RoomItem(
            id = "FCI LAB 6",
            code = "FCI LAB 6",
            name = "FCI LAB 6",
            buildingCode = "FCI",
            buildingName = "FCI Building",
            campus = "Kihumuro Campus",
            level = BuildingLevel.SECOND,
            type = RoomType.COMPUTER_LAB,
            aliases = listOf("FCI LAB 6", "Computer Room 6", "CR6", "FCI CR6", "FCI-LR6"),
            sessionsThisSemester = 0,
            campusSource = "NAME_INFERRED",
            suggestionValue = SuggestionValue.CLASS_ROOM,
            notes = "No scheduled sessions - listed in Room Allocation but unused this semester. NOTE: exists in FCI directory (Computer Rooms 1-6) but no classes scheduled this semester."
        ),
        RoomItem(
            id = "FFS01",
            code = "FFS01",
            name = "FFS01",
            buildingCode = "FAST",
            buildingName = "FAST Building",
            campus = "Kihumuro Campus",
            level = BuildingLevel.FIRST,
            type = RoomType.CLASSROOM,
            aliases = listOf("FFS01"),
            sessionsThisSemester = 10,
            campusSource = "COHORT_DERIVED",
            suggestionValue = SuggestionValue.CLASS_ROOM,
            usedByFaculties = listOf("FAST"),
            notes = "10 scheduled session(s) in 2026/2027 Sem I."
        ),
        RoomItem(
            id = "FFS02",
            code = "FFS02",
            name = "FFS02",
            buildingCode = "FAST",
            buildingName = "FAST Building",
            campus = "Kihumuro Campus",
            level = BuildingLevel.FIRST,
            type = RoomType.CLASSROOM,
            aliases = listOf("FFS02"),
            sessionsThisSemester = 10,
            campusSource = "COHORT_DERIVED",
            suggestionValue = SuggestionValue.CLASS_ROOM,
            usedByFaculties = listOf("FAST"),
            notes = "10 scheduled session(s) in 2026/2027 Sem I."
        ),
        RoomItem(
            id = "Faculty of Science Computer Lab",
            code = "Faculty of Science Computer Lab",
            name = "Faculty of Science Computer Lab",
            buildingCode = "SCI",
            buildingName = "Science Block",
            campus = "Town Campus",
            level = BuildingLevel.GROUND,
            type = RoomType.COMPUTER_LAB,
            aliases = listOf("Faculty of Science Computer Lab"),
            sessionsThisSemester = 0,
            campusSource = "NAME_INFERRED",
            suggestionValue = SuggestionValue.CLASS_ROOM,
            notes = "No scheduled sessions - listed in Room Allocation but unused this semester."
        ),
        RoomItem(
            id = "GFL01",
            code = "GFL01",
            name = "GFL01",
            buildingCode = "FAST",
            buildingName = "FAST Building",
            campus = "Kihumuro Campus",
            level = BuildingLevel.GROUND,
            type = RoomType.CLASSROOM,
            aliases = listOf("GFL01"),
            sessionsThisSemester = 16,
            campusSource = "COHORT_DERIVED",
            suggestionValue = SuggestionValue.CLASS_ROOM,
            usedByFaculties = listOf("FAST", "FCI"),
            notes = "16 scheduled session(s) in 2026/2027 Sem I."
        ),
        RoomItem(
            id = "GFL02",
            code = "GFL02",
            name = "GFL02",
            buildingCode = "FAST",
            buildingName = "FAST Building",
            campus = "Kihumuro Campus",
            level = BuildingLevel.GROUND,
            type = RoomType.CLASSROOM,
            aliases = listOf("GFL02"),
            sessionsThisSemester = 11,
            campusSource = "COHORT_DERIVED",
            suggestionValue = SuggestionValue.CLASS_ROOM,
            usedByFaculties = listOf("FAST", "FCI"),
            notes = "11 scheduled session(s) in 2026/2027 Sem I."
        ),
        RoomItem(
            id = "GFS02",
            code = "GFS02",
            name = "GFS02",
            buildingCode = "FAST",
            buildingName = "FAST Building",
            campus = "Kihumuro Campus",
            level = BuildingLevel.GROUND,
            type = RoomType.CLASSROOM,
            aliases = listOf("GFS02"),
            sessionsThisSemester = 10,
            campusSource = "COHORT_DERIVED",
            suggestionValue = SuggestionValue.CLASS_ROOM,
            usedByFaculties = listOf("FAST", "FCI"),
            notes = "10 scheduled session(s) in 2026/2027 Sem I."
        ),
        RoomItem(
            id = "Hospital and Blood Bank",
            code = "Hospital and Blood Bank",
            name = "Hospital and Blood Bank",
            buildingCode = "CLIN",
            buildingName = "Clinical / Hospital",
            campus = "Town Campus",
            level = BuildingLevel.GROUND,
            type = RoomType.CLINICAL,
            isStudyFriendly = false,
            aliases = listOf("Hospital and Blood Bank"),
            sessionsThisSemester = 0,
            campusSource = "NAME_INFERRED",
            suggestionValue = SuggestionValue.RESTRICTED,
            notes = "No scheduled sessions - listed in Room Allocation but unused this semester. NOTE: duplicate listing in source data."
        ),
        RoomItem(
            id = "ICS LAB III",
            code = "ICS LAB III",
            name = "ICS LAB III",
            buildingCode = "IMS",
            buildingName = "IMS / Business Building",
            campus = "Town Campus",
            level = BuildingLevel.FIRST,
            type = RoomType.COMPUTER_LAB,
            aliases = listOf("ICS LAB III", "ICS LAB 3", "ICSLABIII"),
            sessionsThisSemester = 11,
            campusSource = "COHORT_DERIVED",
            suggestionValue = SuggestionValue.CLASS_ROOM,
            usedByFaculties = listOf("FBMS", "FIS", "FOM", "FoS"),
            notes = "11 scheduled session(s) in 2026/2027 Sem I."
        ),
        RoomItem(
            id = "IITR PGR",
            code = "IITR PGR",
            name = "IITR PGR",
            buildingCode = "IMS",
            buildingName = "IMS / Business Building",
            campus = "Town Campus",
            level = BuildingLevel.FIRST,
            type = RoomType.POSTGRADUATE,
            isStudyFriendly = false,
            aliases = listOf("IITR PGR", "Postgraduate Room 2", "PG Room 2", "PG2"),
            sessionsThisSemester = 15,
            campusSource = "COHORT_DERIVED",
            suggestionValue = SuggestionValue.RESTRICTED,
            usedByFaculties = listOf("FBMS", "FIS", "FOM"),
            notes = "15 scheduled session(s) in 2026/2027 Sem I."
        ),
        RoomItem(
            id = "IMS102",
            code = "IMS102",
            name = "IMS102",
            buildingCode = "IMS",
            buildingName = "IMS / Business Building",
            campus = "Town Campus",
            level = BuildingLevel.FIRST,
            type = RoomType.CLASSROOM,
            aliases = listOf("IMS102"),
            sessionsThisSemester = 17,
            campusSource = "COHORT_DERIVED",
            suggestionValue = SuggestionValue.CLASS_ROOM,
            usedByFaculties = listOf("FBMS", "FIS", "FOM"),
            notes = "17 scheduled session(s) in 2026/2027 Sem I."
        ),
        RoomItem(
            id = "IMS103",
            code = "IMS103",
            name = "IMS103",
            buildingCode = "IMS",
            buildingName = "IMS / Business Building",
            campus = "Town Campus",
            level = BuildingLevel.FIRST,
            type = RoomType.CLASSROOM,
            aliases = listOf("IMS103"),
            sessionsThisSemester = 16,
            campusSource = "COHORT_DERIVED",
            suggestionValue = SuggestionValue.CLASS_ROOM,
            usedByFaculties = listOf("FBMS", "FIS", "FOM"),
            notes = "16 scheduled session(s) in 2026/2027 Sem I."
        ),
        RoomItem(
            id = "IMSG01",
            code = "IMSG01",
            name = "IMSG01",
            buildingCode = "IMS",
            buildingName = "IMS / Business Building",
            campus = "Town Campus",
            level = BuildingLevel.GROUND,
            type = RoomType.CLASSROOM,
            aliases = listOf("IMSG01"),
            sessionsThisSemester = 16,
            campusSource = "COHORT_DERIVED",
            suggestionValue = SuggestionValue.CLASS_ROOM,
            usedByFaculties = listOf("FBMS", "FIS", "FOM"),
            notes = "16 scheduled session(s) in 2026/2027 Sem I."
        ),
        RoomItem(
            id = "IMSG02",
            code = "IMSG02",
            name = "IMSG02",
            buildingCode = "IMS",
            buildingName = "IMS / Business Building",
            campus = "Town Campus",
            level = BuildingLevel.GROUND,
            type = RoomType.CLASSROOM,
            aliases = listOf("IMSG02"),
            sessionsThisSemester = 15,
            campusSource = "COHORT_DERIVED",
            suggestionValue = SuggestionValue.CLASS_ROOM,
            usedByFaculties = listOf("FOM"),
            notes = "15 scheduled session(s) in 2026/2027 Sem I."
        ),
        RoomItem(
            id = "IMSG03",
            code = "IMSG03",
            name = "IMSG03",
            buildingCode = "IMS",
            buildingName = "IMS / Business Building",
            campus = "Town Campus",
            level = BuildingLevel.GROUND,
            type = RoomType.CLASSROOM,
            aliases = listOf("IMSG03"),
            sessionsThisSemester = 12,
            campusSource = "COHORT_DERIVED",
            suggestionValue = SuggestionValue.CLASS_ROOM,
            usedByFaculties = listOf("FBMS", "FIS", "FOM"),
            notes = "12 scheduled session(s) in 2026/2027 Sem I."
        ),
        RoomItem(
            id = "IMSG04",
            code = "IMSG04",
            name = "IMSG04",
            buildingCode = "IMS",
            buildingName = "IMS / Business Building",
            campus = "Town Campus",
            level = BuildingLevel.GROUND,
            type = RoomType.CLASSROOM,
            aliases = listOf("IMSG04"),
            sessionsThisSemester = 15,
            campusSource = "COHORT_DERIVED",
            suggestionValue = SuggestionValue.CLASS_ROOM,
            usedByFaculties = listOf("FBMS", "FIS", "FOM"),
            notes = "15 scheduled session(s) in 2026/2027 Sem I."
        ),
        RoomItem(
            id = "Kihumuro Library Room 1-LEVEL 1",
            code = "Kihumuro Library Room 1-LEVEL 1",
            name = "Kihumuro Library Room 1-LEVEL 1",
            buildingCode = "KLIB",
            buildingName = "Kihumuro Library",
            campus = "Kihumuro Campus",
            level = BuildingLevel.FIRST,
            type = RoomType.LIBRARY,
            aliases = listOf("Kihumuro Library Room 1-LEVEL 1", "Kihumuro Library 1", "Library Room 1"),
            sessionsThisSemester = 0,
            campusSource = "NAME_INFERRED",
            suggestionValue = SuggestionValue.LIBRARY,
            notes = "No scheduled sessions - listed in Room Allocation but unused this semester."
        ),
        RoomItem(
            id = "Kihumuro Library Room 2- LEVEL 1",
            code = "Kihumuro Library Room 2- LEVEL 1",
            name = "Kihumuro Library Room 2- LEVEL 1",
            buildingCode = "KLIB",
            buildingName = "Kihumuro Library",
            campus = "Kihumuro Campus",
            level = BuildingLevel.FIRST,
            type = RoomType.LIBRARY,
            aliases = listOf("Kihumuro Library Room 2- LEVEL 1", "Kihumuro Library 2", "Library Room 2"),
            sessionsThisSemester = 0,
            campusSource = "NAME_INFERRED",
            suggestionValue = SuggestionValue.LIBRARY,
            notes = "No scheduled sessions - listed in Room Allocation but unused this semester."
        ),
        RoomItem(
            id = "Kihumuro Library Room 3-LEVEL 2",
            code = "Kihumuro Library Room 3-LEVEL 2",
            name = "Kihumuro Library Room 3-LEVEL 2",
            buildingCode = "KLIB",
            buildingName = "Kihumuro Library",
            campus = "Kihumuro Campus",
            level = BuildingLevel.SECOND,
            type = RoomType.LIBRARY,
            aliases = listOf("Kihumuro Library Room 3-LEVEL 2", "Kihumuro Library 3", "Library Room 3"),
            sessionsThisSemester = 0,
            campusSource = "NAME_INFERRED",
            suggestionValue = SuggestionValue.LIBRARY,
            notes = "No scheduled sessions - listed in Room Allocation but unused this semester."
        ),
        RoomItem(
            id = "Kihumuro Library Room 4-LEVEL 2",
            code = "Kihumuro Library Room 4-LEVEL 2",
            name = "Kihumuro Library Room 4-LEVEL 2",
            buildingCode = "KLIB",
            buildingName = "Kihumuro Library",
            campus = "Kihumuro Campus",
            level = BuildingLevel.SECOND,
            type = RoomType.LIBRARY,
            aliases = listOf("Kihumuro Library Room 4-LEVEL 2", "Kihumuro Library 4", "Library Room 4"),
            sessionsThisSemester = 0,
            campusSource = "NAME_INFERRED",
            suggestionValue = SuggestionValue.LIBRARY,
            notes = "No scheduled sessions - listed in Room Allocation but unused this semester."
        ),
        RoomItem(
            id = "MCH 1",
            code = "MCH 1",
            name = "MCH 1",
            buildingCode = "PHA",
            buildingName = "Pharmacy / Health Complex",
            campus = "Town Campus",
            level = BuildingLevel.GROUND,
            type = RoomType.CLASSROOM,
            aliases = listOf("MCH 1"),
            sessionsThisSemester = 23,
            campusSource = "COHORT_DERIVED",
            suggestionValue = SuggestionValue.CLASS_ROOM,
            usedByFaculties = listOf("FBMS", "FOM"),
            notes = "23 scheduled session(s) in 2026/2027 Sem I."
        ),
        RoomItem(
            id = "MCH 2",
            code = "MCH 2",
            name = "MCH 2",
            buildingCode = "PHA",
            buildingName = "Pharmacy / Health Complex",
            campus = "Town Campus",
            level = BuildingLevel.GROUND,
            type = RoomType.CLASSROOM,
            aliases = listOf("MCH 2"),
            sessionsThisSemester = 20,
            campusSource = "COHORT_DERIVED",
            suggestionValue = SuggestionValue.CLASS_ROOM,
            usedByFaculties = listOf("FBMS", "FIS", "FOM", "FoS"),
            notes = "20 scheduled session(s) in 2026/2027 Sem I."
        ),
        RoomItem(
            id = "MCH-LR",
            code = "MCH-LR",
            name = "MCH-LR",
            buildingCode = "CLIN",
            buildingName = "Clinical / Hospital",
            campus = "Town Campus",
            level = BuildingLevel.GROUND,
            type = RoomType.CLINICAL,
            isStudyFriendly = false,
            aliases = listOf("MCH-LR"),
            sessionsThisSemester = 1,
            campusSource = "COHORT_DERIVED",
            suggestionValue = SuggestionValue.RESTRICTED,
            usedByFaculties = listOf("FOM"),
            notes = "1 scheduled session(s) in 2026/2027 Sem I."
        ),
        RoomItem(
            id = "MLS LAB",
            code = "MLS LAB",
            name = "MLS LAB",
            buildingCode = "PHA",
            buildingName = "Pharmacy / Health Complex",
            campus = "Town Campus",
            level = BuildingLevel.GROUND,
            type = RoomType.SCIENCE_LAB,
            aliases = listOf("MLS LAB"),
            sessionsThisSemester = 16,
            campusSource = "COHORT_DERIVED",
            suggestionValue = SuggestionValue.CLASS_ROOM,
            usedByFaculties = listOf("FOM"),
            notes = "16 scheduled session(s) in 2026/2027 Sem I."
        ),
        RoomItem(
            id = "MLS Offices",
            code = "MLS Offices",
            name = "MLS Offices",
            buildingCode = "PHA",
            buildingName = "Pharmacy / Health Complex",
            campus = "Town Campus",
            level = BuildingLevel.GROUND,
            type = RoomType.OFFICE,
            isStudyFriendly = false,
            aliases = listOf("MLS Offices"),
            sessionsThisSemester = 2,
            campusSource = "COHORT_DERIVED",
            suggestionValue = SuggestionValue.RESTRICTED,
            usedByFaculties = listOf("FOM"),
            notes = "2 scheduled session(s) in 2026/2027 Sem I."
        ),
        RoomItem(
            id = "MLT",
            code = "MLT",
            name = "MLT",
            buildingCode = "PHA",
            buildingName = "Pharmacy / Health Complex",
            campus = "Town Campus",
            level = BuildingLevel.GROUND,
            type = RoomType.SCIENCE_LAB,
            aliases = listOf("MLT"),
            sessionsThisSemester = 19,
            campusSource = "COHORT_DERIVED",
            suggestionValue = SuggestionValue.CLASS_ROOM,
            usedByFaculties = listOf("FBMS", "FIS", "FOM"),
            notes = "19 scheduled session(s) in 2026/2027 Sem I."
        ),
        RoomItem(
            id = "MRRH",
            code = "MRRH",
            name = "MRRH",
            buildingCode = "CLIN",
            buildingName = "Clinical / Hospital",
            campus = "Town Campus",
            level = BuildingLevel.GROUND,
            type = RoomType.CLINICAL,
            isStudyFriendly = false,
            aliases = listOf("MRRH"),
            sessionsThisSemester = 5,
            campusSource = "COHORT_DERIVED",
            suggestionValue = SuggestionValue.RESTRICTED,
            usedByFaculties = listOf("FOM"),
            notes = "5 scheduled session(s) in 2026/2027 Sem I."
        ),
        RoomItem(
            id = "Medical Ward",
            code = "Medical Ward",
            name = "Medical Ward",
            buildingCode = "CLIN",
            buildingName = "Clinical / Hospital",
            campus = "Town Campus",
            level = BuildingLevel.GROUND,
            type = RoomType.CLINICAL,
            isStudyFriendly = false,
            aliases = listOf("Medical Ward"),
            sessionsThisSemester = 5,
            campusSource = "COHORT_DERIVED",
            suggestionValue = SuggestionValue.RESTRICTED,
            usedByFaculties = listOf("FOM"),
            notes = "5 scheduled session(s) in 2026/2027 Sem I."
        ),
        RoomItem(
            id = "Nursing Lecture Room",
            code = "Nursing Lecture Room",
            name = "Nursing Lecture Room",
            buildingCode = "PHA",
            buildingName = "Pharmacy / Health Complex",
            campus = "Town Campus",
            level = BuildingLevel.GROUND,
            type = RoomType.CLASSROOM,
            aliases = listOf("Nursing Lecture Room"),
            sessionsThisSemester = 0,
            campusSource = "NAME_INFERRED",
            suggestionValue = SuggestionValue.CLASS_ROOM,
            notes = "No scheduled sessions - listed in Room Allocation but unused this semester."
        ),
        RoomItem(
            id = "Nursing Skills Lab",
            code = "Nursing Skills Lab",
            name = "Nursing Skills Lab",
            buildingCode = "PHA",
            buildingName = "Pharmacy / Health Complex",
            campus = "Town Campus",
            level = BuildingLevel.GROUND,
            type = RoomType.SCIENCE_LAB,
            aliases = listOf("Nursing Skills Lab"),
            sessionsThisSemester = 3,
            campusSource = "COHORT_DERIVED",
            suggestionValue = SuggestionValue.STUDY_SPACE,
            usedByFaculties = listOf("FOM"),
            notes = "3 scheduled session(s) in 2026/2027 Sem I."
        ),
        RoomItem(
            id = "OBG Ward",
            code = "OBG Ward",
            name = "OBG Ward",
            buildingCode = "CLIN",
            buildingName = "Clinical / Hospital",
            campus = "Town Campus",
            level = BuildingLevel.GROUND,
            type = RoomType.CLINICAL,
            isStudyFriendly = false,
            aliases = listOf("OBG Ward"),
            sessionsThisSemester = 5,
            campusSource = "COHORT_DERIVED",
            suggestionValue = SuggestionValue.RESTRICTED,
            usedByFaculties = listOf("FOM"),
            notes = "5 scheduled session(s) in 2026/2027 Sem I."
        ),
        RoomItem(
            id = "PAT LAB",
            code = "PAT LAB",
            name = "PAT LAB",
            buildingCode = "CLIN",
            buildingName = "Clinical / Hospital",
            campus = "Town Campus",
            level = BuildingLevel.GROUND,
            type = RoomType.CLINICAL,
            isStudyFriendly = false,
            aliases = listOf("PAT LAB"),
            sessionsThisSemester = 20,
            campusSource = "COHORT_DERIVED",
            suggestionValue = SuggestionValue.RESTRICTED,
            usedByFaculties = listOf("FOM"),
            notes = "20 scheduled session(s) in 2026/2027 Sem I."
        ),
        RoomItem(
            id = "PCH LAB",
            code = "PCH LAB",
            name = "PCH LAB",
            buildingCode = "CLIN",
            buildingName = "Clinical / Hospital",
            campus = "Town Campus",
            level = BuildingLevel.GROUND,
            type = RoomType.CLINICAL,
            isStudyFriendly = false,
            aliases = listOf("PCH LAB"),
            sessionsThisSemester = 5,
            campusSource = "COHORT_DERIVED",
            suggestionValue = SuggestionValue.RESTRICTED,
            usedByFaculties = listOf("FOM"),
            notes = "5 scheduled session(s) in 2026/2027 Sem I."
        ),
        RoomItem(
            id = "PG Room (Library)",
            code = "PG Room (Library)",
            name = "PG Room (Library)",
            buildingCode = "PHA",
            buildingName = "Pharmacy / Health Complex",
            campus = "Town Campus",
            level = BuildingLevel.GROUND,
            type = RoomType.POSTGRADUATE,
            isStudyFriendly = false,
            aliases = listOf("PG Room (Library)"),
            sessionsThisSemester = 0,
            campusSource = "NAME_INFERRED",
            suggestionValue = SuggestionValue.RESTRICTED,
            notes = "No scheduled sessions - listed in Room Allocation but unused this semester."
        ),
        RoomItem(
            id = "PHG LAB",
            code = "PHG LAB",
            name = "PHG LAB",
            buildingCode = "PHA",
            buildingName = "Pharmacy / Health Complex",
            campus = "Town Campus",
            level = BuildingLevel.GROUND,
            type = RoomType.SCIENCE_LAB,
            aliases = listOf("PHG LAB"),
            sessionsThisSemester = 0,
            campusSource = "NAME_INFERRED",
            suggestionValue = SuggestionValue.CLASS_ROOM,
            notes = "No scheduled sessions - listed in Room Allocation but unused this semester."
        ),
        RoomItem(
            id = "PLT",
            code = "PLT",
            name = "PLT",
            buildingCode = "PHA",
            buildingName = "Pharmacy / Health Complex",
            campus = "Town Campus",
            level = BuildingLevel.GROUND,
            type = RoomType.SCIENCE_LAB,
            aliases = listOf("PLT"),
            sessionsThisSemester = 21,
            campusSource = "COHORT_DERIVED",
            suggestionValue = SuggestionValue.CLASS_ROOM,
            usedByFaculties = listOf("FBMS", "FIS", "FOM"),
            notes = "21 scheduled session(s) in 2026/2027 Sem I."
        ),
        RoomItem(
            id = "Paediatric Ward",
            code = "Paediatric Ward",
            name = "Paediatric Ward",
            buildingCode = "CLIN",
            buildingName = "Clinical / Hospital",
            campus = "Town Campus",
            level = BuildingLevel.GROUND,
            type = RoomType.CLINICAL,
            isStudyFriendly = false,
            aliases = listOf("Paediatric Ward"),
            sessionsThisSemester = 1,
            campusSource = "COHORT_DERIVED",
            suggestionValue = SuggestionValue.RESTRICTED,
            usedByFaculties = listOf("FOM"),
            notes = "1 scheduled session(s) in 2026/2027 Sem I."
        ),
        RoomItem(
            id = "Para Lab-MLC",
            code = "Para Lab-MLC",
            name = "Para Lab-MLC",
            buildingCode = "PHA",
            buildingName = "Pharmacy / Health Complex",
            campus = "Town Campus",
            level = BuildingLevel.GROUND,
            type = RoomType.SCIENCE_LAB,
            aliases = listOf("Para Lab-MLC"),
            sessionsThisSemester = 7,
            campusSource = "COHORT_DERIVED",
            suggestionValue = SuggestionValue.CLASS_ROOM,
            usedByFaculties = listOf("FOM"),
            notes = "7 scheduled session(s) in 2026/2027 Sem I."
        ),
        RoomItem(
            id = "Pharmacy Board Room",
            code = "Pharmacy Board Room",
            name = "Pharmacy Board Room",
            buildingCode = "PHA",
            buildingName = "Pharmacy / Health Complex",
            campus = "Town Campus",
            level = BuildingLevel.GROUND,
            type = RoomType.MEETING_ROOM,
            isStudyFriendly = false,
            aliases = listOf("Pharmacy Board Room"),
            sessionsThisSemester = 0,
            campusSource = "NAME_INFERRED",
            suggestionValue = SuggestionValue.RESTRICTED,
            notes = "No scheduled sessions - listed in Room Allocation but unused this semester."
        ),
        RoomItem(
            id = "Pharmacy L1",
            code = "Pharmacy L1",
            name = "Pharmacy L1",
            buildingCode = "PHA",
            buildingName = "Pharmacy / Health Complex",
            campus = "Town Campus",
            level = BuildingLevel.GROUND,
            type = RoomType.CLASSROOM,
            aliases = listOf("Pharmacy L1"),
            sessionsThisSemester = 18,
            campusSource = "COHORT_DERIVED",
            suggestionValue = SuggestionValue.CLASS_ROOM,
            usedByFaculties = listOf("FIS", "FOM"),
            notes = "18 scheduled session(s) in 2026/2027 Sem I."
        ),
        RoomItem(
            id = "Pharmacy L2",
            code = "Pharmacy L2",
            name = "Pharmacy L2",
            buildingCode = "PHA",
            buildingName = "Pharmacy / Health Complex",
            campus = "Town Campus",
            level = BuildingLevel.FIRST,
            type = RoomType.CLASSROOM,
            aliases = listOf("Pharmacy L2"),
            sessionsThisSemester = 17,
            campusSource = "COHORT_DERIVED",
            suggestionValue = SuggestionValue.CLASS_ROOM,
            usedByFaculties = listOf("FBMS", "FIS", "FOM"),
            notes = "17 scheduled session(s) in 2026/2027 Sem I."
        ),
        RoomItem(
            id = "Pharmacy L3",
            code = "Pharmacy L3",
            name = "Pharmacy L3",
            buildingCode = "PHA",
            buildingName = "Pharmacy / Health Complex",
            campus = "Town Campus",
            level = BuildingLevel.SECOND,
            type = RoomType.CLASSROOM,
            aliases = listOf("Pharmacy L3"),
            sessionsThisSemester = 11,
            campusSource = "COHORT_DERIVED",
            suggestionValue = SuggestionValue.CLASS_ROOM,
            usedByFaculties = listOf("FIS", "FOM"),
            notes = "11 scheduled session(s) in 2026/2027 Sem I."
        ),
        RoomItem(
            id = "Postgraduate Room 1",
            code = "Postgraduate Room 1",
            name = "Postgraduate Room 1",
            buildingCode = "PHA",
            buildingName = "Pharmacy / Health Complex",
            campus = "Town Campus",
            level = BuildingLevel.THIRD,
            type = RoomType.POSTGRADUATE,
            isStudyFriendly = false,
            aliases = listOf("Postgraduate Room 1", "PG Room 1", "PG1"),
            sessionsThisSemester = 13,
            campusSource = "COHORT_DERIVED",
            suggestionValue = SuggestionValue.RESTRICTED,
            usedByFaculties = listOf("FBMS", "FIS", "FOM"),
            notes = "13 scheduled session(s) in 2026/2027 Sem I."
        ),
        RoomItem(
            id = "Psychiatric Ward",
            code = "Psychiatric Ward",
            name = "Psychiatric Ward",
            buildingCode = "CLIN",
            buildingName = "Clinical / Hospital",
            campus = "Town Campus",
            level = BuildingLevel.GROUND,
            type = RoomType.CLINICAL,
            isStudyFriendly = false,
            aliases = listOf("Psychiatric Ward"),
            sessionsThisSemester = 4,
            campusSource = "COHORT_DERIVED",
            suggestionValue = SuggestionValue.RESTRICTED,
            usedByFaculties = listOf("FOM"),
            notes = "4 scheduled session(s) in 2026/2027 Sem I."
        ),
        RoomItem(
            id = "ROOM 2",
            code = "ROOM 2",
            name = "ROOM 2",
            buildingCode = "IMS",
            buildingName = "IMS / Business Building",
            campus = "Town Campus",
            level = BuildingLevel.GROUND,
            type = RoomType.CLASSROOM,
            aliases = listOf("ROOM 2"),
            sessionsThisSemester = 23,
            campusSource = "COHORT_DERIVED",
            suggestionValue = SuggestionValue.CLASS_ROOM,
            usedByFaculties = listOf("FBMS", "FIS", "FOM"),
            notes = "23 scheduled session(s) in 2026/2027 Sem I."
        ),
        RoomItem(
            id = "ROOM 5",
            code = "ROOM 5",
            name = "ROOM 5",
            buildingCode = "IMS",
            buildingName = "IMS / Business Building",
            campus = "Town Campus",
            level = BuildingLevel.GROUND,
            type = RoomType.CLASSROOM,
            aliases = listOf("ROOM 5"),
            sessionsThisSemester = 15,
            campusSource = "COHORT_DERIVED",
            suggestionValue = SuggestionValue.CLASS_ROOM,
            usedByFaculties = listOf("FBMS", "FIS", "FOM"),
            notes = "15 scheduled session(s) in 2026/2027 Sem I."
        ),
        RoomItem(
            id = "Resource Center",
            code = "Resource Center",
            name = "Resource Center",
            buildingCode = "IMS",
            buildingName = "IMS / Business Building",
            campus = "Town Campus",
            level = BuildingLevel.FIRST,
            type = RoomType.LIBRARY,
            aliases = listOf("Resource Center", "Resource Centre"),
            sessionsThisSemester = 10,
            campusSource = "COHORT_DERIVED",
            suggestionValue = SuggestionValue.LIBRARY,
            usedByFaculties = listOf("FBMS", "FIS", "FOM"),
            notes = "10 scheduled session(s) in 2026/2027 Sem I."
        ),
        RoomItem(
            id = "S103",
            code = "S103",
            name = "S103",
            buildingCode = "SCI",
            buildingName = "Science Block",
            campus = "Town Campus",
            level = BuildingLevel.FIRST,
            type = RoomType.CLASSROOM,
            aliases = listOf("S103"),
            sessionsThisSemester = 21,
            campusSource = "COHORT_DERIVED",
            suggestionValue = SuggestionValue.CLASS_ROOM,
            usedByFaculties = listOf("FOM", "FoS"),
            notes = "21 scheduled session(s) in 2026/2027 Sem I."
        ),
        RoomItem(
            id = "S109",
            code = "S109",
            name = "S109",
            buildingCode = "SCI",
            buildingName = "Science Block",
            campus = "Town Campus",
            level = BuildingLevel.FIRST,
            type = RoomType.CLASSROOM,
            aliases = listOf("S109"),
            sessionsThisSemester = 0,
            campusSource = "NAME_INFERRED",
            suggestionValue = SuggestionValue.CLASS_ROOM,
            notes = "No scheduled sessions - listed in Room Allocation but unused this semester."
        ),
        RoomItem(
            id = "S202",
            code = "S202",
            name = "S202",
            buildingCode = "SCI",
            buildingName = "Science Block",
            campus = "Town Campus",
            level = BuildingLevel.SECOND,
            type = RoomType.CLASSROOM,
            aliases = listOf("S202"),
            sessionsThisSemester = 8,
            campusSource = "COHORT_DERIVED",
            suggestionValue = SuggestionValue.CLASS_ROOM,
            usedByFaculties = listOf("FoS"),
            notes = "8 scheduled session(s) in 2026/2027 Sem I."
        ),
        RoomItem(
            id = "S204",
            code = "S204",
            name = "S204",
            buildingCode = "SCI",
            buildingName = "Science Block",
            campus = "Town Campus",
            level = BuildingLevel.SECOND,
            type = RoomType.CLASSROOM,
            aliases = listOf("S204"),
            sessionsThisSemester = 13,
            campusSource = "COHORT_DERIVED",
            suggestionValue = SuggestionValue.CLASS_ROOM,
            usedByFaculties = listOf("FoS"),
            notes = "13 scheduled session(s) in 2026/2027 Sem I."
        ),
        RoomItem(
            id = "S205",
            code = "S205",
            name = "S205",
            buildingCode = "SCI",
            buildingName = "Science Block",
            campus = "Town Campus",
            level = BuildingLevel.SECOND,
            type = RoomType.CLASSROOM,
            aliases = listOf("S205"),
            sessionsThisSemester = 20,
            campusSource = "COHORT_DERIVED",
            suggestionValue = SuggestionValue.CLASS_ROOM,
            usedByFaculties = listOf("FOM", "FoS"),
            notes = "20 scheduled session(s) in 2026/2027 Sem I."
        ),
        RoomItem(
            id = "S210",
            code = "S210",
            name = "S210",
            buildingCode = "SCI",
            buildingName = "Science Block",
            campus = "Town Campus",
            level = BuildingLevel.SECOND,
            type = RoomType.CLASSROOM,
            aliases = listOf("S210"),
            sessionsThisSemester = 23,
            campusSource = "COHORT_DERIVED",
            suggestionValue = SuggestionValue.CLASS_ROOM,
            usedByFaculties = listOf("FBMS", "FIS", "FOM", "FoS"),
            notes = "23 scheduled session(s) in 2026/2027 Sem I."
        ),
        RoomItem(
            id = "S211",
            code = "S211",
            name = "S211",
            buildingCode = "SCI",
            buildingName = "Science Block",
            campus = "Town Campus",
            level = BuildingLevel.SECOND,
            type = RoomType.CLASSROOM,
            aliases = listOf("S211"),
            sessionsThisSemester = 23,
            campusSource = "COHORT_DERIVED",
            suggestionValue = SuggestionValue.CLASS_ROOM,
            usedByFaculties = listOf("FBMS", "FOM", "FoS"),
            notes = "23 scheduled session(s) in 2026/2027 Sem I."
        ),
        RoomItem(
            id = "S310",
            code = "S310",
            name = "S310",
            buildingCode = "SCI",
            buildingName = "Science Block",
            campus = "Town Campus",
            level = BuildingLevel.THIRD,
            type = RoomType.CLASSROOM,
            aliases = listOf("S310"),
            sessionsThisSemester = 25,
            campusSource = "COHORT_DERIVED",
            suggestionValue = SuggestionValue.CLASS_ROOM,
            usedByFaculties = listOf("FIS", "FOM", "FoS"),
            notes = "25 scheduled session(s) in 2026/2027 Sem I."
        ),
        RoomItem(
            id = "S311",
            code = "S311",
            name = "S311",
            buildingCode = "SCI",
            buildingName = "Science Block",
            campus = "Town Campus",
            level = BuildingLevel.THIRD,
            type = RoomType.CLASSROOM,
            aliases = listOf("S311"),
            sessionsThisSemester = 21,
            campusSource = "COHORT_DERIVED",
            suggestionValue = SuggestionValue.CLASS_ROOM,
            usedByFaculties = listOf("FBMS", "FOM", "FoS"),
            notes = "21 scheduled session(s) in 2026/2027 Sem I."
        ),
        RoomItem(
            id = "S403",
            code = "S403",
            name = "S403",
            buildingCode = "SCI",
            buildingName = "Science Block",
            campus = "Town Campus",
            level = BuildingLevel.FOURTH,
            type = RoomType.CLASSROOM,
            aliases = listOf("S403"),
            sessionsThisSemester = 26,
            campusSource = "COHORT_DERIVED",
            suggestionValue = SuggestionValue.CLASS_ROOM,
            usedByFaculties = listOf("FIS", "FOM", "FoS"),
            notes = "26 scheduled session(s) in 2026/2027 Sem I."
        ),
        RoomItem(
            id = "SFL01 (Computer Lab)",
            code = "SFL01 (Computer Lab)",
            name = "SFL01 (Computer Lab)",
            buildingCode = "FAST",
            buildingName = "FAST Building",
            campus = "Kihumuro Campus",
            level = BuildingLevel.SECOND,
            type = RoomType.COMPUTER_LAB,
            aliases = listOf("SFL01 (Computer Lab)", "SFL01", "SFL 01"),
            sessionsThisSemester = 11,
            campusSource = "COHORT_DERIVED",
            suggestionValue = SuggestionValue.CLASS_ROOM,
            usedByFaculties = listOf("FAST", "FCI"),
            notes = "11 scheduled session(s) in 2026/2027 Sem I."
        ),
        RoomItem(
            id = "SFL02",
            code = "SFL02",
            name = "SFL02",
            buildingCode = "FAST",
            buildingName = "FAST Building",
            campus = "Kihumuro Campus",
            level = BuildingLevel.SECOND,
            type = RoomType.CLASSROOM,
            aliases = listOf("SFL02"),
            sessionsThisSemester = 13,
            campusSource = "COHORT_DERIVED",
            suggestionValue = SuggestionValue.CLASS_ROOM,
            usedByFaculties = listOf("FAST", "FCI"),
            notes = "13 scheduled session(s) in 2026/2027 Sem I."
        ),
        RoomItem(
            id = "SFS01",
            code = "SFS01",
            name = "SFS01",
            buildingCode = "FAST",
            buildingName = "FAST Building",
            campus = "Kihumuro Campus",
            level = BuildingLevel.SECOND,
            type = RoomType.CLASSROOM,
            aliases = listOf("SFS01"),
            sessionsThisSemester = 13,
            campusSource = "COHORT_DERIVED",
            suggestionValue = SuggestionValue.CLASS_ROOM,
            usedByFaculties = listOf("FAST"),
            notes = "13 scheduled session(s) in 2026/2027 Sem I."
        ),
        RoomItem(
            id = "SFS02",
            code = "SFS02",
            name = "SFS02",
            buildingCode = "FAST",
            buildingName = "FAST Building",
            campus = "Kihumuro Campus",
            level = BuildingLevel.SECOND,
            type = RoomType.CLASSROOM,
            aliases = listOf("SFS02"),
            sessionsThisSemester = 12,
            campusSource = "COHORT_DERIVED",
            suggestionValue = SuggestionValue.CLASS_ROOM,
            usedByFaculties = listOf("FAST"),
            notes = "12 scheduled session(s) in 2026/2027 Sem I."
        ),
        RoomItem(
            id = "SG05",
            code = "SG05",
            name = "SG05",
            buildingCode = "SCI",
            buildingName = "Science Block",
            campus = "Town Campus",
            level = BuildingLevel.GROUND,
            type = RoomType.CLASSROOM,
            aliases = listOf("SG05"),
            sessionsThisSemester = 14,
            campusSource = "COHORT_DERIVED",
            suggestionValue = SuggestionValue.CLASS_ROOM,
            usedByFaculties = listOf("FBMS", "FoS"),
            notes = "14 scheduled session(s) in 2026/2027 Sem I."
        ),
        RoomItem(
            id = "SG06",
            code = "SG06",
            name = "SG06",
            buildingCode = "SCI",
            buildingName = "Science Block",
            campus = "Town Campus",
            level = BuildingLevel.GROUND,
            type = RoomType.CLASSROOM,
            aliases = listOf("SG06"),
            sessionsThisSemester = 13,
            campusSource = "COHORT_DERIVED",
            suggestionValue = SuggestionValue.CLASS_ROOM,
            usedByFaculties = listOf("FOM", "FoS"),
            notes = "13 scheduled session(s) in 2026/2027 Sem I."
        ),
        RoomItem(
            id = "Senior Room",
            code = "Senior Room",
            name = "Senior Room",
            buildingCode = "PHA",
            buildingName = "Pharmacy / Health Complex",
            campus = "Town Campus",
            level = BuildingLevel.GROUND,
            type = RoomType.OFFICE,
            isStudyFriendly = false,
            aliases = listOf("Senior Room"),
            sessionsThisSemester = 0,
            campusSource = "NAME_INFERRED",
            suggestionValue = SuggestionValue.RESTRICTED,
            notes = "No scheduled sessions - listed in Room Allocation but unused this semester."
        ),
        RoomItem(
            id = "Simulation LAB",
            code = "Simulation LAB",
            name = "Simulation LAB",
            buildingCode = "CLIN",
            buildingName = "Clinical / Hospital",
            campus = "Town Campus",
            level = BuildingLevel.GROUND,
            type = RoomType.CLINICAL,
            isStudyFriendly = false,
            aliases = listOf("Simulation LAB"),
            sessionsThisSemester = 2,
            campusSource = "COHORT_DERIVED",
            suggestionValue = SuggestionValue.RESTRICTED,
            usedByFaculties = listOf("FOM"),
            notes = "2 scheduled session(s) in 2026/2027 Sem I."
        ),
        RoomItem(
            id = "Surgical Ward",
            code = "Surgical Ward",
            name = "Surgical Ward",
            buildingCode = "CLIN",
            buildingName = "Clinical / Hospital",
            campus = "Town Campus",
            level = BuildingLevel.GROUND,
            type = RoomType.CLINICAL,
            isStudyFriendly = false,
            aliases = listOf("Surgical Ward"),
            sessionsThisSemester = 2,
            campusSource = "COHORT_DERIVED",
            suggestionValue = SuggestionValue.RESTRICTED,
            usedByFaculties = listOf("FOM"),
            notes = "2 scheduled session(s) in 2026/2027 Sem I."
        ),
        RoomItem(
            id = "TFL01",
            code = "TFL01",
            name = "TFL01",
            buildingCode = "FAST",
            buildingName = "FAST Building",
            campus = "Kihumuro Campus",
            level = BuildingLevel.THIRD,
            type = RoomType.CLASSROOM,
            aliases = listOf("TFL01"),
            sessionsThisSemester = 15,
            campusSource = "COHORT_DERIVED",
            suggestionValue = SuggestionValue.CLASS_ROOM,
            usedByFaculties = listOf("FAST", "FCI"),
            notes = "15 scheduled session(s) in 2026/2027 Sem I."
        ),
        RoomItem(
            id = "TFL02",
            code = "TFL02",
            name = "TFL02",
            buildingCode = "FAST",
            buildingName = "FAST Building",
            campus = "Kihumuro Campus",
            level = BuildingLevel.THIRD,
            type = RoomType.CLASSROOM,
            aliases = listOf("TFL02"),
            sessionsThisSemester = 15,
            campusSource = "COHORT_DERIVED",
            suggestionValue = SuggestionValue.CLASS_ROOM,
            usedByFaculties = listOf("FAST", "FCI"),
            notes = "15 scheduled session(s) in 2026/2027 Sem I."
        ),
        RoomItem(
            id = "TFS01",
            code = "TFS01",
            name = "TFS01",
            buildingCode = "FAST",
            buildingName = "FAST Building",
            campus = "Kihumuro Campus",
            level = BuildingLevel.THIRD,
            type = RoomType.CLASSROOM,
            aliases = listOf("TFS01"),
            sessionsThisSemester = 12,
            campusSource = "COHORT_DERIVED",
            suggestionValue = SuggestionValue.CLASS_ROOM,
            usedByFaculties = listOf("FAST", "FCI"),
            notes = "12 scheduled session(s) in 2026/2027 Sem I."
        ),
        RoomItem(
            id = "TFS02",
            code = "TFS02",
            name = "TFS02",
            buildingCode = "FAST",
            buildingName = "FAST Building",
            campus = "Kihumuro Campus",
            level = BuildingLevel.THIRD,
            type = RoomType.CLASSROOM,
            aliases = listOf("TFS02"),
            sessionsThisSemester = 13,
            campusSource = "COHORT_DERIVED",
            suggestionValue = SuggestionValue.CLASS_ROOM,
            usedByFaculties = listOf("FAST"),
            notes = "13 scheduled session(s) in 2026/2027 Sem I."
        ),
        RoomItem(
            id = "TV ROOM",
            code = "TV ROOM",
            name = "TV ROOM",
            buildingCode = "IMS",
            buildingName = "IMS / Business Building",
            campus = "Town Campus",
            level = BuildingLevel.FIRST,
            type = RoomType.MEETING_ROOM,
            isStudyFriendly = false,
            aliases = listOf("TV ROOM"),
            sessionsThisSemester = 13,
            campusSource = "COHORT_DERIVED",
            suggestionValue = SuggestionValue.RESTRICTED,
            usedByFaculties = listOf("FBMS", "FIS", "FOM"),
            notes = "13 scheduled session(s) in 2026/2027 Sem I."
        ),
        RoomItem(
            id = "Wards",
            code = "Wards",
            name = "Wards",
            buildingCode = "CLIN",
            buildingName = "Clinical / Hospital",
            campus = "Town Campus",
            level = BuildingLevel.GROUND,
            type = RoomType.CLINICAL,
            isStudyFriendly = false,
            aliases = listOf("Wards"),
            sessionsThisSemester = 6,
            campusSource = "COHORT_DERIVED",
            suggestionValue = SuggestionValue.RESTRICTED,
            usedByFaculties = listOf("FOM"),
            notes = "6 scheduled session(s) in 2026/2027 Sem I."
        ),
        RoomItem(
            id = "Wards-2",
            code = "Wards-2",
            name = "Wards-2",
            buildingCode = "CLIN",
            buildingName = "Clinical / Hospital",
            campus = "Town Campus",
            level = BuildingLevel.GROUND,
            type = RoomType.CLINICAL,
            isStudyFriendly = false,
            aliases = listOf("Wards-2"),
            sessionsThisSemester = 2,
            campusSource = "COHORT_DERIVED",
            suggestionValue = SuggestionValue.RESTRICTED,
            usedByFaculties = listOf("FOM"),
            notes = "2 scheduled session(s) in 2026/2027 Sem I."
        ),
        RoomItem(
            id = "FCI Library 1",
            code = "FCI Library 1",
            name = "FCI Library 1",
            buildingCode = "FCI",
            buildingName = "FCI Building",
            campus = "Kihumuro Campus",
            level = BuildingLevel.FIRST,
            type = RoomType.LIBRARY,
            aliases = listOf("FCI Library 1"),
            sessionsThisSemester = 0,
            campusSource = "DIRECTORY_ONLY",
            suggestionValue = SuggestionValue.LIBRARY,
            notes = "From FCI directory sign; not listed in Room Allocation."
        ),
        RoomItem(
            id = "FCI Library 2",
            code = "FCI Library 2",
            name = "FCI Library 2",
            buildingCode = "FCI",
            buildingName = "FCI Building",
            campus = "Kihumuro Campus",
            level = BuildingLevel.SECOND,
            type = RoomType.LIBRARY,
            aliases = listOf("FCI Library 2"),
            sessionsThisSemester = 0,
            campusSource = "DIRECTORY_ONLY",
            suggestionValue = SuggestionValue.LIBRARY,
            notes = "From FCI directory sign; not listed in Room Allocation."
        )
    )

    /** Legacy alias kept for backward compatibility. */
    val FCI_ROOMS: List<RoomItem> = ALL_ROOMS.filter { it.buildingCode == "FCI" }

    fun roomsForCampus(campus: String?): List<RoomItem> =
        if (campus.isNullOrBlank()) ALL_ROOMS
        else ALL_ROOMS.filter { it.campus.equals(campus, ignoreCase = true) }

    fun roomsForBuilding(buildingCode: String?): List<RoomItem> =
        if (buildingCode.isNullOrBlank()) ALL_ROOMS
        else ALL_ROOMS.filter { it.buildingCode.equals(buildingCode, ignoreCase = true) }

    fun buildingsForCampus(campus: String?): List<Building> =
        if (campus.isNullOrBlank()) BUILDINGS
        else BUILDINGS.filter { it.campus.equals(campus, ignoreCase = true) }

    /** Rooms with no scheduled sessions this semester — always available. */
    val PERMANENTLY_VACANT_ROOMS: List<RoomItem> = ALL_ROOMS.filter { it.isPermanentlyVacant }

    /**
     * Ranks free rooms as study suggestions.
     *
     * Priority order:
     *  1. Class rooms / labs first — their availability is the information the
     *     student does not already have. Libraries rank last because everyone
     *     already assumes the library is free.
     *  2. Then by how long the room stays free (longer window = more useful)
     *  3. Then by room code for stable ordering
     *
     * Occupied rooms, non-study-friendly rooms, and libraries-only queries are
     * handled by the caller; libraries are included but deprioritised.
     */
    fun suggestStudyRooms(
        statuses: List<RoomVacancyStatus>,
        minGapMinutes: Int = 30
    ): List<RoomVacancyStatus> =
        statuses
            .filter { !it.isOccupied && it.room.isStudyFriendly }
            .filter { it.room.isPermanentlyVacant || it.gapMinutes >= minGapMinutes || it.freeUntil == "Rest of day" }
            .sortedWith(
                compareBy(
                    { it.room.suggestionValue.suggestionRank },
                    { -it.gapMinutes },
                    { it.room.code }
                )
            )

    /**
     * Calculates the vacancy status of every room given all timetable entries across the
     * university, target day of the week, and query time in "HH:mm" (24-hour).
     *
     * @param campusFilter   null/blank = all campuses; else "Kihumuro Campus" / "Town Campus"
     * @param buildingFilter null/blank = all buildings in scope; else building code e.g. "FCI"
     * @param includePermanentlyVacant include rooms with zero scheduled sessions (default true)
     */
    fun calculateRoomVacancy(
        allEntries: List<TimetableEntry>,
        dayOfWeek: String,
        queryTimeStr: String,
        minGapMinutes: Int = 30,
        customEvents: List<CustomEvent> = emptyList(),
        campusFilter: String? = null,
        buildingFilter: String? = null,
        includePermanentlyVacant: Boolean = true
    ): List<RoomVacancyStatus> {
        val queryMinutes = TimeUtil.toMinutes(queryTimeStr)
        val endOfDayMinutes = 21 * 60 // 21:00

        val syntheticCustomEntries = customEvents.filter {
            it.dayOfWeek.equals(dayOfWeek, ignoreCase = true) && it.location.isNotBlank()
        }.map { event ->
            TimetableEntry(
                natural_key = "custom_event_${event.id}",
                program_group = "Custom Booking",
                day = event.dayOfWeek,
                time_slot = "${event.startTime} - ${event.endTime}",
                start_time = event.startTime,
                end_time = event.endTime,
                course_code = event.category,
                course_title = event.title,
                session_type = "Custom Event",
                lecturer = null,
                room = event.location
            )
        }

        val combinedEntries = allEntries + syntheticCustomEntries

        val scoped = if (!buildingFilter.isNullOrBlank()) roomsForBuilding(buildingFilter)
                     else roomsForCampus(campusFilter)

        return scoped
            .filter { includePermanentlyVacant || !it.isPermanentlyVacant }
            .map { room ->
            val roomDayEntries = combinedEntries.filter { entry ->
                entry.dayOfWeek.equals(dayOfWeek, ignoreCase = true) &&
                        room.matchesRoomString(entry.room)
            }.sortedBy { TimeUtil.toMinutes(it.startTime) }

            val activeSession = roomDayEntries.firstOrNull { entry ->
                val startM = TimeUtil.toMinutes(entry.startTime)
                val endM = if (!entry.endTime.isNullOrBlank()) TimeUtil.toMinutes(entry.endTime) else (startM + 60)
                queryMinutes in startM until endM
            }

            if (activeSession != null) {
                val occupiedEnd = activeSession.endTime ?: queryTimeStr
                val nextSessionAfterThis = roomDayEntries.firstOrNull { entry ->
                    TimeUtil.toMinutes(entry.startTime) >= TimeUtil.toMinutes(occupiedEnd)
                }
                RoomVacancyStatus(
                    room = room, isOccupied = true, currentSession = activeSession,
                    occupiedUntil = occupiedEnd, nextSession = nextSessionAfterThis,
                    freeUntil = null, gapMinutes = 0, allDaySessions = roomDayEntries
                )
            } else {
                val nextSession = roomDayEntries.firstOrNull { entry ->
                    TimeUtil.toMinutes(entry.startTime) > queryMinutes
                }
                val gapMinutes: Int
                val freeUntilStr: String
                if (nextSession != null) {
                    val nextStartM = TimeUtil.toMinutes(nextSession.startTime)
                    gapMinutes = (nextStartM - queryMinutes).coerceAtLeast(0)
                    freeUntilStr = nextSession.startTime
                } else {
                    gapMinutes = (endOfDayMinutes - queryMinutes).coerceAtLeast(60)
                    freeUntilStr = "Rest of day"
                }
                RoomVacancyStatus(
                    room = room, isOccupied = false, currentSession = null,
                    occupiedUntil = null, nextSession = nextSession,
                    freeUntil = freeUntilStr, gapMinutes = gapMinutes,
                    allDaySessions = roomDayEntries
                )
            }
        }
    }
}
