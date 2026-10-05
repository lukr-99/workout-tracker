package com.lukr99.workout.data.transfer

import com.lukr99.workout.domain.query.WorkoutQuery
import com.lukr99.workout.domain.run.Run

data class JsonExportOptions(
    val query: WorkoutQuery = WorkoutQuery(includeEmptySessions = true),
    val includeExercises: Boolean = true,
    val includeTemplates: Boolean = true,
    val includeUnreferencedExercises: Boolean = true,
    val includeDiscardedSessions: Boolean = false,
    /** Include Run Mode data (runs with traces + saved routes) in the bundle. */
    val includeRuns: Boolean = true,
    /** The owner's settings (theme, units, rest), restored by a replace-restore. */
    val includeSettings: Boolean = true,
    /** Personal exercise photos, scaled down to fit (see ExercisePhoto). */
    val includePhotos: Boolean = true,
    val fileName: String = "workout-backup.json",
)
