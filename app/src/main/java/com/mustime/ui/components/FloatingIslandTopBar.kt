package com.mustime.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mustime.features.timetable.ui.BorderSubtleLight
import com.mustime.features.timetable.ui.DarkBorderSubtle
import com.mustime.features.timetable.ui.DarkSurfaceCard
import com.mustime.ui.Chrome
import com.mustime.ui.LocalAppTheme

/**
 * Edge-to-Edge Top Bar Header with rounded bottom corners.
 * - Extends edge-to-edge behind the Android system status bar
 * - Frosted translucent 0.96f background surface with subtle bottom border
 * - Rounded bottom corners (22.dp) curving into the page content below
 * - Static properties: Zero scroll listeners, zero recalculations, completely eliminated flicker.
 */
@Composable
fun FloatingIslandTopBar(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val isDark = LocalAppTheme.current.isDark
    val navBg = if (isDark) DarkSurfaceCard else Color.White
    val navBorder = if (isDark) DarkBorderSubtle else BorderSubtleLight

    Surface(
        shape = RoundedCornerShape(
            topStart = 0.dp,
            topEnd = 0.dp,
            bottomStart = 22.dp,
            bottomEnd = 22.dp
        ),
        color = navBg.copy(alpha = 0.96f),
        tonalElevation = 4.dp,
        shadowElevation = 4.dp,
        border = BorderStroke(1.dp, navBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(bottom = 6.dp)
        ) {
            content()
        }
    }
}
