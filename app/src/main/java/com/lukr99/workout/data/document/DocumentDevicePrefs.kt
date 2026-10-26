package com.lukr99.workout.data.document

import com.lukr99.workout.settings.DevicePrefs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * The [DevicePrefs] over a [DocumentDatabase]: this browser's own switches, kept in the document
 * beside the workout data but never put into a backup, and left alone by a restore or a reset.
 */
class DocumentDevicePrefs(private val database: DocumentDatabase) : DevicePrefs {

    private fun <T> read(select: (DevicePrefsSnapshot) -> T): Flow<T> =
        database.document.map { select(it.devicePrefs) }.distinctUntilChanged()

    private suspend fun change(edit: (DevicePrefsSnapshot) -> DevicePrefsSnapshot) =
        database.update { it.copy(devicePrefs = edit(it.devicePrefs)) }

    override val healthAutoSend: Flow<Boolean> = read { it.healthAutoSend }

    override val updateAutoCheck: Flow<Boolean> = read { it.updateAutoCheck }

    override val lastUpdateCheckUtc: Flow<Long?> = read { it.lastUpdateCheckUtc }

    override val locationSetupDismissed: Flow<Boolean> = read { it.locationSetupDismissed }

    override val positionOfferAnswered: Flow<Set<String>> = read { it.positionOfferAnswered }

    override suspend fun dismissLocationSetup() = change { it.copy(locationSetupDismissed = true) }

    override suspend fun setHealthAutoSend(on: Boolean) = change { it.copy(healthAutoSend = on) }

    override suspend fun setUpdateAutoCheck(on: Boolean) = change { it.copy(updateAutoCheck = on) }

    override suspend fun markUpdateChecked(atUtc: Long) = change { it.copy(lastUpdateCheckUtc = atUtc) }

    override suspend fun markPositionOfferAnswered(locationId: String) =
        change { it.copy(positionOfferAnswered = it.positionOfferAnswered + locationId) }
}
