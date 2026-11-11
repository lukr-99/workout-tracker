package com.lukr99.workout.data.claude

import com.lukr99.workout.data.WorkoutRepository
import com.lukr99.workout.data.export.ExportBundle
import com.lukr99.workout.data.export.JsonExporter
import com.lukr99.workout.data.run.RunRepository
import com.lukr99.workout.domain.ExerciseFilter
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.settings.SettingsStore
import com.lukr99.workout.settings.UnitSystem
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

/**
 * Builds the [ClaudeShare] from the phone's own data: every exercise (archived ones too, so old
 * workouts still name theirs), the templates, finished and live workouts, locations, gym notes,
 * routes without their points, and runs with splits. Photos, settings and GPS traces stay home.
 */
class ClaudeShareBuilder(
    private val repository: WorkoutRepository,
    private val runRepository: RunRepository,
    private val settings: SettingsStore,
    private val appVersion: String?,
    private val timeZone: () -> String,
) {
    suspend fun build(): ClaudeShare {
        val runs = runRepository.getRunsWithTraces()
        val bundle = ExportBundle(
            exercises = repository.getExercises(ExerciseFilter(includeArchived = true)),
            templates = repository.getTemplates(),
            sessions = repository.getSessions(includeDiscarded = false).sortedByDescending(WorkoutSession::startedAtUtc),
            routes = runRepository.getRoutes().map { it.copy(points = emptyList(), encodedPolyline = "") },
            appVersion = appVersion,
            locations = repository.getLocations(),
            locationNotes = repository.getAllLocationNotes(),
        )
        return ClaudeShare(
            timeZone = timeZone(),
            units = if (settings.settings.first().units == UnitSystem.Imperial) "imperial" else "metric",
            appVersion = appVersion,
            bundle = bundle,
            runs = runs.sortedByDescending { it.startedAtUtc }.map(SharedRun::of),
        )
    }

    companion object {
        /** The bundle's JSON settings without the pretty printing, since nobody reads this file. */
        val json: Json = Json(JsonExporter.json) { prettyPrint = false }

        fun toJson(share: ClaudeShare): String = json.encodeToString(ClaudeShare.serializer(), share)
    }
}
