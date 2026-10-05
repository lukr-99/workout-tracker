package com.lukr99.workout.data.transfer

import com.lukr99.workout.domain.Exercise

enum class CsvColumn(val header: String) {
    SessionId("Session ID"),
    SessionName("Session"),
    StartedAt("Started At"),
    CompletedAt("Completed At"),
    SessionDurationSeconds("Session Duration Seconds"),
    SessionStatus("Session Status"),
    SessionRpe("Session RPE"),
    Bodyweight("Bodyweight"),
    ExerciseId("Exercise ID"),
    ExerciseName("Exercise"),
    Category("Category"),
    BodyPart("Body Part"),
    SupersetGroup("Superset Group"),
    SetNumber("Set Number"),
    SetType("Set Type"),
    IsWarmup("Is Warmup"),
    IsPr("Is PR"),
    Reps("Reps"),
    Weight("Weight"),
    SetDurationSeconds("Set Duration Seconds"),
    Rir("RIR"),
    Rpe("RPE"),
    Distance("Distance"),
    CardioDurationSeconds("Cardio Duration Seconds"),
    Calories("Calories"),
    Notes("Notes"),
    SetTags("Set Tags"),
}
