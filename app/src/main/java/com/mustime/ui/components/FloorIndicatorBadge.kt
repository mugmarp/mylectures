package com.mustime.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Stairs
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mustime.features.rooms.BuildingLevel

/**
 * Visual indicator badge displaying on what floor/level a university room is located.
 * Used beside room identifiers in VacantRoomsScreen, Timetable lecture cards, and detail sheets.
 */
@Composable
fun FloorIndicatorBadge(
    level: BuildingLevel,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    isDark: Boolean = false,
    useContrastColor: Boolean = false
) {
    val (badgeBg, badgeText, badgeBorder) = when {
        useContrastColor -> {
            // High-contrast variant for cards with saturated/gradient backgrounds (e.g. NextUpCard)
            Triple(
                Color.White.copy(alpha = 0.22f),
                Color.White,
                Color.White.copy(alpha = 0.35f)
            )
        }
        isDark -> {
            when (level) {
                BuildingLevel.GROUND -> Triple(Color(0xFF064E3B).copy(alpha = 0.7f), Color(0xFF6EE7B7), Color(0xFF047857).copy(alpha = 0.5f))
                BuildingLevel.FIRST -> Triple(Color(0xFF1E3A8A).copy(alpha = 0.7f), Color(0xFF93C5FD), Color(0xFF2563EB).copy(alpha = 0.5f))
                BuildingLevel.SECOND -> Triple(Color(0xFF4C1D95).copy(alpha = 0.7f), Color(0xFFC4B5FD), Color(0xFF7C3AED).copy(alpha = 0.5f))
                BuildingLevel.THIRD -> Triple(Color(0xFF78350F).copy(alpha = 0.7f), Color(0xFFFDE68A), Color(0xFFD97706).copy(alpha = 0.5f))
                BuildingLevel.FOURTH -> Triple(Color(0xFF831843).copy(alpha = 0.7f), Color(0xFFFBCFE8), Color(0xFFBE185D).copy(alpha = 0.5f))
            }
        }
        else -> {
            when (level) {
                BuildingLevel.GROUND -> Triple(Color(0xFFECFDF5), Color(0xFF047857), Color(0xFFA7F3D0))
                BuildingLevel.FIRST -> Triple(Color(0xFFEFF6FF), Color(0xFF1D4ED8), Color(0xFFBFDBFE))
                BuildingLevel.SECOND -> Triple(Color(0xFFF5F3FF), Color(0xFF6D28D9), Color(0xFFDDD6FE))
                BuildingLevel.THIRD -> Triple(Color(0xFFFFFBEB), Color(0xFFB45309), Color(0xFFFDE68A))
                BuildingLevel.FOURTH -> Triple(Color(0xFFFDF2F8), Color(0xFFBE185D), Color(0xFFFBCFE8))
            }
        }
    }

    val label = if (compact) level.shortName else level.displayName

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(badgeBg)
            .border(1.dp, badgeBorder, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Icon(
            imageVector = if (level == BuildingLevel.GROUND) Icons.Outlined.Layers else Icons.Outlined.Stairs,
            contentDescription = "Floor level: ${level.displayName}",
            tint = badgeText,
            modifier = Modifier.size(11.dp)
        )
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = badgeText,
            letterSpacing = 0.1.sp
        )
    }
}

/**
 * Convenient helper to resolve and render a FloorIndicatorBadge directly from any room name / identifier string.
 * Automatically resolves room directory level or applies smart campus naming heuristics.
 */
@Composable
fun RoomFloorBadge(
    roomName: String?,
    modifier: Modifier = Modifier,
    compact: Boolean = true,
    isDark: Boolean = false,
    useContrastColor: Boolean = false
) {
    if (roomName.isNullOrBlank()) return
    val level = androidx.compose.runtime.remember(roomName) {
        com.mustime.features.rooms.UniversityDirectory.resolveLevel(roomName)
    } ?: return

    FloorIndicatorBadge(
        level = level,
        modifier = modifier,
        compact = compact,
        isDark = isDark,
        useContrastColor = useContrastColor
    )
}
