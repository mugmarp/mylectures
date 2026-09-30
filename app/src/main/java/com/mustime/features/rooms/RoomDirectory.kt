package com.mustime.features.rooms

import com.mustime.core.util.TimeUtil
import com.mustime.features.timetable.domain.CustomEvent
import com.mustime.features.timetable.domain.TimetableEntry

enum class RoomType(val displayName: String, val isStudyFriendlyDefault: Boolean) {
    CLASSROOM("Classroom", true),
    COMPUTER_LAB("Computer Lab", true),
    LIBRARY("Library / Study Area", true),
    MEETING_ROOM("Meeting Room / Board Room", false),
    POSTGRADUATE("Postgraduate Room", false),
    OFFICE("Staff Office / Lounge", false)
}

enum class BuildingLevel(val displayName: String, val shortName: String) {
    GROUND("Ground Floor", "Ground"),
    FIRST("First Floor", "1st Floor"),
    SECOND("Second Floor", "2nd Floor"),
    THIRD("Third Floor", "3rd Floor")
}

data class RoomItem(
    val id: String,
    val code: String,
    val name: String,
    val buildingCode: String = "FCI",
    val buildingName: String = "FCI Building",
    val campus: String = "Kihumuro Campus",
    val level: BuildingLevel,
    val type: RoomType,
    val isStudyFriendly: Boolean = type.isStudyFriendlyDefault,
    val aliases: List<String> = emptyList(),
    val notes: String = ""
) {
    fun matchesRoomString(queryRoom: String?): Boolean {
        if (queryRoom.isNullOrBlank()) return false
        val clean = queryRoom.trim()
        val cleanCode = code.trim()
        val cleanName = name.trim()

        if (clean.equals(cleanCode, ignoreCase = true) || clean.equals(cleanName, ignoreCase = true)) return true
        if (aliases.any { it.trim().equals(clean, ignoreCase = true) }) return true

        // Normalized matching: ignore dashes, redundant spaces, and case
        val cleanNorm = clean.lowercase().replace("-", " ").replace(Regex("\\s+"), " ")
        val codeNorm = cleanCode.lowercase().replace("-", " ").replace(Regex("\\s+"), " ")
        if (cleanNorm == codeNorm) return true

        for (alias in aliases) {
            val aliasNorm = alias.lowercase().replace("-", " ").replace(Regex("\\s+"), " ")
            if (cleanNorm == aliasNorm) return true

            // Handle compound rooms (e.g. "FCI L1 / FCI L2" or "FCI LAB 3, FCI LAB 4")
            val parts = cleanNorm.split("/", ",", ";", "&").map { it.trim() }
            if (parts.any { it == aliasNorm || it == codeNorm }) return true
        }

        return false
    }
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
        get() {
            return if (isOccupied) {
                if (!occupiedUntil.isNullOrBlank()) "Occupied until $occupiedUntil" else "Currently Occupied"
            } else {
                if (freeUntil == "Rest of day" || nextSession == null) {
                    "Free for rest of day"
                } else {
                    "Free until $freeUntil (${formatMinutes(gapMinutes)})"
                }
            }
        }

    private fun formatMinutes(minutes: Int): String {
        if (minutes < 60) return "${minutes}m"
        val h = minutes / 60
        val m = minutes % 60
        return if (m == 0) "${h}h" else "${h}h ${m}m"
    }
}

object UniversityDirectory {

    val FCI_ROOMS = listOf(
        // Ground Floor
        RoomItem(
            id = "FCI-LR1",
            code = "FCI-LR1",
            name = "Lecture Room 1",
            level = BuildingLevel.GROUND,
            type = RoomType.CLASSROOM,
            aliases = listOf("FCI L1", "FCI-LR1", "Lecture Room 1", "LR1", "FCI LR1", "FCI LR 1", "ROOM 1")
        ),
        RoomItem(
            id = "FCI-LR2",
            code = "FCI-LR2",
            name = "Lecture Room 2",
            level = BuildingLevel.GROUND,
            type = RoomType.CLASSROOM,
            aliases = listOf("FCI L2", "FCI-LR2", "Lecture Room 2", "LR2", "FCI LR2", "FCI LR 2", "ROOM 2")
        ),
        RoomItem(
            id = "FCI-CR1",
            code = "FCI-CR1",
            name = "Computer Room 1",
            level = BuildingLevel.GROUND,
            type = RoomType.COMPUTER_LAB,
            aliases = listOf("Computer Room 1", "CR1", "FCI CR1", "FCI LAB 1", "LAB 1")
        ),
        RoomItem(
            id = "FCI-CR2",
            code = "FCI-CR2",
            name = "Computer Room 2",
            level = BuildingLevel.GROUND,
            type = RoomType.COMPUTER_LAB,
            aliases = listOf("Computer Room 2", "CR2", "FCI CR2", "FCI LAB 2", "LAB 2")
        ),

        // First Floor
        RoomItem(
            id = "FCI-LR3",
            code = "FCI-LR3",
            name = "Lecture Room 3",
            level = BuildingLevel.FIRST,
            type = RoomType.CLASSROOM,
            aliases = listOf("FCI L3", "FCI-LR3", "Lecture Room 3", "LR3", "FCI LR3")
        ),
        RoomItem(
            id = "FCI-LR4",
            code = "FCI-LR4",
            name = "Lecture Room 4",
            level = BuildingLevel.FIRST,
            type = RoomType.CLASSROOM,
            aliases = listOf("FCI L4", "FCI-LR4", "Lecture Room 4", "LR4", "FCI LR4")
        ),
        RoomItem(
            id = "FCI-CR3",
            code = "FCI-CR3",
            name = "Computer Room 3",
            level = BuildingLevel.FIRST,
            type = RoomType.COMPUTER_LAB,
            aliases = listOf("Computer Room 3", "CR3", "FCI CR3", "FCI LAB 3", "ICS LAB III", "LAB 3")
        ),
        RoomItem(
            id = "FCI-CR4",
            code = "FCI-CR4",
            name = "Computer Room 4",
            level = BuildingLevel.FIRST,
            type = RoomType.COMPUTER_LAB,
            aliases = listOf("Computer Room 4", "CR4", "FCI CR4", "FCI LAB 4", "DS LAB IV", "LAB 4")
        ),
        RoomItem(
            id = "FCI-LIB1",
            code = "FCI-LIB1",
            name = "Library 1",
            level = BuildingLevel.FIRST,
            type = RoomType.LIBRARY,
            aliases = listOf("Library 1", "FCI Library 1", "Resource Center", "LIB 1")
        ),

        // Second Floor
        RoomItem(
            id = "FCI-LR5",
            code = "FCI-LR5",
            name = "Lecture Room 5",
            level = BuildingLevel.SECOND,
            type = RoomType.CLASSROOM,
            aliases = listOf("FCI L5", "FCI-LR5", "Lecture Room 5", "LR5", "FCI LR5", "ROOM 5")
        ),
        RoomItem(
            id = "FCI-LR6",
            code = "FCI-LR6",
            name = "Lecture Room 6",
            level = BuildingLevel.SECOND,
            type = RoomType.CLASSROOM,
            aliases = listOf("FCI L6", "FCI-LR6", "Lecture Room 6", "LR6", "FCI LR6")
        ),
        RoomItem(
            id = "FCI-CR5",
            code = "FCI-CR5",
            name = "Computer Room 5",
            level = BuildingLevel.SECOND,
            type = RoomType.COMPUTER_LAB,
            aliases = listOf("Computer Room 5", "CR5", "FCI CR5", "FCI LAB 5", "LAB 5")
        ),
        RoomItem(
            id = "FCI-CR6",
            code = "FCI-CR6",
            name = "Computer Room 6",
            level = BuildingLevel.SECOND,
            type = RoomType.COMPUTER_LAB,
            aliases = listOf("Computer Room 6", "CR6", "FCI CR6", "FCI LAB 6", "LAB 6")
        ),
        RoomItem(
            id = "FCI-LIB2",
            code = "FCI-LIB2",
            name = "Library 2",
            level = BuildingLevel.SECOND,
            type = RoomType.LIBRARY,
            aliases = listOf("Library 2", "FCI Library 2", "LIB 2")
        ),

        // Third Floor (Administrative / Postgraduate - NOT study friendly for general students)
        RoomItem(
            id = "FCI-BR",
            code = "FCI-BR",
            name = "Board Room",
            level = BuildingLevel.THIRD,
            type = RoomType.MEETING_ROOM,
            isStudyFriendly = false,
            aliases = listOf("Board Room", "FCI Board Room", "BR"),
            notes = "Administrative meetings only - not open for individual or group study."
        ),
        RoomItem(
            id = "FCI-PG1",
            code = "FCI-PG1",
            name = "Postgraduate Room 1",
            level = BuildingLevel.THIRD,
            type = RoomType.POSTGRADUATE,
            isStudyFriendly = false,
            aliases = listOf("Postgraduate Room 1", "PG Room 1", "PG1", "IITR PGR"),
            notes = "Designated for Postgraduate researchers and scholars."
        ),
        RoomItem(
            id = "FCI-PG2",
            code = "FCI-PG2",
            name = "Postgraduate Room 2",
            level = BuildingLevel.THIRD,
            type = RoomType.POSTGRADUATE,
            isStudyFriendly = false,
            aliases = listOf("Postgraduate Room 2", "PG Room 2", "PG2"),
            notes = "Designated for Postgraduate researchers and scholars."
        ),
        RoomItem(
            id = "FCI-EXEC",
            code = "FCI-EXEC",
            name = "Executive Director & Dean",
            level = BuildingLevel.THIRD,
            type = RoomType.OFFICE,
            isStudyFriendly = false,
            aliases = listOf("Executive Director", "Dean Office", "FCI Dean"),
            notes = "Official faculty executive office."
        ),
        RoomItem(
            id = "FCI-STAFF",
            code = "FCI-STAFF",
            name = "Staff Lounge & Open Plan Offices",
            level = BuildingLevel.THIRD,
            type = RoomType.OFFICE,
            isStudyFriendly = false,
            aliases = listOf("Staff Lounge", "Open Plan Offices", "FCI Staff"),
            notes = "Faculty lecturers and departmental staff workstations."
        )
    )

    /**
     * Calculates the vacancy status of every room given all timetable entries across the university,
     * target day of the week, and query time in "HH:mm" (24-hour).
     */
    fun calculateRoomVacancy(
        allEntries: List<TimetableEntry>,
        dayOfWeek: String,
        queryTimeStr: String,
        minGapMinutes: Int = 30,
        customEvents: List<CustomEvent> = emptyList()
    ): List<RoomVacancyStatus> {
        val queryMinutes = TimeUtil.toMinutes(queryTimeStr)
        val endOfDayMinutes = 21 * 60 // 21:00 (9:00 PM)

        // Convert any custom events with rooms into TimetableEntry format for vacancy evaluation
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

        return FCI_ROOMS.map { room ->
            // Find all entries for this room on the given day
            val roomDayEntries = combinedEntries.filter { entry ->
                entry.dayOfWeek.equals(dayOfWeek, ignoreCase = true) &&
                        room.matchesRoomString(entry.room)
            }.sortedBy { TimeUtil.toMinutes(it.startTime) }

            // 1. Is room occupied right now?
            val activeSession = roomDayEntries.firstOrNull { entry ->
                val startM = TimeUtil.toMinutes(entry.startTime)
                val endM = if (!entry.endTime.isNullOrBlank()) TimeUtil.toMinutes(entry.endTime) else (startM + 60)
                queryMinutes in startM until endM
            }

            if (activeSession != null) {
                // Room is occupied
                val occupiedEnd = activeSession.endTime ?: queryTimeStr
                // Find next session after active session
                val nextSessionAfterThis = roomDayEntries.firstOrNull { entry ->
                    TimeUtil.toMinutes(entry.startTime) >= TimeUtil.toMinutes(occupiedEnd)
                }

                RoomVacancyStatus(
                    room = room,
                    isOccupied = true,
                    currentSession = activeSession,
                    occupiedUntil = occupiedEnd,
                    nextSession = nextSessionAfterThis,
                    freeUntil = null,
                    gapMinutes = 0,
                    allDaySessions = roomDayEntries
                )
            } else {
                // Room is currently free!
                // 2. Next-lecture lookup (for "Free until X" display)
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
                    // Free for rest of the academic day
                    gapMinutes = (endOfDayMinutes - queryMinutes).coerceAtLeast(60)
                    freeUntilStr = "Rest of day"
                }

                RoomVacancyStatus(
                    room = room,
                    isOccupied = false,
                    currentSession = null,
                    occupiedUntil = null,
                    nextSession = nextSession,
                    freeUntil = freeUntilStr,
                    gapMinutes = gapMinutes,
                    allDaySessions = roomDayEntries
                )
            }
        }
    }
}
