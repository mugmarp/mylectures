package com.mustime.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mustime.features.timetable.ui.BorderSubtleLight
import com.mustime.features.timetable.ui.DarkBorderSubtle
import com.mustime.features.timetable.ui.DarkSurfaceCard
import com.mustime.ui.LocalAppTheme

/**
 * Editorial Seamless Top Bar container (Apple / Minimalist Flagship Style).
 * - At rest (top of page): 100% transparent background, zero dividing line, seamlessly blends with content.
 * - On scroll: Smoothly transitions into the 0.96f frosted translucent surface matching the bottom dock,
 *   with a crisp 0.5dp hairline border for content separation.
 */
@Composable
fun SeamlessEditorialTopBar(
    isScrolled: Boolean,
    modifier: Modifier = Modifier,
    elevation: Dp = if (isScrolled) 4.dp else 0.dp,
    content: @Composable () -> Unit
) {
    val isDark = LocalAppTheme.current.isDark
    val targetBg = if (isScrolled) {
        if (isDark) DarkSurfaceCard.copy(alpha = 0.96f) else Color.White.copy(alpha = 0.96f)
    } else {
        Color.Transparent
    }
    val animatedBg by animateColorAsState(
        targetValue = targetBg,
        animationSpec = tween(durationMillis = 200),
        label = "TopBarBg"
    )

    val targetBorder = if (isScrolled) {
        (if (isDark) DarkBorderSubtle else BorderSubtleLight).copy(alpha = 0.5f)
    } else {
        Color.Transparent
    }
    val animatedBorderColor by animateColorAsState(
        targetValue = targetBorder,
        animationSpec = tween(durationMillis = 200),
        label = "TopBarBorder"
    )

    Surface(
        color = animatedBg,
        tonalElevation = if (isScrolled) 2.dp else 0.dp,
        shadowElevation = elevation,
        border = BorderStroke(0.5.dp, animatedBorderColor),
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            content()
        }
    }
}
