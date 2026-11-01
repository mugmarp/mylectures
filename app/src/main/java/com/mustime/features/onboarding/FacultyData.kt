package com.mustime.features.onboarding

data class Faculty(
    val id: String,
    val name: String,
    val subtitle: String? = null,
    val icon: String,
    val programmes: List<Programme>
) {
    val iconName: String get() = icon
}

data class Programme(
    val code: String,
    val name: String,
    val years: Int,
    val tracks: Map<String, List<String>> = emptyMap(),
    val customYears: List<String>? = null
) {
    val defaultYears: List<String>
        get() = customYears ?: when (years) {
            5 -> listOf("I", "II", "III", "IV", "V")
            4 -> listOf("I", "II", "III", "IV")
            3 -> listOf("I", "II", "III")
            2 -> listOf("I", "II")
            1 -> listOf("I")
            else -> listOf("I", "II", "III")
        }

    val allGroups: List<String>
        get() {
            val list = mutableListOf<String>()
            for (yr in defaultYears) {
                val yearTracks = tracks[yr]
                if (yearTracks.isNullOrEmpty()) {
                    list.add("$code $yr")
                } else {
                    // Include each specific track group.
                    // For years with specialized tracks (e.g. BS, BBA III, DLT II, BPCD III, HEAP),
                    // avoid creating generic parent groups that have no real dedicated schedule
                    // and cause false schedule conflicts.
                    yearTracks.forEach { track ->
                        if (code == "HEAP") {
                            list.add("$code $track")
                        } else {
                            list.add("$code $track $yr")
                        }
                    }
                }
            }
            return list
        }
}

val CLINICAL_PROGRAMMES = setOf("MBR", "PHA", "BNS", "MLS", "BSP", "PHS", "DCM", "DEM", "DCAM")

val FACULTIES = listOf(
    Faculty("medicine", "Medicine", "Health Sciences", "Stethoscope", listOf(
        Programme("MBR", "MBChB (Medicine & Surgery)", 5),
        Programme("PHA", "B. Pharmacy (BPharm)", 4),
        Programme("BNS", "BSc Nursing Science", 4),
        Programme("MLS", "BSc Medical Lab Science", 4),
        Programme("BSP", "BSc Physiotherapy", 4),
        Programme("PHS", "BSc Pharmaceutical Sciences", 3),
        Programme("DCM", "Dip. Community HIV/AIDS Care & Mgt", 2),
        Programme("DEM", "Dip. Emergency Medicine", 2),
        Programme("DCAM", "Adv. Dip. Child & Adolescent Mental Health", 2)
    )),
    Faculty("science", "Science", null, "FlaskConical", listOf(
        Programme(
            "BS",
            "BSc Education (BSc.Ed)",
            3,
            tracks = mapOf(
                "I" to listOf("BIOLOGICAL", "CHEM MATHS", "PHYSICAL"),
                "II" to listOf("BIOLOGICAL", "CHEM MATHS", "PHYSICAL"),
                "III" to listOf("BIOLOGICAL", "CHEMISTRY", "MATHEMATICS", "PHYSICS")
            )
        ),
        Programme(
            "DLT",
            "Dip. Science Laboratory Technology",
            2,
            tracks = mapOf(
                "II" to listOf("BIOLOGY", "CHEMISTRY", "PHYSICS")
            )
        ),
        Programme(
            "HEAP",
            "Higher Education Access Cert.",
            1,
            tracks = mapOf(
                "I" to listOf("Biology-Chemistry", "Chemistry- Maths", "Physics-Maths")
            )
        )
    )),
    Faculty("fast", "Applied Sciences & Technology", "FAST", "Cog", listOf(
        Programme("BME", "BEng Biomedical Engineering", 4),
        Programme("EEE", "BEng Electrical & Electronics Engineering", 4),
        Programme("PEEM", "BSc Petroleum Engineering & Env. Mgt", 4),
        Programme("CVE", "BSc Civil Engineering", 4),
        Programme("MIE", "BSc Mechanical & Industrial Engineering", 4)
    )),
    Faculty("computing", "Computing & Informatics", null, "Cpu", listOf(
        Programme("BCS", "BSc Computer Science", 3),
        Programme("BIT", "B. Information Technology", 3),
        Programme("BSE", "B. Software Engineering", 4)
    )),
    Faculty("business", "Business & Management Sciences", null, "Briefcase", listOf(
        Programme(
            "BBA",
            "B. Business Administration",
            3,
            tracks = mapOf(
                "III" to listOf("ACCOUNTING", "BANKING AND FINANCE", "HUMAN RESOURCE", "MARKETING", "PROCUREMENT")
            )
        ),
        Programme("BSAF", "BSc Accounting & Finance", 3),
        Programme("ECO", "BSc Economics", 3),
        Programme("BPSM", "BSc Procurement & Supply Chain Mgt", 3)
    )),
    Faculty("interdisciplinary", "Interdisciplinary Studies", null, "Users", listOf(
        Programme("BSAL", "BSc Agriculture & Livelihoods", 4),
        Programme("BGWH", "BSc Gender & Applied Women Health", 3),
        Programme(
            "BPCD",
            "BSc Planning & Community Dev.",
            3,
            tracks = mapOf(
                "III" to listOf("SUSTAINABLE RURAL DEVELOPMENT", "URBAN POLICY")
            )
        )
    ))
)

object FacultyCatalog {
    val faculties = FACULTIES
}
