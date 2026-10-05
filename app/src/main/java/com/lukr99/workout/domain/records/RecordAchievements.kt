package com.lukr99.workout.domain.records

data class RecordAchievements(
    val kinds: Set<RecordKind> = emptySet(),
    val repMaxReps: Set<Int> = emptySet(),
) {
    val isPersonalRecord: Boolean get() = kinds.isNotEmpty()

    /** The strongest record in plain words: e1RM beats heaviest set, which beats volume. */
    val headline: String
        get() = when {
            RecordKind.Estimated1Rm in kinds -> "New estimated 1RM"
            RecordKind.HeaviestSet in kinds -> "Heaviest set ever"
            repMaxReps.isNotEmpty() -> "New ${repMaxReps.min()}-rep max"
            RecordKind.SetVolume in kinds -> "Best set volume"
            RecordKind.SessionVolume in kinds -> "Best session volume"
            else -> "New personal record"
        }

    operator fun plus(other: RecordAchievements): RecordAchievements =
        RecordAchievements(kinds + other.kinds, repMaxReps + other.repMaxReps)
}
