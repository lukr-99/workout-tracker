package com.lukr99.workout.domain.query

import com.lukr99.workout.domain.WorkoutSession

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
