package com.mustime.features.timetable.domain

object TimetableVersionResolver {

    /**
     * Dynamically identifies and parses a timetable release version.
     * The number of drafts is dynamic (Draft 1, Draft 2, Draft 3, Draft 4, Draft N),
     * followed by the final release ("Final Timetable" / "Official Final").
     */
    fun resolve(
        rawVersion: String?,
        isExplicitFinal: Boolean? = null,
        academicYear: String = "2026/2027",
        semester: String = "Semester 1",
        releaseNotes: String? = null,
        totalEntries: Int = 1304
    ): TimetableMetadata {
        val clean = rawVersion?.trim() ?: ""

        val isFinal = isExplicitFinal == true ||
                clean.equals("FINAL", ignoreCase = true) ||
                clean.contains("final", ignoreCase = true)

        if (isFinal) {
            val label = if (clean.isNotBlank() && clean.contains("final", ignoreCase = true)) {
                if (clean.equals("FINAL", ignoreCase = true)) "Final Timetable" else clean
            } else {
                "Final Timetable"
            }
            return TimetableMetadata(
                id = 1,
                versionLabel = label,
                status = "Official Final",
                isFinal = true,
                draftNumber = null,
                academicYear = academicYear,
                semester = semester,
                releaseNotes = releaseNotes ?: "Authoritative MUST Teaching Timetable approved by Academic Registrar",
                lastUpdated = System.currentTimeMillis(),
                source = "MUST Academic Registrar",
                totalSessions = if (totalEntries > 0) totalEntries else 1304
            )
        }

        // Dynamically extract draft number without assuming a fixed number of drafts
        val draftRegex = Regex("(?i)draft\\s*(\\d+)")
        val match = draftRegex.find(clean)
        val draftNumber = match?.groupValues?.get(1)?.toIntOrNull() ?: run {
            val romanRegex = Regex("(?i)draft\\s*(I|II|III|IV|V|VI)\\b")
            val romanMatch = romanRegex.find(clean)?.groupValues?.get(1)?.uppercase()
            when (romanMatch) {
                "I" -> 1
                "II" -> 2
                "III" -> 3
                "IV" -> 4
                "V" -> 5
                "VI" -> 6
                else -> 1
            }
        }

        val label = if (clean.isNotBlank()) clean else "Draft $draftNumber"

        return TimetableMetadata(
            id = 1,
            versionLabel = label,
            status = "Active Draft",
            isFinal = false,
            draftNumber = draftNumber,
            academicYear = academicYear,
            semester = semester,
            releaseNotes = releaseNotes ?: "MUST Semester 1 Timetable Draft $draftNumber (Pending Final Ratification)",
            lastUpdated = System.currentTimeMillis(),
            source = "MUST Academic Registrar",
            totalSessions = if (totalEntries > 0) totalEntries else 1304
        )
    }
}
