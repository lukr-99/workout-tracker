package com.lukr99.workout.data

import com.lukr99.workout.data.export.SettingsSnapshot
import com.lukr99.workout.data.transfer.SettingsArchive

/** A [SettingsArchive] that only holds a snapshot, for transfer tests. */
class MemorySettingsArchive(var current: SettingsSnapshot = SettingsSnapshot()) : SettingsArchive {
    override suspend fun snapshot(): SettingsSnapshot = current
    override suspend fun restore(snapshot: SettingsSnapshot) {
        current = snapshot
    }
    override suspend fun reset() {
        current = SettingsSnapshot()
    }
}
