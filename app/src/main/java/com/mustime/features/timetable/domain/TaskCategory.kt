package com.mustime.features.timetable.domain

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class TaskCategory(
    val displayName: String,
    val colorHex: String,
    val color: Color,
    val lightContainerColor: Color,
    val icon: ImageVector
) {
    ASSIGNMENT(
        displayName = "Assignment",
        colorHex = "#7C3AED",
        color = Color(0xFF7C3AED),
        lightContainerColor = Color(0xFFEDE9FE),
        icon = Icons.AutoMirrored.Outlined.Assignment
    ),
    PROJECT(
        displayName = "Project",
        colorHex = "#2563EB",
        color = Color(0xFF2563EB),
        lightContainerColor = Color(0xFFDBEAFE),
        icon = Icons.Outlined.RocketLaunch
    ),
    LAB_REPORT(
        displayName = "Lab Report",
        colorHex = "#0D9488",
        color = Color(0xFF0D9488),
        lightContainerColor = Color(0xFFCCFBF1),
        icon = Icons.Outlined.Science
    ),
    READING(
        displayName = "Reading / Prep",
        colorHex = "#D97706",
        color = Color(0xFFD97706),
        lightContainerColor = Color(0xFFFEF3C7),
        icon = Icons.Outlined.MenuBook
    ),
    EXAM_PREP(
        displayName = "Quiz / Exam",
        colorHex = "#DC2626",
        color = Color(0xFFDC2626),
        lightContainerColor = Color(0xFFFEE2E2),
        icon = Icons.Outlined.Timer
    ),
    GENERAL(
        displayName = "General Task",
        colorHex = "#4B5563",
        color = Color(0xFF4B5563),
        lightContainerColor = Color(0xFFF3F4F6),
        icon = Icons.Outlined.CheckCircleOutline
    );

    companion object {
        fun fromName(name: String?): TaskCategory {
            if (name.isNullOrBlank()) return ASSIGNMENT
            return entries.firstOrNull {
                it.name.equals(name, ignoreCase = true) ||
                it.displayName.equals(name, ignoreCase = true)
            } ?: ASSIGNMENT
        }
    }
}
