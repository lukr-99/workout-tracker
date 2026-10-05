package com.lukr99.workout.data.transfer

import com.lukr99.workout.domain.WorkoutSession

/**
 * Plans a replace-restore: every record goes in exactly as the backup holds it, with its own id.
 * No matching or merging happens, so two exercises that share a name stay two exercises. Only our
 * own JSON bundle can replace the store; anything else is refused with an error issue.
 */
internal object ReplacePlanner {
    fun plan(payload: ImportedPayload, current: StoreCounts): ImportPreview {
        val issues = payload.issues.toMutableList()
        if (payload.format != DataFormat.WorkoutJson) {
            issues += TransferIssue(
                "replace.format",
                "Only an Ember backup (JSON) can replace your data. Import this file with Merge instead.",
                TransferIssueSeverity.Error,
            )
        }
        val plan = ImportPlan(
            format = payload.format,
            exercises = payload.exercises.map { PlannedExercise(it, PlannedAction.Insert, reason = "Restored") },
            templates = payload.templates.map { PlannedTemplate(it, PlannedAction.Insert, reason = "Restored") },
            sessions = payload.sessions.map { PlannedSession(it, PlannedAction.Insert, reason = "Restored") },
            runs = payload.runs,
            routes = payload.routes,
            issues = issues,
            sourceLabel = payload.sourceLabel,
            mode = RestoreMode.Replace,
            exerciseIdMap = payload.exercises.associate { it.id to it.id },
            photos = payload.photos,
            settings = payload.settings,
            replaces = current,
        )
        val summary = ImportSummary(
            sourceRows = payload.sourceRows,
            parsedSessions = payload.sessions.size,
            insertedSessions = payload.sessions.size,
            insertedExercises = payload.exercises.size,
            templates = payload.templates.size,
            setCount = payload.sessions.sumOf { session ->
                session.entries.sumOf { it.strengthSets.size } + session.entries.count { it.cardioData != null }
            },
            insertedRuns = payload.runs.size,
            insertedRoutes = payload.routes.size,
            photos = payload.photos.size,
            dateFromUtc = payload.sessions.minOfOrNull(WorkoutSession::startedAtUtc),
            dateToUtc = payload.sessions.maxOfOrNull(WorkoutSession::startedAtUtc),
            metadata = payload.metadata,
        )
        return ImportPreview(plan, summary)
    }
}
