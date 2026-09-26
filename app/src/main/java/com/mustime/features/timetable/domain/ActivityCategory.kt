package com.mustime.features.timetable.domain

import androidx.compose.ui.graphics.Color

enum class ActivityCategory(
    val displayName: String,
    val colorHex: String,
    val color: Color,
    val lightContainerColor: Color,
    val darkContainerColor: Color
) {
    STUDY(
        displayName = "Study & Revision",
        colorHex = "#2563EB",
        color = Color(0xFF2563EB),
        lightContainerColor = Color(0xFFDBEAFE),
        darkContainerColor = Color(0xFF1E3A8A)
    ),
    LAB(
        displayName = "Lab & Practical",
        colorHex = "#7C3AED",
        color = Color(0xFF7C3AED),
        lightContainerColor = Color(0xFFEDE9FE),
        darkContainerColor = Color(0xFF4C1D95)
    ),
    SPORTS(
        displayName = "Sports & Fitness",
        colorHex = "#059669",
        color = Color(0xFF059669),
        lightContainerColor = Color(0xFFD1FAE5),
        darkContainerColor = Color(0xFF064E3B)
    ),
    CLUB(
        displayName = "Club & Society",
        colorHex = "#D97706",
        color = Color(0xFFD97706),
        lightContainerColor = Color(0xFFFEF3C7),
        darkContainerColor = Color(0xFF78350F)
    ),
    PERSONAL(
        displayName = "Personal & Social",
        colorHex = "#DB2777",
        color = Color(0xFFDB2777),
        lightContainerColor = Color(0xFFFCE7F3),
        darkContainerColor = Color(0xFF831843)
    ),
    EXAM(
        displayName = "Exam & Test",
        colorHex = "#DC2626",
        color = Color(0xFFDC2626),
        lightContainerColor = Color(0xFFFEE2E2),
        darkContainerColor = Color(0xFF7F1D1D)
    );

    companion object {
        fun fromName(name: String?): ActivityCategory {
            if (name.isNullOrBlank()) return STUDY
            return entries.firstOrNull {
                it.name.equals(name, ignoreCase = true) ||
                it.displayName.equals(name, ignoreCase = true)
            } ?: STUDY
        }

        fun colorForHex(hex: String?): Color {
            if (hex.isNullOrBlank()) return Color(0xFF2563EB)
            return try {
                Color(android.graphics.Color.parseColor(hex))
            } catch (e: Exception) {
                Color(0xFF2563EB)
            }
        }
    }
}
