package com.mustime.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

val AccentColors = listOf(
    Color(0xFF2563EB) // Royal Blue (Default)
)

val AccentColorNames = listOf(
    "Royal Blue"
)

enum class ThemeMode(val title: String, val subtitle: String) {
    LIGHT("Light", "Clean & bright daylight"),
    DARK("Dark", "Deep charcoal & eye comfort"),
    SYSTEM("System", "Match device settings")
}

data class AppThemeState(
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val isDark: Boolean = false,
    val accentIndex: Int = 0,
    val onThemeModeChange: (ThemeMode) -> Unit = {},
    val onAccentChange: (Int) -> Unit = {}
)

val LocalAppTheme = compositionLocalOf { AppThemeState() }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppTheme(
    darkTheme: Boolean? = null,
    themeMode: ThemeMode = ThemeMode.LIGHT,
    accentIndex: Int = 0,
    onThemeModeChange: (ThemeMode) -> Unit = {},
    onAccentChange: (Int) -> Unit = {},
    content: @Composable () -> Unit
) {
    val systemInDark = isSystemInDarkTheme()
    val isDark = if (darkTheme != null) {
        darkTheme
    } else {
        when (themeMode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.SYSTEM -> systemInDark
        }
    }

    val primaryColor = Color(0xFF2563EB) // Royal Blue default
    val primaryDark = Color(0xFF3B82F6)  // Luminous blue for dark theme legibility

    val colorScheme: ColorScheme = if (isDark) {
        darkColorScheme(
            primary = primaryDark,
            onPrimary = Color.White,
            primaryContainer = Color(0xFF1E3A8A).copy(alpha = 0.5f),
            onPrimaryContainer = Color(0xFFBFDBFE),
            secondary = primaryDark,
            onSecondary = Color.White,
            background = Color(0xFF101216),       // Neutral deep charcoal/obsidian
            onBackground = Color(0xFFF8FAFC),     // Pure crisp text
            surface = Color(0xFF181B22),          // Elevated crisp dark surface
            onSurface = Color(0xFFF8FAFC),        // Pure crisp text
            surfaceVariant = Color(0xFF20242D),   // Neutral elevated container
            onSurfaceVariant = Color(0xFF94A3B8), // Readable secondary text
            outline = Color(0xFF2E3442),          // Crisp subtle border
            outlineVariant = Color(0xFF20242D)
        )
    } else {
        lightColorScheme(
            primary = primaryColor,
            onPrimary = Color.White,
            primaryContainer = primaryColor.copy(alpha = 0.12f),
            onPrimaryContainer = Color(0xFF1D4ED8),
            secondary = primaryColor,
            onSecondary = Color.White,
            background = Color(0xFFF8FAFC),       // Slate 50 daylight
            onBackground = Color(0xFF0F172A),     // Slate 900 dark text
            surface = Color.White,
            onSurface = Color(0xFF0F172A),
            surfaceVariant = Color(0xFFF1F5F9),   // Slate 100
            onSurfaceVariant = Color(0xFF64748B), // Slate 500
            outline = Color(0xFFE2E8F0),          // Slate 200
            outlineVariant = Color(0xFFF1F5F9)
        )
    }

    val themeState = AppThemeState(
        themeMode = themeMode,
        isDark = isDark,
        accentIndex = accentIndex,
        onThemeModeChange = onThemeModeChange,
        onAccentChange = onAccentChange
    )

    CompositionLocalProvider(
        LocalAppTheme provides themeState,
        LocalOverscrollConfiguration provides null
    ) {
        MaterialTheme(
            colorScheme = colorScheme
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = colorScheme.background,
                content = content
            )
        }
    }
}
