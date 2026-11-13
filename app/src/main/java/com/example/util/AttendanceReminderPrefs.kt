package com.example.util

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.reminderDataStore by preferencesDataStore(name = "kgn_reminder_prefs")

data class AttendanceReminderSettings(
    val enabled: Boolean = true,
    val hour: Int = 18, // 6 PM IST
    val minute: Int = 30 // 6:30 PM IST
)

object AttendanceReminderPrefs {
    private val KEY_ENABLED = booleanPreferencesKey("daily_attendance_reminder_enabled")
    private val KEY_HOUR = intPreferencesKey("daily_attendance_reminder_hour")
    private val KEY_MINUTE = intPreferencesKey("daily_attendance_reminder_minute")

    fun observeSettings(context: Context): Flow<AttendanceReminderSettings> {
        return context.applicationContext.reminderDataStore.data.map { prefs ->
            AttendanceReminderSettings(
                enabled = prefs[KEY_ENABLED] ?: true,
                hour = (prefs[KEY_HOUR] ?: 18).coerceIn(0, 23),
                minute = (prefs[KEY_MINUTE] ?: 30).coerceIn(0, 59)
            )
        }
    }

    suspend fun getSettingsSync(context: Context): AttendanceReminderSettings {
        return observeSettings(context).first()
    }

    suspend fun setEnabled(context: Context, enabled: Boolean) {
        context.applicationContext.reminderDataStore.edit { prefs ->
            prefs[KEY_ENABLED] = enabled
        }
    }

    suspend fun setTime(context: Context, hour: Int, minute: Int) {
        context.applicationContext.reminderDataStore.edit { prefs ->
            prefs[KEY_HOUR] = hour.coerceIn(0, 23)
            prefs[KEY_MINUTE] = minute.coerceIn(0, 59)
        }
    }
}
