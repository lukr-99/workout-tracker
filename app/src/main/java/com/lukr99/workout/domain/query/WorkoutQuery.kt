package com.lukr99.workout.domain.query

import com.lukr99.workout.domain.CardioEntryData
import com.lukr99.workout.domain.ExerciseCategory
import com.lukr99.workout.domain.SetType
import com.lukr99.workout.domain.StrengthSet
import com.lukr99.workout.domain.WorkoutEntry
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.domain.WorkoutSessionStatus

/**
 * A composable, storage-agnostic query tree. The same query can drive stats, CSV exports,
 * dashboards, future sync selection, or an in-memory preview without adding one-off repository
 * methods for every filter combination.
 */
data class WorkoutQuery(
    val filter: WorkoutFilter = WorkoutFilter.All,
    val includeEmptySessions: Boolean = false,
    val includeEmptyEntries: Boolean = false,
)

sealed interface WorkoutFilter {
    data object All : WorkoutFilter
    data object None : WorkoutFilter
    data class Criterion(val criterion: WorkoutCriterion) : WorkoutFilter
    data class And(val filters: List<WorkoutFilter>) : WorkoutFilter
    data class Or(val filters: List<WorkoutFilter>) : WorkoutFilter
    data class Not(val filter: WorkoutFilter) : WorkoutFilter
}

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

infix fun WorkoutFilter.and(other: WorkoutFilter): WorkoutFilter = when {
    this == WorkoutFilter.All -> other
    other == WorkoutFilter.All -> this
    this == WorkoutFilter.None || other == WorkoutFilter.None -> WorkoutFilter.None
    this is WorkoutFilter.And && other is WorkoutFilter.And -> WorkoutFilter.And(filters + other.filters)
    this is WorkoutFilter.And -> WorkoutFilter.And(filters + other)
    other is WorkoutFilter.And -> WorkoutFilter.And(listOf(this) + other.filters)
    else -> WorkoutFilter.And(listOf(this, other))
}

infix fun WorkoutFilter.or(other: WorkoutFilter): WorkoutFilter = when {
    this == WorkoutFilter.None -> other
    other == WorkoutFilter.None -> this
    this == WorkoutFilter.All || other == WorkoutFilter.All -> WorkoutFilter.All
    this is WorkoutFilter.Or && other is WorkoutFilter.Or -> WorkoutFilter.Or(filters + other.filters)
    this is WorkoutFilter.Or -> WorkoutFilter.Or(filters + other)
    other is WorkoutFilter.Or -> WorkoutFilter.Or(listOf(this) + other.filters)
    else -> WorkoutFilter.Or(listOf(this, other))
}

operator fun WorkoutFilter.not(): WorkoutFilter = WorkoutFilter.Not(this)

fun WorkoutCriterion.asFilter(): WorkoutFilter = WorkoutFilter.Criterion(this)

data class WorkoutDataPoint(
    val session: WorkoutSession,
    val entry: WorkoutEntry? = null,
    val strengthSet: StrengthSet? = null,
    val cardio: CardioEntryData? = null,
)

data class WorkoutSelection(
    val points: List<WorkoutDataPoint>,
    val matchedSessionIds: Set<String>,
    val matchedEntryIds: Set<String>,
) {
    val sessions: List<WorkoutSession>
        get() = points.distinctBy { it.session.id }.map { it.session }
}

object WorkoutQueryEngine {
    fun select(
        sessions: Iterable<WorkoutSession>,
        query: WorkoutQuery = WorkoutQuery(),
    ): WorkoutSelection {
        val selected = buildList {
            for (session in sessions) {
                val sessionPoints = buildList {
                    for (entry in session.entries) {
                        val entryPoints = buildList {
                            entry.strengthSets.forEach { set ->
                                val point = WorkoutDataPoint(session, entry, strengthSet = set)
                                if (query.filter.matches(point)) add(point)
                            }
                            entry.cardioData?.let { cardio ->
                                val point = WorkoutDataPoint(session, entry, cardio = cardio)
                                if (query.filter.matches(point)) add(point)
                            }
                            if (entry.strengthSets.isEmpty() && entry.cardioData == null) {
                                val point = WorkoutDataPoint(session, entry)
                                if (query.includeEmptyEntries && query.filter.matches(point)) add(point)
                            }
                        }
                        addAll(entryPoints)
                    }
                }

                if (sessionPoints.isNotEmpty()) {
                    addAll(sessionPoints)
                } else if (query.includeEmptySessions) {
                    val point = WorkoutDataPoint(session)
                    if (query.filter.matches(point)) add(point)
                }
            }
        }
        return WorkoutSelection(
            points = selected,
            matchedSessionIds = selected.mapTo(linkedSetOf()) { it.session.id },
            matchedEntryIds = selected.mapNotNullTo(linkedSetOf()) { it.entry?.id },
        )
    }

    fun filterSessions(
        sessions: Iterable<WorkoutSession>,
        query: WorkoutQuery = WorkoutQuery(includeEmptySessions = true),
    ): List<WorkoutSession> {
        val ids = select(sessions, query).matchedSessionIds
        return sessions.filter { it.id in ids }
    }

    private fun WorkoutFilter.matches(point: WorkoutDataPoint): Boolean = when (this) {
        WorkoutFilter.All -> true
        WorkoutFilter.None -> false
        is WorkoutFilter.Criterion -> criterion.matches(point)
        is WorkoutFilter.And -> filters.all { it.matches(point) }
        is WorkoutFilter.Or -> filters.any { it.matches(point) }
        is WorkoutFilter.Not -> !filter.matches(point)
    }

    private fun WorkoutCriterion.matches(point: WorkoutDataPoint): Boolean {
        val session = point.session
        val entry = point.entry
        val set = point.strengthSet
        return when (this) {
            is WorkoutCriterion.SessionIds -> session.id in values
            is WorkoutCriterion.Statuses -> session.status in values
            is WorkoutCriterion.StartedBetween ->
                (fromInclusiveUtc == null || session.startedAtUtc >= fromInclusiveUtc) &&
                    (toExclusiveUtc == null || session.startedAtUtc < toExclusiveUtc)
            is WorkoutCriterion.SessionNameContains ->
                session.name.contains(value.trim(), ignoreCase = true)
            is WorkoutCriterion.TemplateIds -> session.templateId in values
            is WorkoutCriterion.ExerciseIds -> entry?.exerciseId in values
            is WorkoutCriterion.ExerciseNames ->
                entry != null && values.any { it.equals(entry.exerciseSnapshotName, ignoreCase = true) }
            is WorkoutCriterion.ExerciseNameContains ->
                entry?.exerciseSnapshotName?.contains(value.trim(), ignoreCase = true) == true
            is WorkoutCriterion.BodyParts ->
                entry != null && values.any {
                    it.equals(entry.exerciseSnapshotPrimaryBodyPart, ignoreCase = true)
                }
            is WorkoutCriterion.Categories -> entry?.entryType in values
            is WorkoutCriterion.SupersetGroups -> entry?.supersetGroup in values
            is WorkoutCriterion.SetTypes -> set?.setType in values
            is WorkoutCriterion.Warmup -> set != null && set.isWarmup == included
            is WorkoutCriterion.Pr -> set != null && set.isPr == included
            is WorkoutCriterion.RepsBetween ->
                set != null && (minimum == null || set.reps >= minimum) &&
                    (maximum == null || set.reps <= maximum)
            is WorkoutCriterion.WeightBetweenKg ->
                set != null && (minimum == null || set.weightKg >= minimum) &&
                    (maximum == null || set.weightKg <= maximum)
            is WorkoutCriterion.HasTimedWork ->
                ((set?.durationSeconds ?: 0) > 0 || (point.cardio?.durationSeconds ?: 0) > 0) == value
            is WorkoutCriterion.HasCardio -> (point.cardio != null) == value
        }
    }
}
