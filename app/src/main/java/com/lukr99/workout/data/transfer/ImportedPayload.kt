package com.lukr99.workout.data.transfer

import com.lukr99.workout.domain.Exercise
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.domain.WorkoutTemplate
import com.lukr99.workout.domain.run.Route
import com.lukr99.workout.domain.run.Run

data class ImportedPayload(
    val format: DataFormat,
    val exercises: List<Exercise> = emptyList(),
    val templates: List<WorkoutTemplate> = emptyList(),
    val sessions: List<WorkoutSession> = emptyList(),
    val runs: List<Run> = emptyList(),
    val routes: List<Route> = emptyList(),
    val photos: List<com.lukr99.workout.data.export.ExercisePhoto> = emptyList(),
    val settings: com.lukr99.workout.data.export.SettingsSnapshot? = null,
    val issues: List<TransferIssue> = emptyList(),
    val sourceRows: Int = 0,
    val sourceLabel: String? = null,
    val metadata: Map<String, String> = emptyMap(),
)
