package com.lukr99.workout.domain.records

data class ExerciseRecords(
    val exerciseId: String,
    val exerciseName: String,
    val heaviestSet: SetRecord?,
    val bestEstimated1Rm: SetRecord?,
    val bestSetVolume: SetRecord?,
    val bestSessionVolume: SessionVolumeRecord?,
    val repMaxes: List<RepMaxRecord>,
)
