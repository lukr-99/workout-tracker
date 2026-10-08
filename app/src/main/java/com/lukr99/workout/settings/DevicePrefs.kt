package com.lukr99.workout.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.devicePrefsDataStore by preferencesDataStore(name = "device_prefs")

/**
 * Settings that belong to this phone rather than to the owner's data: they stay out of backups, and
 * a restore or a settings reset leaves them alone, like the automatic backup folder.
 */
class DevicePrefs(private val context: Context) {

    /** Send each finished workout and run to Health Connect. On unless turned off. */
    val healthAutoSend: Flow<Boolean> = context.devicePrefsDataStore.data.map { it[HealthAutoSend] ?: true }

    /** Look for a new Ember release on launch, at most once a day. On unless turned off. */
    val updateAutoCheck: Flow<Boolean> = context.devicePrefsDataStore.data.map { it[UpdateAutoCheck] ?: true }

    /** When the automatic update check last ran, or null if never. */
    val lastUpdateCheckUtc: Flow<Long?> = context.devicePrefsDataStore.data.map { it[LastUpdateCheck] }

    suspend fun setHealthAutoSend(on: Boolean) {
        context.devicePrefsDataStore.edit { it[HealthAutoSend] = on }
    }

    suspend fun setUpdateAutoCheck(on: Boolean) {
        context.devicePrefsDataStore.edit { it[UpdateAutoCheck] = on }
    }

    suspend fun markUpdateChecked(atUtc: Long) {
        context.devicePrefsDataStore.edit { it[LastUpdateCheck] = atUtc }
    }

    private companion object {
        val HealthAutoSend = booleanPreferencesKey("health_auto_send")
        val UpdateAutoCheck = booleanPreferencesKey("update_auto_check")
        val LastUpdateCheck = longPreferencesKey("last_update_check_utc")
    }
}
