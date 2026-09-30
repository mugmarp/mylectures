package com.mustime.features.timetable.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ETagStore(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("mustime_settings", Context.MODE_PRIVATE)

    private val PROGRAMME_KEY = "must_programme"
    private val THEME_DARK_KEY = "theme_dark"
    private val THEME_ACCENT_KEY = "theme_accent"
    private val THEME_MODE_KEY = "theme_mode"
    private val NOTIFICATIONS_ENABLED_KEY = "notifications_enabled"
    private val LAST_SYNC_KEY = "last_sync_time"
    private val ONBOARDING_COMPLETED_KEY = "onboarding_completed"
    private val ACADEMIC_YEAR_KEY = "academic_year"
    private val SEMESTER_KEY = "academic_semester"

    private val initialMode = prefs.getString(THEME_MODE_KEY, "LIGHT") ?: "LIGHT"

    private val _academicYearPref = MutableStateFlow(prefs.getString(ACADEMIC_YEAR_KEY, "2026/2027") ?: "2026/2027")
    val academicYearPref: Flow<String> = _academicYearPref.asStateFlow()

    private val _semesterPref = MutableStateFlow(prefs.getString(SEMESTER_KEY, "Semester 1") ?: "Semester 1")
    val semesterPref: Flow<String> = _semesterPref.asStateFlow()

    fun getAcademicYear(): String = prefs.getString(ACADEMIC_YEAR_KEY, "2026/2027") ?: "2026/2027"
    fun getSemester(): String = prefs.getString(SEMESTER_KEY, "Semester 1") ?: "Semester 1"

    suspend fun setAcademicSession(academicYear: String, semester: String) {
        prefs.edit()
            .putString(ACADEMIC_YEAR_KEY, academicYear)
            .putString(SEMESTER_KEY, semester)
            .apply()
        _academicYearPref.value = academicYear
        _semesterPref.value = semester
    }

    private val _programmePref = MutableStateFlow<String?>(
        prefs.getString(PROGRAMME_KEY, null)?.ifBlank { null }
    )
    val programmePref: Flow<String?> = _programmePref.asStateFlow()

    fun getSavedProgramme(): String? {
        return prefs.getString(PROGRAMME_KEY, null)?.ifBlank { null }
    }

    fun isOnboardingCompleted(): Boolean {
        return prefs.getBoolean(ONBOARDING_COMPLETED_KEY, false)
    }

    private val _onboardingCompletedPref = MutableStateFlow(prefs.getBoolean(ONBOARDING_COMPLETED_KEY, false))
    val onboardingCompletedPref: Flow<Boolean> = _onboardingCompletedPref.asStateFlow()

    private val _themeModePref = MutableStateFlow(initialMode)
    val themeModePref: Flow<String> = _themeModePref.asStateFlow()

    private val _themeDarkPref = MutableStateFlow(
        when (initialMode) {
            "DARK" -> true
            "LIGHT" -> false
            else -> prefs.getBoolean(THEME_DARK_KEY, false)
        }
    )
    val themeDarkPref: Flow<Boolean> = _themeDarkPref.asStateFlow()

    private val _themeAccentPref = MutableStateFlow(prefs.getInt(THEME_ACCENT_KEY, 0))
    val themeAccentPref: Flow<Int> = _themeAccentPref.asStateFlow()

    private val _notificationsEnabledPref = MutableStateFlow(prefs.getBoolean(NOTIFICATIONS_ENABLED_KEY, true))
    val notificationsEnabledPref: Flow<Boolean> = _notificationsEnabledPref.asStateFlow()

    private val _lastSyncPref = MutableStateFlow<String?>(prefs.getString(LAST_SYNC_KEY, "Up to date"))
    val lastSyncPref: Flow<String?> = _lastSyncPref.asStateFlow()

    suspend fun setProgrammePref(programme: String) {
        prefs.edit().putString(PROGRAMME_KEY, programme).commit()
        _programmePref.value = programme
    }

    suspend fun setThemeMode(mode: String) {
        val isDark = mode == "DARK"
        prefs.edit()
            .putString(THEME_MODE_KEY, mode)
            .putBoolean(THEME_DARK_KEY, isDark)
            .apply()
        _themeModePref.value = mode
        _themeDarkPref.value = isDark
    }

    suspend fun setThemeDark(dark: Boolean) {
        val mode = if (dark) "DARK" else "LIGHT"
        prefs.edit()
            .putString(THEME_MODE_KEY, mode)
            .putBoolean(THEME_DARK_KEY, dark)
            .apply()
        _themeDarkPref.value = dark
        _themeModePref.value = mode
    }

    suspend fun setThemeAccent(accent: Int) {
        prefs.edit().putInt(THEME_ACCENT_KEY, accent).apply()
        _themeAccentPref.value = accent
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(NOTIFICATIONS_ENABLED_KEY, enabled).apply()
        _notificationsEnabledPref.value = enabled
    }

    suspend fun setLastSyncTime(timeStr: String) {
        prefs.edit().putString(LAST_SYNC_KEY, timeStr).apply()
        _lastSyncPref.value = timeStr
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean(ONBOARDING_COMPLETED_KEY, completed).apply()
        _onboardingCompletedPref.value = completed
    }
}
