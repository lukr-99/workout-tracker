package com.lukr99.workout.settings

import kotlinx.coroutines.flow.Flow

/**
 * Settings that belong to this phone rather than to the owner's data: they stay out of backups, and
 * a restore or a settings reset leaves them alone, like the automatic backup folder.
 * [DataStoreDevicePrefs] is the Android implementation.
 */
interface DevicePrefs {
    /** Send each finished workout and run to Health Connect. On unless turned off. */
    val healthAutoSend: Flow<Boolean>

    /** Look for a new Ember release on launch, at most once a day. On unless turned off. */
    val updateAutoCheck: Flow<Boolean>

    /** When the automatic update check last ran, or null if never. */
    val lastUpdateCheckUtc: Flow<Long?>

    /** The owner put away Home's "Where do you train?" card. */
    val locationSetupDismissed: Flow<Boolean>

    /** Places where the "save this spot?" offer was already answered, so it is made once per place. */
    val positionOfferAnswered: Flow<Set<String>>

    suspend fun dismissLocationSetup()

    suspend fun setHealthAutoSend(on: Boolean)

    suspend fun setUpdateAutoCheck(on: Boolean)

    suspend fun markUpdateChecked(atUtc: Long)

    suspend fun markPositionOfferAnswered(locationId: String)
}
