package com.lukr99.workout.domain.records

data class RecordAchievements(
    val kinds: Set<RecordKind> = emptySet(),
    val repMaxReps: Set<Int> = emptySet(),
) {
    val isPersonalRecord: Boolean get() = kinds.isNotEmpty()

    operator fun plus(other: RecordAchievements): RecordAchievements =
        RecordAchievements(kinds + other.kinds, repMaxReps + other.repMaxReps)
}
