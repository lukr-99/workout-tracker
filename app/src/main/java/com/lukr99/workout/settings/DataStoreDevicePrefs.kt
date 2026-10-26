package com.lukr99.workout.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.devicePrefsDataStore by preferencesDataStore(name = "device_prefs")

/** The AndroidX DataStore [DevicePrefs]: one `Preferences` file named "device_prefs". */
class DataStoreDevicePrefs(private val context: Context) : DevicePrefs {

    override val healthAutoSend: Flow<Boolean> = context.devicePrefsDataStore.data.map { it[HealthAutoSend] ?: true }

    override val updateAutoCheck: Flow<Boolean> = context.devicePrefsDataStore.data.map { it[UpdateAutoCheck] ?: true }

    override val lastUpdateCheckUtc: Flow<Long?> = context.devicePrefsDataStore.data.map { it[LastUpdateCheck] }

    override val locationSetupDismissed: Flow<Boolean> =
        context.devicePrefsDataStore.data.map { it[LocationSetupDismissed] ?: false }

    override suspend fun dismissLocationSetup() {
        context.devicePrefsDataStore.edit { it[LocationSetupDismissed] = true }
    }

    override suspend fun setHealthAutoSend(on: Boolean) {
        context.devicePrefsDataStore.edit { it[HealthAutoSend] = on }
    }

    override suspend fun setUpdateAutoCheck(on: Boolean) {
        context.devicePrefsDataStore.edit { it[UpdateAutoCheck] = on }
    }

    override suspend fun markUpdateChecked(atUtc: Long) {
        context.devicePrefsDataStore.edit { it[LastUpdateCheck] = atUtc }
    }

    override val positionOfferAnswered: Flow<Set<String>> =
        context.devicePrefsDataStore.data.map { it[PositionOfferAnswered] ?: emptySet() }

    override suspend fun markPositionOfferAnswered(locationId: String) {
        context.devicePrefsDataStore.edit { it[PositionOfferAnswered] = (it[PositionOfferAnswered] ?: emptySet()) + locationId }
    }

    private companion object {
        val PositionOfferAnswered = stringSetPreferencesKey("position_offer_answered")
        val HealthAutoSend = booleanPreferencesKey("health_auto_send")
        val UpdateAutoCheck = booleanPreferencesKey("update_auto_check")
        val LastUpdateCheck = longPreferencesKey("last_update_check_utc")
        val LocationSetupDismissed = booleanPreferencesKey("location_setup_dismissed")
    }
}
