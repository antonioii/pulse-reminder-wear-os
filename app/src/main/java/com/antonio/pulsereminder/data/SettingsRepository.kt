package com.antonio.pulsereminder.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "pulse_reminder_settings"
)

class SettingsRepository(
    context: Context
) {
    private val dataStore = context.applicationContext.settingsDataStore

    val settings: Flow<ReminderSettings> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(androidx.datastore.preferences.core.emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            ReminderSettings(
                totalDurationMillis = preferences[TOTAL_DURATION_MILLIS] ?: DEFAULT_SETTINGS.totalDurationMillis,
                intervalMillis = preferences[INTERVAL_MILLIS] ?: DEFAULT_SETTINGS.intervalMillis,
                alertMode = preferences[ALERT_MODE]?.let(::parseAlertMode) ?: DEFAULT_SETTINGS.alertMode
            )
        }

    suspend fun saveSettings(settings: ReminderSettings) {
        dataStore.edit { preferences ->
            preferences[TOTAL_DURATION_MILLIS] = settings.totalDurationMillis
            preferences[INTERVAL_MILLIS] = settings.intervalMillis
            preferences[ALERT_MODE] = settings.alertMode.name
        }
    }

    private fun parseAlertMode(value: String): AlertMode {
        return runCatching { AlertMode.valueOf(value) }.getOrDefault(DEFAULT_SETTINGS.alertMode)
    }

    private companion object {
        val DEFAULT_SETTINGS = ReminderSettings(
            totalDurationMillis = 5 * 60 * 1000L,
            intervalMillis = 60 * 1000L,
            alertMode = AlertMode.VIBRATION
        )

        val TOTAL_DURATION_MILLIS = longPreferencesKey("total_duration_millis")
        val INTERVAL_MILLIS = longPreferencesKey("interval_millis")
        val ALERT_MODE = stringPreferencesKey("alert_mode")
    }
}
