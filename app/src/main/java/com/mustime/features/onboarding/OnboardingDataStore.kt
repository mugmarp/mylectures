package com.mustime.features.onboarding

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.onboardingDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "mustime_onboarding_preferences",
    produceMigrations = { context ->
        listOf(SharedPreferencesMigration(context, "mustime_settings", keysToMigrate = setOf("onboarding_completed")))
    }
)

/**
 * DataStore manager for persisting onboarding and first-launch state.
 * Uses AndroidX Jetpack DataStore Preferences to ensure thread-safe,
 * non-blocking persistence across application restarts.
 */
class OnboardingDataStore(
    private val context: Context,
    private val customDataStore: DataStore<Preferences>? = null
) {
    companion object {
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val KEY_PROGRAMME = stringPreferencesKey("must_programme")
    }

    private val dataStore: DataStore<Preferences>
        get() = customDataStore ?: context.applicationContext.onboardingDataStore

    /**
     * Flow emitting the persisted programme from DataStore, if set.
     */
    val programmeFlow: Flow<String?> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[KEY_PROGRAMME]?.ifBlank { null }
        }
        .distinctUntilChanged()

    suspend fun setProgramme(programme: String) {
        dataStore.edit { preferences ->
            preferences[KEY_PROGRAMME] = programme
        }
    }

    /**
     * Flow emitting true when onboarding has been completed, or false on first launch.
     */
    val isOnboardingCompleted: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[KEY_ONBOARDING_COMPLETED] ?: false
        }
        .distinctUntilChanged()

    /**
     * Persist the onboarding completion state into DataStore.
     */
    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_ONBOARDING_COMPLETED] = completed
        }
    }

    /**
     * One-shot check of whether onboarding was already completed.
     */
    suspend fun checkIsOnboardingCompleted(): Boolean {
        return try {
            isOnboardingCompleted.first()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Reset onboarding state back to first launch (useful for settings / testing).
     */
    suspend fun resetOnboarding() {
        setOnboardingCompleted(false)
    }
}
