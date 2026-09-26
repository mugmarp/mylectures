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
    Color(0xFF2563EB), // Royal Blue
    Color(0xFF16A34A), // Forest Emerald Green
    Color(0xFFD97706), // Academic Amber
    Color(0xFFE11D48)  // Rose Crimson
)

val AccentColorNames = listOf(
    "Royal Blue",
    "Forest Emerald",
    "Academic Amber",
    "Rose Crimson"
)

enum class ThemeMode(val title: String, val subtitle: String) {
    SYSTEM("System", "Match device settings"),
    LIGHT("Light", "Clean & bright daylight"),
    DARK("Dark", "OLED slate & eye comfort")
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

    val primaryColor = AccentColors.getOrElse(accentIndex) { AccentColors[0] }

    val colorScheme: ColorScheme = if (isDark) {
        darkColorScheme(
            primary = primaryColor,
            onPrimary = Color.White,
            primaryContainer = primaryColor.copy(alpha = 0.25f),
            onPrimaryContainer = Color(0xFF93C5FD),
            secondary = primaryColor,
            onSecondary = Color.White,
            background = Color(0xFF0F172A),       // Slate 900
            onBackground = Color(0xFFF8FAFC),     // Slate 50
            surface = Color(0xFF1E293B),          // Slate 800
            onSurface = Color(0xFFF8FAFC),        // Slate 50
            surfaceVariant = Color(0xFF334155),   // Slate 700
            onSurfaceVariant = Color(0xFF94A3B8), // Slate 400
            outline = Color(0xFF475569),          // Slate 600
            outlineVariant = Color(0xFF334155)
        )
    } else {
        lightColorScheme(
            primary = primaryColor,
            onPrimary = Color.White,
            primaryContainer = primaryColor.copy(alpha = 0.12f),
            onPrimaryContainer = primaryColor,
            secondary = primaryColor,
            onSecondary = Color.White,
            background = Color(0xFFF8FAFC),       // Slate 50
            onBackground = Color(0xFF0F172A),     // Slate 900
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
