package com.lukr99.workout.domain.creation

import com.lukr99.workout.domain.CardioEntryData
import com.lukr99.workout.domain.Exercise
import com.lukr99.workout.domain.ExerciseCategory
import com.lukr99.workout.domain.ExerciseSource
import com.lukr99.workout.domain.SetType
import com.lukr99.workout.domain.StrengthSet
import com.lukr99.workout.domain.WorkoutEntry
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.domain.WorkoutSessionSource
import com.lukr99.workout.domain.WorkoutSessionStatus
import com.lukr99.workout.domain.WorkoutTemplate
import com.lukr99.workout.domain.WorkoutTemplateExercise
import com.lukr99.workout.domain.newId

/**
 * Pure, reusable creation boundary shared by UI actions, imports, tests, and future sync adapters.
 * Drafts accept partial input; [CreationPolicy] controls how aggressively it is normalized.
 */
class WorkoutFactory(
    private val ids: IdGenerator = IdGenerator { newId() },
    private val clock: TimeProvider = TimeProvider { System.currentTimeMillis() },
) {
    fun exercise(
        draft: ExerciseDraft,
        policy: CreationPolicy = CreationPolicy(),
    ): CreationResult<Exercise> {
        val issues = mutableListOf<ValidationIssue>()
        val name = draft.name.trim().ifBlank {
            issues += ValidationIssue("exercise.name", "Name is required.", IssueSeverity.Error)
            policy.fallbackExerciseName
        }
        val primaryBodyPart = draft.primaryBodyPart.trim().ifBlank {
            if (draft.category == ExerciseCategory.Cardio) "Cardio" else policy.fallbackBodyPart
        }
        val rest = draft.defaultRestSeconds?.also {
            if (it !in policy.restSecondsRange) {
                issues += ValidationIssue(
                    "exercise.defaultRestSeconds",
                    "Rest must be within ${policy.restSecondsRange}.",
                    IssueSeverity.Error,
                )
            }
        }?.coerceIn(policy.restSecondsRange)

        return CreationResult(
            value = Exercise(
                id = draft.id.ifBlank(ids::next),
                name = name,
                category = draft.category,
                primaryBodyPart = primaryBodyPart,
                secondaryBodyParts = draft.secondaryBodyParts
                    .map(String::trim)
                    .filter(String::isNotBlank)
                    .filterNot { it.equals(primaryBodyPart, ignoreCase = true) }
                    .distinctBy(String::lowercase),
                equipment = draft.equipment.trim(),
                notes = draft.notes.trim(),
                source = draft.source,
                externalSourceId = draft.externalSourceId?.trim()?.ifBlank { null },
                isArchived = draft.isArchived,
                defaultRestSeconds = rest,
                imageUrl = draft.imageUrl?.trim()?.ifBlank { null },
                imageAttribution = draft.imageAttribution?.trim()?.ifBlank { null },
                localImagePath = draft.localImagePath?.trim()?.ifBlank { null },
            ),
            issues = issues,
        )
    }

    fun template(
        draft: TemplateDraft,
        catalog: Map<String, Exercise> = emptyMap(),
        policy: CreationPolicy = CreationPolicy(),
    ): CreationResult<WorkoutTemplate> {
        val issues = mutableListOf<ValidationIssue>()
        val templateId = draft.id.ifBlank(ids::next)
        val children = draft.exercises.mapIndexed { index, item ->
            val exercise = catalog[item.exerciseId]
            if (exercise == null && item.exerciseName.isBlank()) {
                issues += ValidationIssue(
                    "template.exercises[$index]",
                    "An exercise id or snapshot name is required.",
                    IssueSeverity.Error,
                )
            }
            WorkoutTemplateExercise(
                id = item.id.ifBlank(ids::next),
                exerciseId = item.exerciseId.ifBlank { exercise?.id.orEmpty() },
                exerciseName = item.exerciseName.trim().ifBlank { exercise?.name ?: "Exercise" },
                category = exercise?.category ?: item.category,
                bodyPart = item.bodyPart.trim().ifBlank { exercise?.primaryBodyPart.orEmpty() },
                sortOrder = index,
                notes = item.notes.trim(),
            )
        }
        if (children.isEmpty() && policy.requireTemplateExercises) {
            issues += ValidationIssue(
                "template.exercises",
                "At least one exercise is required.",
                IssueSeverity.Error,
            )
        }

        return CreationResult(
            WorkoutTemplate(
                id = templateId,
                name = draft.name.trim().ifBlank { policy.fallbackTemplateName },
                notes = draft.notes.trim(),
                exercises = children,
            ),
            issues,
        )
    }

    fun session(
        draft: SessionDraft,
        catalog: Map<String, Exercise> = emptyMap(),
        policy: CreationPolicy = CreationPolicy(),
    ): CreationResult<WorkoutSession> {
        val issues = mutableListOf<ValidationIssue>()
        val sessionId = draft.id.ifBlank(ids::next)
        val startedAt = draft.startedAtUtc ?: clock.now()
        val endedAt = draft.endedAtUtc ?: when (draft.status) {
            WorkoutSessionStatus.Active -> null
            else -> startedAt + (draft.durationSeconds.coerceAtLeast(0) * 1_000)
        }
        val duration = when {
            draft.durationSeconds > 0 -> draft.durationSeconds
            endedAt != null -> ((endedAt - startedAt) / 1_000).coerceAtLeast(0)
            else -> 0
        }

        if (draft.perceivedEffort != null && draft.perceivedEffort !in 1..10) {
            issues += ValidationIssue(
                "session.perceivedEffort",
                "Perceived effort must be from 1 to 10.",
                IssueSeverity.Error,
            )
        }
        if (draft.bodyweightKg != null && draft.bodyweightKg <= 0) {
            issues += ValidationIssue(
                "session.bodyweightKg",
                "Bodyweight must be positive.",
                IssueSeverity.Error,
            )
        }

        val entries = draft.entries.mapIndexed { index, item ->
            createEntry(sessionId, index, item, catalog[item.exerciseId], policy, issues)
        }
        if (entries.isEmpty() && policy.requireSessionEntries) {
            issues += ValidationIssue(
                "session.entries",
                "At least one exercise is required.",
                IssueSeverity.Error,
            )
        }

        return CreationResult(
            WorkoutSession(
                id = sessionId,
                templateId = draft.templateId?.ifBlank { null },
                name = draft.name.trim().ifBlank { policy.fallbackSessionName },
                startedAtUtc = startedAt,
                endedAtUtc = endedAt,
                completedDateUtc = draft.completedDateUtc
                    ?: endedAt?.takeIf { draft.status == WorkoutSessionStatus.Completed },
                durationSeconds = duration,
                notes = draft.notes.trim(),
                status = draft.status,
                perceivedEffort = draft.perceivedEffort?.coerceIn(1, 10),
                bodyweightKg = draft.bodyweightKg?.takeIf { it > 0 },
                source = draft.source,
                externalKey = draft.externalKey?.trim()?.takeIf(String::isNotBlank),
                entries = entries,
            ),
            issues,
        )
    }

    private fun createEntry(
        sessionId: String,
        index: Int,
        draft: EntryDraft,
        exercise: Exercise?,
        policy: CreationPolicy,
        issues: MutableList<ValidationIssue>,
    ): WorkoutEntry {
        val entryId = draft.id.ifBlank(ids::next)
        val category = exercise?.category ?: draft.category
        val snapshotName = draft.exerciseName.trim().ifBlank { exercise?.name ?: "Exercise" }
        if (draft.exerciseId.isBlank() && exercise == null && policy.requireCatalogExercise) {
            issues += ValidationIssue(
                "session.entries[$index].exerciseId",
                "A catalog exercise is required.",
                IssueSeverity.Error,
            )
        }

        val strengthSets = if (category == ExerciseCategory.Strength) {
            draft.strengthSets.mapIndexed { setIndex, set ->
                if (set.reps < 0) {
                    issues += ValidationIssue(
                        "session.entries[$index].sets[$setIndex].reps",
                        "Reps cannot be negative.",
                        IssueSeverity.Error,
                    )
                }
                if (set.weightKg < 0) {
                    issues += ValidationIssue(
                        "session.entries[$index].sets[$setIndex].weightKg",
                        "Weight cannot be negative.",
                        IssueSeverity.Error,
                    )
                }
                val type = set.setType
                StrengthSet(
                    id = set.id.ifBlank(ids::next),
                    workoutEntryId = entryId,
                    setNumber = setIndex + 1,
                    reps = set.reps.coerceAtLeast(0),
                    weightKg = set.weightKg.coerceAtLeast(0.0),
                    rir = set.rir,
                    rpe = set.rpe,
                    performedAtUtc = set.performedAtUtc,
                    notes = set.notes.trim(),
                    isWarmup = set.isWarmup || type == SetType.Warmup,
                    isPr = set.isPr,
                    durationSeconds = set.durationSeconds?.coerceAtLeast(0),
                    setType = type,
                )
            }
        } else {
            if (draft.strengthSets.isNotEmpty()) {
                issues += ValidationIssue(
                    "session.entries[$index].sets",
                    "Strength sets were ignored for a cardio entry.",
                    IssueSeverity.Warning,
                )
            }
            emptyList()
        }

        val cardio = if (category == ExerciseCategory.Cardio) {
            val raw = draft.cardio ?: CardioDraft()
            CardioEntryData(
                workoutEntryId = entryId,
                durationSeconds = raw.durationSeconds.coerceAtLeast(0),
                distanceKm = raw.distanceKm?.coerceAtLeast(0.0),
                calories = raw.calories?.coerceAtLeast(0.0),
                notes = raw.notes.trim(),
            )
        } else {
            if (draft.cardio != null) {
                issues += ValidationIssue(
                    "session.entries[$index].cardio",
                    "Cardio data was ignored for a strength entry.",
                    IssueSeverity.Warning,
                )
            }
            null
        }

        return WorkoutEntry(
            id = entryId,
            workoutSessionId = sessionId,
            exerciseId = exercise?.id ?: draft.exerciseId,
            exerciseSnapshotName = snapshotName,
            exerciseSnapshotCategory = category,
            exerciseSnapshotPrimaryBodyPart = draft.bodyPart.trim()
                .ifBlank { exercise?.primaryBodyPart.orEmpty() },
            sortOrder = index,
            entryType = category,
            notes = draft.notes.trim(),
            supersetGroup = draft.supersetGroup,
            strengthSets = strengthSets,
            cardioData = cardio,
        )
    }
}

fun interface IdGenerator {
    fun next(): String
}

fun interface TimeProvider {
    fun now(): Long
}

data class CreationPolicy(
    val requireCatalogExercise: Boolean = false,
    val requireSessionEntries: Boolean = false,
    val requireTemplateExercises: Boolean = false,
    val fallbackExerciseName: String = "Custom Exercise",
    val fallbackBodyPart: String = "Full Body",
    val fallbackTemplateName: String = "Untitled Template",
    val fallbackSessionName: String = "Quick Workout",
    val restSecondsRange: IntRange = 0..3_600,
)

data class CreationResult<T>(
    val value: T,
    val issues: List<ValidationIssue> = emptyList(),
) {
    val isValid: Boolean get() = issues.none { it.severity == IssueSeverity.Error }

    fun requireValid(): T {
        require(isValid) { issues.filter { it.severity == IssueSeverity.Error }.joinToString { it.message } }
        return value
    }
}

data class ValidationIssue(
    val path: String,
    val message: String,
    val severity: IssueSeverity,
)

enum class IssueSeverity { Info, Warning, Error }

data class ExerciseDraft(
    val id: String = "",
    val name: String = "",
    val category: ExerciseCategory = ExerciseCategory.Strength,
    val primaryBodyPart: String = "",
    val secondaryBodyParts: List<String> = emptyList(),
    val equipment: String = "",
    val notes: String = "",
    val source: ExerciseSource = ExerciseSource.Custom,
    val externalSourceId: String? = null,
    val isArchived: Boolean = false,
    val defaultRestSeconds: Int? = null,
    val imageUrl: String? = null,
    val imageAttribution: String? = null,
    val localImagePath: String? = null,
)

data class TemplateDraft(
    val id: String = "",
    val name: String = "",
    val notes: String = "",
    val exercises: List<TemplateExerciseDraft> = emptyList(),
)

data class TemplateExerciseDraft(
    val id: String = "",
    val exerciseId: String = "",
    val exerciseName: String = "",
    val category: ExerciseCategory = ExerciseCategory.Strength,
    val bodyPart: String = "",
    val notes: String = "",
)

data class SessionDraft(
    val id: String = "",
    val templateId: String? = null,
    val name: String = "",
    val startedAtUtc: Long? = null,
    val endedAtUtc: Long? = null,
    val completedDateUtc: Long? = null,
    val durationSeconds: Long = 0,
    val notes: String = "",
    val status: WorkoutSessionStatus = WorkoutSessionStatus.Active,
    val perceivedEffort: Int? = null,
    val bodyweightKg: Double? = null,
    val source: WorkoutSessionSource = WorkoutSessionSource.Local,
    val externalKey: String? = null,
    val entries: List<EntryDraft> = emptyList(),
)

data class EntryDraft(
    val id: String = "",
    val exerciseId: String = "",
    val exerciseName: String = "",
    val category: ExerciseCategory = ExerciseCategory.Strength,
    val bodyPart: String = "",
    val notes: String = "",
    val supersetGroup: Int? = null,
    val strengthSets: List<StrengthSetDraft> = emptyList(),
    val cardio: CardioDraft? = null,
)

data class StrengthSetDraft(
    val id: String = "",
    val reps: Int = 0,
    val weightKg: Double = 0.0,
    val rir: Double? = null,
    val rpe: Double? = null,
    val performedAtUtc: Long? = null,
    val notes: String = "",
    val isWarmup: Boolean = false,
    val isPr: Boolean = false,
    val durationSeconds: Int? = null,
    val setType: SetType = SetType.Normal,
)

data class CardioDraft(
    val durationSeconds: Int = 0,
    val distanceKm: Double? = null,
    val calories: Double? = null,
    val notes: String = "",
)
