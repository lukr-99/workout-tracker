package com.lukr99.workout.domain.query

import com.lukr99.workout.domain.CardioEntryData
import com.lukr99.workout.domain.StrengthSet
import com.lukr99.workout.domain.WorkoutEntry
import com.lukr99.workout.domain.WorkoutSession

data class WorkoutDataPoint(
    val session: WorkoutSession,
    val entry: WorkoutEntry? = null,
    val strengthSet: StrengthSet? = null,
    val cardio: CardioEntryData? = null,
)
