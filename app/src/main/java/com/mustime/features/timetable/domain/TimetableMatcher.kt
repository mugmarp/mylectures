package com.mustime.features.timetable.domain

object TimetableMatcher {
    val CODE_ALIASES = mapOf(
        "MLC" to "MLS",
        "BNC" to "BNS",
        "BSPC" to "BSP",
        "PEM" to "PEEM",
        "CIV" to "CVE",
        "BAF" to "BSAF"
    )

    val OFFICIAL_CODES = setOf(
        "MBR", "PHA", "BNS", "MLS", "BSP", "PHS", "DCM", "DEM", "DCAM",
        "BS", "DLT", "BME", "EEE", "PEEM", "CVE", "MIE", "BCS", "BIT",
        "BSE", "BBA", "BSAF", "ECO", "BPSM", "BSAL", "BGWH", "BPCD"
    )

    fun parseGroup(raw: String): String? {
        val clean = raw.trim()
        if (clean.isEmpty()) return null
        val parts = clean.split(Regex("[\\s-]+")).filter { it.isNotEmpty() }
        val romanYears = setOf("I", "II", "III", "IV", "V", "VI")
        val yearIdx = parts.indexOfFirst { it.uppercase() in romanYears }
        if (yearIdx <= 0) return null
        val rawCode = parts[0].uppercase().replace(Regex("[^A-Z]"), "")
        val code = CODE_ALIASES[rawCode] ?: rawCode
        if (code !in OFFICIAL_CODES) return null
        val year = parts[yearIdx].uppercase()
        val subject = (parts.subList(1, yearIdx) + parts.subList(yearIdx + 1, parts.size))
            .joinToString(" ").uppercase().trim()
        return listOf(code, subject, year).filter { it.isNotEmpty() }.joinToString(" ")
    }

    fun entryMatchesGroup(entry: TimetableEntry, selectedGroup: String): Boolean {
        val target = parseGroup(selectedGroup)
        if (target != null) {
            val entryTarget = parseGroup(entry.program_group)
            if (entryTarget == target) return true
            if (entry.shared_with.any { parseGroup(it) == target }) return true
        }
        val cleanSel = selectedGroup.trim().uppercase()
        if (entry.program_group.trim().uppercase() == cleanSel) return true
        if (entry.shared_with.any { it.trim().uppercase() == cleanSel }) return true
        return false
    }

    fun deduplicateEntries(entries: List<TimetableEntry>, targetGroup: String = ""): List<TimetableEntry> {
        if (entries.size <= 1) return entries
        val targetNorm = targetGroup.trim().uppercase()
        val result = LinkedHashMap<String, TimetableEntry>()

        for (entry in entries) {
            val codeKey = entry.course_code.trim().uppercase()
            val dayKey = entry.day.trim().take(3).uppercase()
            val startKey = entry.start_time.trim()
            val endKey = (entry.end_time ?: "").trim()
            val sessionKey = "$codeKey|$dayKey|$startKey|$endKey"

            val existing = result[sessionKey]
            if (existing == null) {
                result[sessionKey] = entry
            } else {
                // If this new entry matches the student's target group directly, prefer it
                val newMatchesDirectly = targetNorm.isNotEmpty() && entry.program_group.trim().uppercase().contains(targetNorm)
                val existingMatchesDirectly = targetNorm.isNotEmpty() && existing.program_group.trim().uppercase().contains(targetNorm)
                val primary = if (newMatchesDirectly && !existingMatchesDirectly) entry else existing
                val secondary = if (primary == entry) existing else entry

                // Merge non-blank rooms (omitting generic placeholders)
                val mergedRoom = when {
                    !primary.room.isNullOrBlank() && primary.room.trim().uppercase() !in listOf("TBA", "TBD", "NONE", "N/A") -> primary.room.trim()
                    !secondary.room.isNullOrBlank() && secondary.room.trim().uppercase() !in listOf("TBA", "TBD", "NONE", "N/A") -> secondary.room.trim()
                    else -> null
                }

                // Merge non-blank lecturers
                val mergedLecturer = when {
                    !primary.lecturer.isNullOrBlank() && !primary.lecturer.equals("Staff", ignoreCase = true) -> primary.lecturer.trim()
                    !secondary.lecturer.isNullOrBlank() && !secondary.lecturer.equals("Staff", ignoreCase = true) -> secondary.lecturer.trim()
                    else -> primary.lecturer
                }

                // Combine all shared groups without duplicates
                val combinedShared = (primary.shared_with + secondary.shared_with + listOf(primary.program_group, secondary.program_group))
                    .map { it.trim() }
                    .filter { it.isNotBlank() && !it.equals(primary.program_group.trim(), ignoreCase = true) }
                    .distinct()

                result[sessionKey] = primary.copy(
                    room = mergedRoom,
                    lecturer = mergedLecturer,
                    shared_with = combinedShared
                )
            }
        }
        return result.values.toList()
    }

    fun filterTimetable(entries: List<TimetableEntry>, group: String, day: String? = null): List<TimetableEntry> {
        val matched = entries.filter { entry ->
            (day == null ||
             entry.day.equals(day, ignoreCase = true) ||
             entry.day.startsWith(day.take(3), ignoreCase = true) ||
             day.startsWith(entry.day.take(3), ignoreCase = true)) &&
            entryMatchesGroup(entry, group)
        }
        return deduplicateEntries(matched, group).sortedBy { it.start_time }
    }
}
