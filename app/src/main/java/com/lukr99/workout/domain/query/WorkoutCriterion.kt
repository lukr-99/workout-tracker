package com.lukr99.workout.domain.query

import com.lukr99.workout.domain.ExerciseCategory
import com.lukr99.workout.domain.SetType
import com.lukr99.workout.domain.WorkoutSessionStatus

sealed interface WorkoutCriterion {
    data class SessionIds(val values: Set<String>) : WorkoutCriterion
    data class Statuses(val values: Set<WorkoutSessionStatus>) : WorkoutCriterion
    data class StartedBetween(val fromInclusiveUtc: Long? = null, val toExclusiveUtc: Long? = null) :
        WorkoutCriterion
    data class SessionNameContains(val value: String) : WorkoutCriterion
    data class TemplateIds(val values: Set<String?>) : WorkoutCriterion
    data class ExerciseIds(val values: Set<String>) : WorkoutCriterion
    data class ExerciseNames(val values: Set<String>) : WorkoutCriterion
    data class ExerciseNameContains(val value: String) : WorkoutCriterion
    data class BodyParts(val values: Set<String>) : WorkoutCriterion
    data class Categories(val values: Set<ExerciseCategory>) : WorkoutCriterion
    data class SupersetGroups(val values: Set<Int>) : WorkoutCriterion
    data class SetTypes(val values: Set<SetType>) : WorkoutCriterion
    data class Warmup(val included: Boolean) : WorkoutCriterion
    data class Pr(val included: Boolean) : WorkoutCriterion
    data class RepsBetween(val minimum: Int? = null, val maximum: Int? = null) : WorkoutCriterion
    data class WeightBetweenKg(val minimum: Double? = null, val maximum: Double? = null) :
        WorkoutCriterion
    data class HasTimedWork(val value: Boolean = true) : WorkoutCriterion
    data class HasCardio(val value: Boolean = true) : WorkoutCriterion
}
