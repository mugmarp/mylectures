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
                    // Include the parent year group (which aggregates all tracks or common classes)
                    list.add("$code $yr")
                    // Include each specific track group
                    yearTracks.forEach { track ->
                        list.add("$code $track $yr")
                    }
                }
            }
            return list
        }
}

val CLINICAL_PROGRAMMES = setOf("MBR", "PHA", "BNS", "MLS", "BSP", "PHS", "DCM", "DEM", "DCAM")

val FACULTIES = listOf(
    Faculty("medicine", "Faculty of Medicine", "Faculty Health Sciences", "Stethoscope", listOf(
        Programme("MBR", "Bachelor of Medicine and Bachelor of Surgery", 5),
        Programme("PHA", "Bachelor of Pharmacy", 4),
        Programme("BNS", "Bachelor of Nursing Science", 4),
        Programme("MLS", "Bachelor of Medical Laboratory Science", 4),
        Programme("BSP", "Bachelor of Science in Physiotherapy", 4),
        Programme("PHS", "Bachelor of Science in Pharmaceutical Sciences", 3),
        Programme("DCM", "Diploma in Community HIV/AIDS Care and Management", 2),
        Programme("DEM", "Diploma in Emergency Medicine", 2),
        Programme("DCAM", "Adv. Diploma in Child and Adolescent Mental Health", 2)
    )),
    Faculty("science", "Faculty of Science", null, "FlaskConical", listOf(
        Programme(
            "BS",
            "Bachelor of Science with Education",
            3,
            tracks = mapOf(
                "I" to listOf("BIOLOGICAL", "CHEM MATHS", "PHYSICAL"),
                "II" to listOf("BIOLOGICAL", "CHEM MATHS", "PHYSICAL"),
                "III" to listOf("BIOLOGICAL", "MATHEMATICS", "PHYSICS")
            )
        ),
        Programme(
            "DLT",
            "Diploma in Science Laboratory Technology",
            2,
            tracks = mapOf(
                "II" to listOf("BIOLOGY", "CHEMISTRY", "PHYSICS")
            )
        )
    )),
    Faculty("fast", "Faculty of Applied Sciences and Technology", "FAST", "Cog", listOf(
        Programme("BME", "Bachelor of Biomedical Engineering", 4),
        Programme("EEE", "Bachelor of Engineering in Electrical & Electronics Engineering", 4),
        Programme("PEEM", "BSc in Petroleum Engineering & Environmental Management", 4),
        Programme("CVE", "Bachelor of Science in Civil Engineering", 4),
        Programme("MIE", "Bachelor of Science in Mechanical and Industrial Engineering", 4)
    )),
    Faculty("computing", "Faculty of Computing and Informatics", null, "Cpu", listOf(
        Programme("BCS", "Bachelor of Computer Science", 3),
        Programme("BIT", "Bachelor of Information Technology", 3),
        Programme("BSE", "Bachelor of Software Engineering", 4)
    )),
    Faculty("business", "Faculty of Business and Management Sciences", null, "Briefcase", listOf(
        Programme("BBA", "Bachelor of Business Administration", 3),
        Programme("BSAF", "Bachelor of Science in Accounting and Finance", 3),
        Programme("ECO", "Bachelor of Science in Economics", 3),
        Programme("BPSM", "BSc in Procurement & Supply Chain Management", 3)
    )),
    Faculty("interdisciplinary", "Faculty of Interdisciplinary Studies", null, "Users", listOf(
        Programme("BSAL", "BSc in Agriculture and Livelihoods", 4),
        Programme("BGWH", "BSc in Gender and Applied Women Health", 3, customYears = listOf("III")),
        Programme("BPCD", "BSc in Planning and Community Development", 3)
    ))
)

object FacultyCatalog {
    val faculties = FACULTIES
}
