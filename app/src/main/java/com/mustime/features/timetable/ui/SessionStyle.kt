package com.mustime.features.timetable.ui

import androidx.compose.ui.graphics.Color
import com.mustime.features.timetable.domain.TimetableEntry

data class SessionStyle(
    val badgeBg: Color,
    val badgeText: Color,
    val accentBar: Color
)

fun sessionStyle(sessionType: String?, room: String, title: String): SessionStyle {
    val type = sessionType?.uppercase() ?: ""
    val isAssociation = title.contains("MUCOSA", ignoreCase = true) ||
            title.contains("ASSOCIATION", ignoreCase = true) ||
            title.contains("GUILD", ignoreCase = true) ||
            title.contains("SOCIETY", ignoreCase = true) ||
            type.contains("ASSOCIATION")

    val isPractical = type == "PRACTICAL" || type == "LAB" || room.uppercase().contains("LAB")
    val isClinical = type == "CLINICAL" || title.uppercase().contains("CLINICAL") || title.uppercase().contains("WARD")

    return when {
        isAssociation -> SessionStyle(Color(0xFF8B5CF6), Color.White, Color(0xFF7C3AED))
        isClinical -> SessionStyle(Color(0xFFF59E0B), Color.White, Color(0xFFD97706))
        isPractical -> SessionStyle(Color(0xFF22C55E), Color.White, Color(0xFF16A34A))
        type == "THEORY" -> SessionStyle(Color(0xFF3B82F6), Color.White, Color(0xFF2563EB))
        else -> SessionStyle(Color(0xFF3B82F6), Color.White, Color(0xFF2563EB))
    }
}

fun resolveSessionTypeLabel(entry: TimetableEntry): String {
    val code = entry.courseCode.trim().uppercase()
    val title = entry.courseTitle.trim().uppercase()
    if (code.contains("MUCOSA") || title.contains("MUCOSA") ||
        title.contains("ASSOCIATION") || title.contains("GUILD") ||
        code.contains("GUILD") || title.contains("SOCIETY")
    ) {
        return "Student Association"
    }

    val type = entry.sessionType?.trim()
    if (!type.isNullOrEmpty()) {
        return when (type.uppercase()) {
            "THEORY" -> "Lecture"
            "LAB" -> "Lab"
            "PRACTICAL" -> "Practical"
            "CLINICAL" -> "Clinical"
            else -> type.lowercase().replaceFirstChar { it.uppercase() }
        }
    }

    if (entry.room?.uppercase()?.contains("LAB") == true) return "Lab"
    if (title.contains("CLINICAL") || title.contains("WARD")) return "Clinical"
    return "Lecture"
}
