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
        "BS", "DLT", "HEAP", "BME", "EEE", "PEEM", "CVE", "MIE", "BCS", "BIT",
        "BSE", "BBA", "BSAF", "ECO", "BPSM", "BSAL", "BGWH", "BPCD"
    )

    // Direct mapping for un-prefixed database groups to their academic programme, specialization, and year
    private val DIRECT_DB_GROUPS: Map<String, ParsedGroup> = mapOf(
        "BIOLOGY" to ParsedGroup("BS", "BIOLOGICAL", "III"),
        "CHEMISTRY" to ParsedGroup("BS", "CHEMISTRY", "III"),
        "MATHEMATICS" to ParsedGroup("BS", "MATHEMATICS", "III"),
        "PHYSICS" to ParsedGroup("BS", "PHYSICS", "III"),
        "ACCOUNTING" to ParsedGroup("BBA", "ACCOUNTING", "III"),
        "BANKING AND FINANCE" to ParsedGroup("BBA", "BANKING AND FINANCE", "III"),
        "HUMAN RESOURCE" to ParsedGroup("BBA", "HUMAN RESOURCE", "III"),
        "MARKETING" to ParsedGroup("BBA", "MARKETING", "III"),
        "PROCUREMENT" to ParsedGroup("BBA", "PROCUREMENT", "III"),
        "SUSTAINABLE RURAL DEVELOPMENT" to ParsedGroup("BPCD", "SUSTAINABLE RURAL DEVELOPMENT", "III"),
        "URBAN POLICY" to ParsedGroup("BPCD", "URBAN POLICY", "III"),
        "Biology-Chemistry" to ParsedGroup("HEAP", "Biology-Chemistry", "I"),
        "Chemistry- Maths" to ParsedGroup("HEAP", "Chemistry- Maths", "I"),
        "Physics-Maths" to ParsedGroup("HEAP", "Physics-Maths", "I")
    )

    @Deprecated(
        message = "Precomputed static counts are retained strictly as a startup fallback. Query live cohort counts dynamically via TimetableDao.getCohortCounts() or pass entries to getGroupCount.",
        replaceWith = ReplaceWith("dao.getCohortCounts(programmeCode)")
    )
    val PRECOMPUTED_GROUP_COUNTS: Map<String, Int> = mapOf(
        "MBR I" to 17, "MBR II" to 12, "MBR III" to 11, "MBR IV" to 19, "MBR V" to 11,
        "PHA I" to 16, "PHA II" to 14, "PHA III" to 16, "PHA IV" to 14,
        "BNS I" to 25, "BNS II" to 31, "BNS III" to 34, "BNS IV" to 17,
        "MLS I" to 16, "MLS II" to 32, "MLS III" to 35, "MLS IV" to 14,
        "BSP I" to 23, "BSP II" to 28, "BSP III" to 10, "BSP IV" to 11,
        "PHS I" to 15, "PHS II" to 16, "PHS III" to 15,
        "DCM I" to 0, "DCM II" to 0, "DEM I" to 0, "DEM II" to 0, "DCAM I" to 0, "DCAM II" to 0,
        "BS BIOLOGICAL I" to 22, "BS CHEM MATHS I" to 19, "BS PHYSICAL I" to 21,
        "BS BIOLOGICAL II" to 24, "BS CHEM MATHS II" to 23, "BS PHYSICAL II" to 24,
        "BS BIOLOGICAL III" to 13, "BS CHEMISTRY III" to 12, "BS MATHEMATICS III" to 13, "BS PHYSICS III" to 15,
        "DLT I" to 11, "DLT BIOLOGY II" to 6, "DLT CHEMISTRY II" to 7, "DLT PHYSICS II" to 5,
        "HEAP Biology-Chemistry" to 14, "HEAP Chemistry- Maths" to 14, "HEAP Physics-Maths" to 14,
        "BME I" to 9, "BME II" to 11, "BME III" to 11, "BME IV" to 9,
        "EEE I" to 10, "EEE II" to 8, "EEE III" to 9, "EEE IV" to 7,
        "PEEM I" to 11, "PEEM II" to 13, "PEEM III" to 9, "PEEM IV" to 9,
        "CVE I" to 12, "CVE II" to 8, "CVE III" to 11, "CVE IV" to 8,
        "MIE I" to 11, "MIE II" to 12, "MIE III" to 10, "MIE IV" to 9,
        "BCS I" to 13, "BCS II" to 9, "BCS III" to 12,
        "BIT I" to 9, "BIT II" to 15, "BIT III" to 12,
        "BSE I" to 9, "BSE II" to 11, "BSE III" to 7, "BSE IV" to 7,
        "BBA I" to 10, "BBA II" to 17,
        "BBA ACCOUNTING III" to 12, "BBA BANKING AND FINANCE III" to 11, "BBA HUMAN RESOURCE III" to 10, "BBA MARKETING III" to 10, "BBA PROCUREMENT III" to 10,
        "BSAF I" to 10, "BSAF II" to 13, "BSAF III" to 12,
        "ECO I" to 11, "ECO II" to 10, "ECO III" to 12,
        "BPSM I" to 9, "BPSM II" to 10, "BPSM III" to 11,
        "BSAL I" to 12, "BSAL II" to 12, "BSAL III" to 12, "BSAL IV" to 9,
        "BGWH I" to 0, "BGWH II" to 0, "BGWH III" to 12,
        "BPCD I" to 7, "BPCD II" to 11,
        "BPCD SUSTAINABLE RURAL DEVELOPMENT III" to 8, "BPCD URBAN POLICY III" to 7
    )

    fun getGroupCount(
        group: String,
        entries: List<TimetableEntry>? = null,
        cohortCounts: Map<String, Int>? = null
    ): Int {
        if (!entries.isNullOrEmpty()) {
            return filterTimetable(entries, group).size
        }
        if (cohortCounts != null) {
            val count = cohortCounts[group] ?: cohortCounts[canonicalCohort(group)]
            if (count != null && count > 0) return count
        }
        val pre = PRECOMPUTED_GROUP_COUNTS[group] ?: PRECOMPUTED_GROUP_COUNTS[canonicalCohort(group)]
        if (pre != null) return pre
        return 0
    }

    fun parseGroupParts(raw: String): ParsedGroup? {
        val clean = raw.trim()
        if (clean.isEmpty()) return null

        // 1. Direct un-prefixed database groups
        DIRECT_DB_GROUPS[clean]?.let { return it }
        val upperClean = clean.uppercase()
        DIRECT_DB_GROUPS.entries.firstOrNull { it.key.uppercase() == upperClean }?.value?.let { return it }

        // 2. HEAP tracks
        if (upperClean.startsWith("HEAP")) {
            val trackPart = clean.removePrefix("HEAP").trim()
            return ParsedGroup("HEAP", trackPart.ifBlank { "Biology-Chemistry" }, "I")
        }

        // 3. Normal Roman numeral formats (handles both spaces and hyphens e.g. "DLT-BIOLOGY II", "BS-CHEM-MATHS I")
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
            .replace("BIOLOGY", "BIOLOGICAL")

        return ParsedGroup(code, subject, year)
    }

    fun canonicalCohort(raw: String): String {
        val clean = raw.trim()
        if (clean.isEmpty()) return ""
        val parsed = parseGroupParts(clean) ?: return clean
        return if (parsed.code == "HEAP") {
            "HEAP ${parsed.subject}".trim()
        } else if (parsed.subject.isNotEmpty()) {
            "${parsed.code} ${parsed.subject} ${parsed.year}".trim()
        } else {
            "${parsed.code} ${parsed.year}".trim()
        }
    }

    fun parseGroup(raw: String): String? {
        val parsed = parseGroupParts(raw) ?: return null
        return listOf(parsed.code, parsed.subject, parsed.year).filter { it.isNotEmpty() }.joinToString(" ")
    }

    fun entryMatchesGroup(entry: TimetableEntry, selectedGroup: String): Boolean {
        val target = parseGroupParts(selectedGroup)
        val cleanSel = selectedGroup.trim().uppercase()
        val entryGroups = listOf(entry.program_group) + entry.shared_with

        for (g in entryGroups) {
            val gClean = g.trim().uppercase()
            if (gClean == cleanSel) return true

            val parsed = parseGroupParts(g)
            if (target != null && parsed != null) {
                if (parsed.code == target.code && parsed.year == target.year) {
                    if (target.subject.isEmpty() || parsed.subject.isEmpty() ||
                        parsed.subject.equals(target.subject, ignoreCase = true) ||
                        parsed.subject.contains(target.subject) || target.subject.contains(parsed.subject) ||
                        (target.subject.startsWith("BIOLOG") && parsed.subject.startsWith("BIOLOG"))) {
                        return true
                    }
                }
            } else if (target != null) {
                val cleanParts = gClean.split(Regex("[\\s-/]+")).filter { it.isNotEmpty() }
                if (target.code in cleanParts && target.year in cleanParts) {
                    return true
                }
            }
        }
        return false
    }

    fun deduplicateEntries(entries: List<TimetableEntry>, targetGroup: String = ""): List<TimetableEntry> {
        if (entries.size <= 1) {
            return entries.map { cleanAnomalousEntry(it) }
        }
        val targetNorm = targetGroup.trim().uppercase()
        val result = LinkedHashMap<String, TimetableEntry>()

        for (rawEntry in entries) {
            val entry = cleanAnomalousEntry(rawEntry)
            val codeKey = entry.course_code.trim().uppercase()
            val dayKey = entry.day.trim().take(3).uppercase()
            val startKey = entry.start_time.trim()
            val endKey = (entry.end_time ?: "").trim()
            val sessionKey = "$codeKey|$dayKey|$startKey|$endKey"

            val existing = result[sessionKey]
            if (existing == null) {
                result[sessionKey] = entry
            } else {
                val newMatchesDirectly = targetNorm.isNotEmpty() && entry.program_group.trim().uppercase().contains(targetNorm)
                val existingMatchesDirectly = targetNorm.isNotEmpty() && existing.program_group.trim().uppercase().contains(targetNorm)
                val primary = if (newMatchesDirectly && !existingMatchesDirectly) entry else existing
                val secondary = if (primary == entry) existing else entry

                val mergedRoom = when {
                    !primary.room.isNullOrBlank() && primary.room.trim().uppercase() !in listOf("TBA", "TBD", "NONE", "N/A") -> primary.room.trim()
                    !secondary.room.isNullOrBlank() && secondary.room.trim().uppercase() !in listOf("TBA", "TBD", "NONE", "N/A") -> secondary.room.trim()
                    else -> null
                }

                val mergedLecturer = when {
                    !primary.lecturer.isNullOrBlank() && !primary.lecturer.equals("Staff", ignoreCase = true) -> primary.lecturer.trim()
                    !secondary.lecturer.isNullOrBlank() && !secondary.lecturer.equals("Staff", ignoreCase = true) -> secondary.lecturer.trim()
                    else -> primary.lecturer
                }

                // Combine cohorts and canonicalize so aliases collapse into a single clean cohort
                val primaryCanon = canonicalCohort(primary.program_group)
                val combinedShared = (primary.shared_with + secondary.shared_with + listOf(primary.program_group, secondary.program_group))
                    .map { canonicalCohort(it.trim()) }
                    .filter { it.isNotBlank() && !it.equals(primaryCanon, ignoreCase = true) }
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

    /**
     * Cleans up known historical anomalies from raw timetable exports
     * (e.g. lecturer names erroneously recorded in room fields, or course code placeholders).
     */
    private fun cleanAnomalousEntry(entry: TimetableEntry): TimetableEntry {
        var cleanRoom = entry.room
        var cleanLecturer = entry.lecturer
        var cleanCode = entry.course_code
        var cleanTitle = entry.course_title

        // Fix Dr. William Wasswa erroneously placed in room column
        if (cleanRoom?.equals("William Wasswa", ignoreCase = true) == true) {
            cleanRoom = null
            if (cleanLecturer.isNullOrBlank() || cleanLecturer.equals("Staff", ignoreCase = true)) {
                cleanLecturer = "Dr. William Wasswa"
            }
        }

        // Fix Dr. Nsambu Kijjambu Frederick in course code
        if (cleanCode.equals("NSAMBU", ignoreCase = true)) {
            cleanCode = "BBA3105"
            cleanTitle = "Taxation II"
            if (cleanLecturer.isNullOrBlank() || cleanLecturer.equals("Staff", ignoreCase = true)) {
                cleanLecturer = "Dr. Nsambu Kijjambu Frederick"
            }
        }

        // Canonicalize shared_with entries
        val canonShared = entry.shared_with.map { canonicalCohort(it.trim()) }.filter { it.isNotBlank() }.distinct()

        return entry.copy(
            course_code = cleanCode,
            course_title = cleanTitle,
            room = cleanRoom,
            lecturer = cleanLecturer,
            shared_with = canonShared
        )
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
