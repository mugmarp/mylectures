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
 * Floating Island Header (Option 1).
 * Mirrors the floating pill dock at the bottom:
 * - Rounded capsule (22.dp corner radius)
 * - Side insets (14.dp) and status bar clearance
 * - Translucent 0.96f frosted surface
 * - 6.dp elevation shadow & subtle hairline border
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

    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(
                start = Chrome.DockSideInset,
                end = Chrome.DockSideInset,
                top = 6.dp,
                bottom = 6.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = navBg.copy(alpha = 0.96f),
            tonalElevation = 6.dp,
            shadowElevation = 6.dp,
            border = BorderStroke(1.dp, navBorder),
            modifier = Modifier
                .widthIn(max = 440.dp)
                .fillMaxWidth()
        ) {
            content()
        }
    }
}
