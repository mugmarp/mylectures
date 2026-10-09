package com.mustime.features.timetable.domain

data class ParsedGroup(
    val code: String,
    val subject: String,
    val year: String
)

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

    val PRECOMPUTED_GROUP_COUNTS: Map<String, Int> = mapOf(
        "MBR I" to 17, "MBR II" to 12, "MBR III" to 11, "MBR IV" to 19, "MBR V" to 11,
        "PHA I" to 16, "PHA II" to 14, "PHA III" to 16, "PHA IV" to 14,
        "BNS I" to 25, "BNS II" to 31, "BNS III" to 34, "BNS IV" to 17,
        "MLS I" to 16, "MLS II" to 32, "MLS III" to 35, "MLS IV" to 14,
        "BSP I" to 23, "BSP II" to 28, "BSP III" to 10, "BSP IV" to 11,
        "PHS I" to 15, "PHS II" to 16, "PHS III" to 15,
        "DCM I" to 0, "DCM II" to 0, "DEM I" to 0, "DEM II" to 0, "DCAM I" to 0, "DCAM II" to 0,
        "BS I" to 35, "BS II" to 43, "BS III" to 15,
        "BS BIOLOGICAL I" to 22, "BS CHEM MATHS I" to 19, "BS PHYSICAL I" to 21,
        "BS BIOLOGICAL II" to 24, "BS CHEM MATHS II" to 23, "BS PHYSICAL II" to 24,
        "BS BIOLOGICAL III" to 2, "BS MATHEMATICS III" to 2, "BS PHYSICS III" to 15,
        "DLT I" to 11, "DLT II" to 18,
        "DLT BIOLOGY II" to 6, "DLT CHEMISTRY II" to 7, "DLT PHYSICS II" to 5,
        "BME I" to 9, "BME II" to 11, "BME III" to 11, "BME IV" to 9,
        "EEE I" to 10, "EEE II" to 8, "EEE III" to 9, "EEE IV" to 7,
        "PEEM I" to 11, "PEEM II" to 13, "PEEM III" to 9, "PEEM IV" to 9,
        "CVE I" to 12, "CVE II" to 8, "CVE III" to 11, "CVE IV" to 8,
        "MIE I" to 11, "MIE II" to 12, "MIE III" to 10, "MIE IV" to 9,
        "BCS I" to 13, "BCS II" to 9, "BCS III" to 12,
        "BIT I" to 9, "BIT II" to 15, "BIT III" to 12,
        "BSE I" to 9, "BSE II" to 11, "BSE III" to 7, "BSE IV" to 7,
        "BBA I" to 10, "BBA II" to 17, "BBA III" to 4,
        "BSAF I" to 10, "BSAF II" to 13, "BSAF III" to 12,
        "ECO I" to 11, "ECO II" to 10, "ECO III" to 12,
        "BPSM I" to 9, "BPSM II" to 10, "BPSM III" to 11,
        "BSAL I" to 12, "BSAL II" to 12, "BSAL III" to 12, "BSAL IV" to 9,
        "BGWH I" to 0, "BGWH II" to 0, "BGWH III" to 12,
        "BPCD I" to 7, "BPCD II" to 11, "BPCD III" to 4
    )

    /**
     * Returns the number of distinct deduplicated timetable entries for [group].
     *
     * IMPORTANT: [entries] should always be supplied from the live [TimetableRepository]
     * to avoid silent data drift across academic semester updates.
     * When [entries] is null or empty (e.g. before initial asset hydration completes),
     * [PRECOMPUTED_GROUP_COUNTS] serves as a temporary startup fallback.
     */
    fun getGroupCount(group: String, entries: List<TimetableEntry>? = null): Int {
        if (!entries.isNullOrEmpty()) {
            return filterTimetable(entries, group).size
        }
        val pre = PRECOMPUTED_GROUP_COUNTS[group]
        if (pre != null) return pre
        return 0
    }

    fun parseGroupParts(raw: String): ParsedGroup? {
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
        return ParsedGroup(code, subject, year)
    }

    fun parseGroup(raw: String): String? {
        val parsed = parseGroupParts(raw) ?: return null
        return listOf(parsed.code, parsed.subject, parsed.year).filter { it.isNotEmpty() }.joinToString(" ")
    }

    fun entryMatchesGroup(entry: TimetableEntry, selectedGroup: String): Boolean {
        val target = parseGroupParts(selectedGroup)
        if (target != null) {
            val entryGroups = listOf(entry.program_group) + entry.shared_with
            for (g in entryGroups) {
                val parsed = parseGroupParts(g)
                if (parsed != null) {
                    if (parsed.code == target.code && parsed.year == target.year) {
                        if (target.subject.isEmpty() || parsed.subject.isEmpty() ||
                            parsed.subject.contains(target.subject) || target.subject.contains(parsed.subject)) {
                            return true
                        }
                    }
                } else {
                    // Fallback for non-standard shared labels like "MBR III" or "AB- MED/PCH"
                    val clean = g.uppercase().replace(Regex("[\\s-/]+"), " ")
                    val parts = clean.split(" ").filter { it.isNotEmpty() }
                    val cleanParts = parts.map { CODE_ALIASES[it] ?: it }
                    if (target.code in cleanParts && target.year in cleanParts) {
                        return true
                    }
                }
            }
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
