package com.mustime

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.lifecycleScope
import com.mustime.ui.AppTheme
import com.mustime.ui.MainScaffold
import com.mustime.ui.ThemeMode
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.setBackgroundDrawableResource(android.R.color.transparent)
        setContent {
            val app = application as? TimetableApplication
            val repo = app?.repository
            val initialThemeModeStr = remember { repo?.getThemeMode() ?: "LIGHT" }
            val themeModeStr by (repo?.themeModePref ?: flowOf(initialThemeModeStr)).collectAsState(initial = initialThemeModeStr)
            val accentIndex by (repo?.themeAccentPref ?: flowOf(0)).collectAsState(initial = 0)

            val themeMode = remember(themeModeStr) {
                try {
                    ThemeMode.valueOf(themeModeStr.uppercase())
                } catch (e: Exception) {
                    ThemeMode.LIGHT
                }
            }

            AppTheme(
                themeMode = themeMode,
                accentIndex = accentIndex,
                onThemeModeChange = { newMode ->
                    lifecycleScope.launch {
                        repo?.setThemeMode(newMode.name)
                    }
                },
                onAccentChange = { newAccent ->
                    lifecycleScope.launch {
                        repo?.setThemeAccent(newAccent)
                    }
                }
            ) {
                MainScaffold()
            }
        }
    }
}
