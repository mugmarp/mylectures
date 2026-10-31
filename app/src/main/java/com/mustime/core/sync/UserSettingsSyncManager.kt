package com.mustime.core.sync

import com.mustime.features.onboarding.OnboardingDataStore
import com.mustime.features.timetable.data.ETagStore
import kotlinx.coroutines.flow.firstOrNull

/**
 * Handles exporting and importing user preferences to and from the sync engine.
 */
class UserSettingsSyncManager(
    private val etagStore: ETagStore,
    private val onboardingDataStore: OnboardingDataStore? = null
) {
    suspend fun exportCurrentSettings(): UserSettingsSyncPayload {
        val programme = etagStore.getSavedProgramme()
            ?: onboardingDataStore?.programmeFlow?.firstOrNull()

        return UserSettingsSyncPayload(
            programme = programme,
            themeMode = etagStore.getThemeMode(),
            themeAccent = etagStore.themeAccentPref.firstOrNull() ?: 0,
            notificationsEnabled = etagStore.notificationsEnabledPref.firstOrNull() ?: true,
            classAlarmLeadMinutes = etagStore.getClassAlarmLeadMinutes(),
            taskReminderLeadHours = etagStore.taskReminderLeadPref.firstOrNull() ?: 2,
            alarmVibration = etagStore.alarmVibrationPref.firstOrNull() ?: true,
            alarmSound = etagStore.getAlarmSound(),
            academicYear = etagStore.getAcademicYear(),
            semester = etagStore.getSemester(),
            updatedAt = System.currentTimeMillis()
        )
    }

    suspend fun applySettings(payload: UserSettingsSyncPayload) {
        payload.programme?.let { prog ->
            if (prog.isNotBlank()) {
                etagStore.setProgrammePref(prog)
                onboardingDataStore?.setProgramme(prog)
                onboardingDataStore?.setOnboardingCompleted(true)
            }
        }

        etagStore.setThemeMode(payload.themeMode)
        etagStore.setThemeAccent(payload.themeAccent)
        etagStore.setNotificationsEnabled(payload.notificationsEnabled)
        etagStore.setClassAlarmLeadMinutes(payload.classAlarmLeadMinutes)
        etagStore.setTaskReminderLeadHours(payload.taskReminderLeadHours)
        etagStore.setAlarmVibration(payload.alarmVibration)
        etagStore.setAlarmSound(payload.alarmSound)
        etagStore.setAcademicSession(payload.academicYear, payload.semester)
    }
}
