package com.lukr99.workout.data.transfer

import com.lukr99.workout.domain.run.Route
import com.lukr99.workout.domain.run.Run

data class ImportPlan(
    val format: DataFormat,
    val exercises: List<PlannedExercise> = emptyList(),
    val templates: List<PlannedTemplate> = emptyList(),
    val sessions: List<PlannedSession> = emptyList(),
    /** Runs to insert (those whose id isn't already present); restored as-is with their traces. */
    val runs: List<Run> = emptyList(),
    /** Saved routes to insert (those whose id isn't already present). */
    val routes: List<Route> = emptyList(),
    val issues: List<TransferIssue> = emptyList(),
    val sourceLabel: String? = null,
    val mode: RestoreMode = RestoreMode.Merge,
    /** Backup id of each exercise -> the id it will have in the store (they differ on a merge). */
    val exerciseIdMap: Map<String, String> = emptyMap(),
    /** Personal photos from the backup, keyed by their backup exercise id. */
    val photos: List<com.lukr99.workout.data.export.ExercisePhoto> = emptyList(),
    /** Settings from the backup; only a replace-restore applies them. */
    val settings: com.lukr99.workout.data.export.SettingsSnapshot? = null,
    /** On a replace-restore, what the store holds now and will lose. */
    val replaces: StoreCounts? = null,
)
