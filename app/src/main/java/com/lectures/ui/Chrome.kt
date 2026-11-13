package com.lectures.ui

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Single source of truth for app-wide chrome dimensions, insets, and density modes.
 *
 * Guarantees that the Floating Dock, Extended FABs, and LazyColumn scroll bodies
 * across Timetable, Calendar, Notes, and Tasks all adhere to the exact same clearance
 * contract, preventing OEM-specific and 3-button navigation underlaps.
 */
object Chrome {
    val DockHeight: Dp = 60.dp
    val DockBottomGap: Dp = 6.dp
    val DockSideInset: Dp = 14.dp
    val ContentAboveDockGap: Dp = 16.dp
    val FabAboveDockGap: Dp = 12.dp
    val FabHeight: Dp = 56.dp

    /**
     * Alpha transparency for glassy top bars when scrolled.
     * Matches the bottom dock (0.96f) for a unified premium feel.
     */
    const val TopBarGlassAlpha: Float = 0.96f
    const val DockAlpha: Float = 0.96f

    val NavigationRailWidth: Dp = 76.dp

    /**
     * Total bottom padding required for scrollable list content to clear both the floating dock
     * (or NavigationRail in tablet mode) and the Floating Action Button (FAB).
     */
    fun listBottomPadding(navBarBottom: Dp, useNavigationRail: Boolean = false): Dp =
        if (useNavigationRail) {
            navBarBottom + FabHeight + ContentAboveDockGap + 8.dp
        } else {
            navBarBottom + DockHeight + FabAboveDockGap + FabHeight + ContentAboveDockGap
        }

    /**
     * Total bottom padding required for scrollable list content when no FAB is present.
     */
    fun listBottomPaddingDockOnly(navBarBottom: Dp, useNavigationRail: Boolean = false): Dp =
        if (useNavigationRail) {
            navBarBottom + ContentAboveDockGap
        } else {
            navBarBottom + DockHeight + ContentAboveDockGap
        }

    /**
     * Bottom padding for FAB placed inside a layout using navigationBarsPadding().
     * (60.dp dock + 12.dp above-dock gap = 72.dp on phone; 16.dp on tablet rail).
     */
    fun fabBottomPadding(useNavigationRail: Boolean = false): Dp =
        if (useNavigationRail) 16.dp else DockHeight + FabAboveDockGap
}

/**
 * Responsive viewport metrics and density modes for Lectures across phone, foldable, and tablet screens.
 */
data class LecturesWindow(
    val widthDp: Int,
    val heightDp: Int,
    val fontScale: Float,
    val navBarBottom: Dp,
    val isImeVisible: Boolean
) {
    /** True for narrow viewports (e.g. Galaxy Fold cover screen, split-screen, or < 360dp width). */
    val isNarrow: Boolean get() = widthDp < 360

    /** True for short viewports (e.g. split-screen multi-window or < 620dp usable height). */
    val isShort: Boolean get() = heightDp < 620

    /** True when system font scaling is enlarged by the user (>= 1.15x). */
    val isTightFont: Boolean get() = fontScale >= 1.15f

    /** Material 3 WindowSizeClass width classifications. */
    val isCompact: Boolean get() = widthDp < 600
    val isMedium: Boolean get() = widthDp in 600..839
    val isExpanded: Boolean get() = widthDp >= 840

    /** True for tablets, foldables unfolded, and desktop viewports (width >= 600dp). */
    val isTablet: Boolean get() = widthDp >= 600

    /** True when the shell should switch from a bottom pill dock to an ergonomic leading NavigationRail. */
    val useNavigationRail: Boolean get() = isTablet && heightDp >= 440

    /** True when screens should render dual-pane master-detail or side-by-side layouts (Expanded width >= 840dp). */
    val isDualPane: Boolean get() = isExpanded

    /** True when dock tab labels would be cramped and should switch to icon-only. */
    val dockIconOnly: Boolean get() = isNarrow || isTightFont

    /** True when day capsule strip should use compact 1/2-letter tokens to prevent clipping. */
    val dayStripCompact: Boolean get() = isNarrow || isTightFont

    /** True when top bar action button ("Free Rooms") should be an icon badge to avoid title collision. */
    val topBarCompactAction: Boolean get() = widthDp < 380

    /** True when dual filter dropdowns (Campus & Building) should stack vertically. */
    val filtersStacked: Boolean get() = isNarrow

    /** True when FAB should collapse to an icon-only button to conserve vertical viewport. */
    val collapseFab: Boolean get() = isShort && !useNavigationRail
}

/**
 * Remember and observe active responsive window parameters.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun rememberLecturesWindow(): LecturesWindow {
    val configuration = LocalConfiguration.current
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val isIme = WindowInsets.isImeVisible

    return remember(
        configuration.screenWidthDp,
        configuration.screenHeightDp,
        configuration.fontScale,
        navBottom,
        isIme
    ) {
        LecturesWindow(
            widthDp = configuration.screenWidthDp,
            heightDp = configuration.screenHeightDp,
            fontScale = configuration.fontScale,
            navBarBottom = navBottom,
            isImeVisible = isIme
        )
    }
}
