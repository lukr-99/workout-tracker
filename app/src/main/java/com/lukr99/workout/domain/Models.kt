package com.lukr99.workout.domain

import java.util.UUID
import kotlinx.serialization.Serializable

/**
 * The portable domain model — ported from the MAUI `WorkoutTracker.Core/Domain/Models.cs`.
 *
 * These classes are the app's shared vocabulary **and** the `@Serializable` export contract
 * (see `data/export/ExportBundle.kt`). They are pure Kotlin with **no Android imports**, so the
 * analytics that operate on them stay portable toward a future desktop tool. Room persistence uses
 * a separate set of `@Entity` shapes in `data/`; the repository maps between the two.
 *
 * Timestamps are epoch-millis UTC `Long`; on the wire they become ISO-8601 strings
 * ([InstantMillisSerializer]) and enums become their Int ordinal, matching the `v1.0` export.
 */

/** MAUI `Guid.NewGuid().ToString("N")` — 32 lowercase hex chars, no dashes. */
fun newId(): String = UUID.randomUUID().toString().replace("-", "")

@Serializable
data class Exercise(
    val id: String = newId(),
    val name: String = "",
    val category: ExerciseCategory = ExerciseCategory.Strength,
    val primaryBodyPart: String = "",
    val secondaryBodyParts: List<String> = emptyList(),
    val equipment: String = "",
    val notes: String = "",
    val source: ExerciseSource = ExerciseSource.Custom,
    val externalSourceId: String? = null,
    val isArchived: Boolean = false,
    // Rework-additive (03-data-model.md).
    val defaultRestSeconds: Int? = null,
    // Optional remote/local artwork. Null keeps seeded and custom exercises offline-friendly.
    val imageUrl: String? = null,
    val imageAttribution: String? = null,
    // Exported as a reference only; the app-private image file remains device-local.
    val localImagePath: String? = null,
) {
    /** Primary + distinct secondaries, joined for display. Derived — not persisted. */
    val bodyPartsSummary: String
        get() = (listOf(primaryBodyPart) + secondaryBodyParts)
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .joinToString(" / ")
}

/** Catalog query filter (ported from MAUI `ExerciseFilter`). Applied in the repository. */
data class ExerciseFilter(
    val searchText: String = "",
    val bodyPart: String = "",
    val category: ExerciseCategory? = null,
    val equipment: String = "",
    val includeArchived: Boolean = false,
)

@Serializable
data class WorkoutTemplate(
    val id: String = newId(),
    val name: String = "",
    val notes: String = "",
    val exercises: List<WorkoutTemplateExercise> = emptyList(),
)

@Serializable
data class WorkoutTemplateExercise(
    val id: String = newId(),
    val exerciseId: String = "",
    val exerciseName: String = "",
    val category: ExerciseCategory = ExerciseCategory.Strength,
    val bodyPart: String = "",
    val sortOrder: Int = 0,
    val notes: String = "",
)

@Serializable
data class WorkoutSession(
    val id: String = newId(),
    val templateId: String? = null,
    val name: String = "",
    @Serializable(with = InstantMillisSerializer::class)
    val startedAtUtc: Long = System.currentTimeMillis(),
    @Serializable(with = InstantMillisSerializer::class)
    val endedAtUtc: Long? = null,
    @Serializable(with = InstantMillisSerializer::class)
    val completedDateUtc: Long? = null,
    val durationSeconds: Long = 0,
    val notes: String = "",
    val status: WorkoutSessionStatus = WorkoutSessionStatus.Active,
    // Rework-additive (03-data-model.md).
    val perceivedEffort: Int? = null,
    val bodyweightKg: Double? = null,
    // Export v1.2 / integration provenance. Existing 1.0/1.1 data defaults to local.
    val source: WorkoutSessionSource = WorkoutSessionSource.Local,
    val externalKey: String? = null,
    val entries: List<WorkoutEntry> = emptyList(),
)

@Serializable
data class WorkoutEntry(
    val id: String = newId(),
    val workoutSessionId: String = "",
    val exerciseId: String = "",
    val exerciseSnapshotName: String = "",
    val exerciseSnapshotCategory: ExerciseCategory = ExerciseCategory.Strength,
    val exerciseSnapshotPrimaryBodyPart: String = "",
    val sortOrder: Int = 0,
    val entryType: ExerciseCategory = ExerciseCategory.Strength,
    val notes: String = "",
    // Rework-additive (03-data-model.md).
    val supersetGroup: Int? = null,
    val strengthSets: List<StrengthSet> = emptyList(),
    val cardioData: CardioEntryData? = null,
) {
    val isStrength: Boolean get() = entryType == ExerciseCategory.Strength
}

@Serializable
data class StrengthSet(
    val id: String = newId(),
    val workoutEntryId: String = "",
    val setNumber: Int = 0,
    val reps: Int = 0,
    val weightKg: Double = 0.0,
    val rir: Double? = null,
    val rpe: Double? = null,
    @Serializable(with = InstantMillisSerializer::class)
    val performedAtUtc: Long? = null,
    val notes: String = "",
    // Rework-additive (03-data-model.md).
    val isWarmup: Boolean = false,
    val isPr: Boolean = false,
    val durationSeconds: Int? = null,
    val setType: SetType = SetType.Normal,
)

@Serializable
data class CardioEntryData(
    val workoutEntryId: String = "",
    val durationSeconds: Int = 0,
    val distanceKm: Double? = null,
    val calories: Double? = null,
    val notes: String = "",
)

// --- Projections (computed, not persisted) — ported from MAUI Models.cs -------------------------

@Serializable
data class WorkoutSessionSummary(
    val id: String = "",
    val name: String = "",
    @Serializable(with = InstantMillisSerializer::class)
    val startedAtUtc: Long = 0,
    @Serializable(with = InstantMillisSerializer::class)
    val completedDateUtc: Long? = null,
    val status: WorkoutSessionStatus = WorkoutSessionStatus.Completed,
    val exerciseCount: Int = 0,
    val strengthSetCount: Int = 0,
    val totalVolumeKg: Double = 0.0,
    val cardioMinutes: Int = 0,
    val sessionTypeLabel: String = "",
    val bodyPartsSummary: String = "",
)

@Serializable
data class AnalyticsOverview(
    val totalCompletedWorkouts: Int = 0,
    val workoutsLast30Days: Int = 0,
    val totalVolumeKg: Double = 0.0,
    val currentWeeklyStreak: Int = 0,
    val mostLoggedExerciseName: String = "",
)

@Serializable
data class WorkoutConsistencySnapshot(
    val workoutsLast7Days: Int = 0,
    val workoutsLast30Days: Int = 0,
    val currentWeeklyStreak: Int = 0,
    val longestGapDays: Int = 0,
)

@Serializable
data class ExerciseAnalyticsPoint(
    val exerciseId: String = "",
    @Serializable(with = InstantMillisSerializer::class)
    val dateUtc: Long = 0,
    val bestWeightKg: Double = 0.0,
    val totalVolumeKg: Double = 0.0,
    val totalReps: Int = 0,
    val sessionName: String = "",
)

data class DashboardSnapshot(
    val activeWorkout: WorkoutSession? = null,
    val analytics: AnalyticsOverview = AnalyticsOverview(),
    val consistency: WorkoutConsistencySnapshot = WorkoutConsistencySnapshot(),
    val recentWorkouts: List<WorkoutSessionSummary> = emptyList(),
)
