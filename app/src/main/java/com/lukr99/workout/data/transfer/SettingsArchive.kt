package com.lukr99.workout.data.transfer

import com.lukr99.workout.data.export.SettingsSnapshot

/** Reads, restores and resets the owner's settings for backup, replace-restore and delete-all. */
interface SettingsArchive {
    suspend fun snapshot(): SettingsSnapshot
    suspend fun restore(snapshot: SettingsSnapshot)
    suspend fun reset()
}
