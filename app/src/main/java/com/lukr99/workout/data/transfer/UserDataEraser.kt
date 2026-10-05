package com.lukr99.workout.data.transfer

import com.lukr99.workout.data.WorkoutRepository
import com.lukr99.workout.data.run.RunRepository

/**
 * "Delete all data": the store goes back to a fresh install. Workouts, templates, the catalog, runs,
 * routes, personal photos and settings are removed, and the starter catalog is seeded again.
 *
 * Automatic backup is turned off first. Otherwise it would keep writing empty backups and, after a
 * few runs, delete the last good ones under its retention rule. Data already sent to Health Connect
 * stays there.
 */
class UserDataEraser(
    private val repository: WorkoutRepository,
    private val runRepository: RunRepository,
    private val photos: PhotoArchive,
    private val settings: SettingsArchive,
    private val stopAutomaticBackup: suspend () -> Unit,
    private val isRunRecording: () -> Boolean,
) {
    /** Why erasing is not possible right now, or null when it is. */
    suspend fun blocker(): String? = when {
        repository.getActiveSession() != null -> "Finish or discard the live workout first."
        isRunRecording() -> "Finish the run you are recording first."
        else -> null
    }

    suspend fun eraseAll() {
        blocker()?.let { error(it) }
        stopAutomaticBackup()
        repository.inTransaction {
            deleteAllWorkoutData()
            runRepository.deleteAll()
        }
        photos.deleteAllExcept(emptySet())
        settings.reset()
        repository.ensureSeeded()
    }
}
