package com.mustime.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
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
 * Floating Island header.
 *
 * Mirrors the floating pill dock at the bottom of the screen:
 * rounded capsule, side insets, status-bar clearance, translucent frosted
 * surface, hairline border.
 *
 * Deliberately static: no scroll listeners, no derived state, no elevation
 * animation. Unlike [SeamlessEditorialTopBar] it does not react to scroll
 * position, which removes the recomposition churn that caused flicker while
 * scrolling the timetable.
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
