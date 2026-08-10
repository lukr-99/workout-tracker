package com.lukr99.workout.data.transfer

import com.lukr99.workout.data.WorkoutRepository
import com.lukr99.workout.data.export.CsvExporter
import com.lukr99.workout.data.export.ExportBundle
import com.lukr99.workout.data.export.JsonExporter
import com.lukr99.workout.data.importer.BundleTextImporter
import com.lukr99.workout.data.importer.LyftaCsvImporter
import com.lukr99.workout.data.run.RunRepository
import com.lukr99.workout.domain.Exercise
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.domain.WorkoutTemplate
import com.lukr99.workout.domain.query.WorkoutQueryEngine

/**
 * High-level Phase 3 API: detect -> parse -> preview -> plan -> atomic commit, plus selective
 * JSON/CSV exports. UI, command-line tools, and future sync adapters can all use the same service.
 *
 * Owns both the strength [repository] and the Run Mode [runRepository] so a bundle is the *whole*
 * store — exports carry runs + routes (with traces), and a restore puts them back. (This is the seam
 * the automatic backup writes through, so backups include runs too.)
 */
class DataTransferService(
    private val repository: WorkoutRepository,
    private val runRepository: RunRepository,
    importers: Iterable<TextDataImporter> = listOf(BundleTextImporter, LyftaCsvImporter),
) {
    private val importers = importers.toList()

    suspend fun previewImport(
        text: String,
        fileName: String? = null,
        options: ImportOptions = ImportOptions(),
    ): ImportPreview {
        val context = ImportContext(
            exercises = repository.getExercises(
                com.lukr99.workout.domain.ExerciseFilter(includeArchived = true),
            ),
            templates = repository.getTemplates(),
            sessions = repository.getSessions(includeDiscarded = true),
            existingRunIds = runRepository.getRuns().mapTo(mutableSetOf()) { it.id },
            existingRouteIds = runRepository.getRoutes().mapTo(mutableSetOf()) { it.id },
        )
        val candidates = options.formatHint?.let { hint ->
            importers.filter { it.format == hint }
        } ?: importers
        val importer = candidates.maxByOrNull { it.confidence(text, fileName) }
        if (importer == null || importer.confidence(text, fileName) <= 0) {
            return ImportPreview(
                ImportPlan(
                    format = options.formatHint ?: DataFormat.WorkoutJson,
                    issues = listOf(
                        TransferIssue(
                            "format.unknown",
                            "The selected file is not a supported Workout JSON or Lyfta CSV export.",
                            TransferIssueSeverity.Error,
                        ),
                    ),
                    sourceLabel = fileName,
                ),
                ImportSummary(),
            )
        }
        val payload = importer.parse(text, context, options, fileName)
        return ImportPlanner.plan(payload, context, options)
    }

    suspend fun commitImport(preview: ImportPreview): ImportCommitResult {
        require(preview.canCommit) { "Import preview contains errors and cannot be committed." }
        var insertedExercises = 0
        var changedExercises = 0
        var insertedTemplates = 0
        var changedTemplates = 0
        var insertedSessions = 0
        var changedSessions = 0
        var skippedSessions = 0

        repository.inTransaction {
            preview.plan.exercises.forEach { planned ->
                when (planned.action) {
                    PlannedAction.Skip -> Unit
                    PlannedAction.Insert, PlannedAction.KeepBoth -> {
                        saveExercise(planned.value)
                        insertedExercises++
                    }
                    else -> {
                        saveExercise(planned.value)
                        changedExercises++
                    }
                }
            }
            preview.plan.templates.forEach { planned ->
                when (planned.action) {
                    PlannedAction.Skip -> Unit
                    PlannedAction.Insert, PlannedAction.KeepBoth -> {
                        saveTemplate(planned.value)
                        insertedTemplates++
                    }
                    else -> {
                        saveTemplate(planned.value)
                        changedTemplates++
                    }
                }
            }
            preview.plan.sessions.forEach { planned ->
                when (planned.action) {
                    PlannedAction.Skip -> skippedSessions++
                    PlannedAction.Insert, PlannedAction.KeepBoth -> {
                        saveWorkoutSession(planned.value)
                        insertedSessions++
                    }
                    PlannedAction.Replace -> {
                        planned.targetId?.let { target ->
                            if (target != planned.value.id) deleteWorkoutSession(target)
                        }
                        saveWorkoutSession(planned.value)
                        changedSessions++
                    }
                    PlannedAction.Update, PlannedAction.Merge -> {
                        saveWorkoutSession(planned.value)
                        changedSessions++
                    }
                }
            }
        }

        // Runs/routes live in their own tables (RunRepository); restore them after the strength commit.
        // Routes first so a run's routeId reference resolves to an already-present route.
        preview.plan.routes.forEach { runRepository.saveRoute(it) }
        preview.plan.runs.forEach { runRepository.saveRun(it) }

        return ImportCommitResult(
            format = preview.plan.format,
            insertedExercises = insertedExercises,
            changedExercises = changedExercises,
            insertedTemplates = insertedTemplates,
            changedTemplates = changedTemplates,
            insertedSessions = insertedSessions,
            changedSessions = changedSessions,
            skippedSessions = skippedSessions,
            insertedRuns = preview.plan.runs.size,
            insertedRoutes = preview.plan.routes.size,
            issues = preview.plan.issues,
        )
    }

    suspend fun exportJson(options: JsonExportOptions = JsonExportOptions()): ExportArtifact {
        val allSessions = repository.getSessions(options.includeDiscardedSessions)
        val sessions = WorkoutQueryEngine.filterSessions(allSessions, options.query)
        val allExercises = repository.getExercises(
            com.lukr99.workout.domain.ExerciseFilter(includeArchived = true),
        )
        val referencedIds = sessions.flatMap(WorkoutSession::entries)
            .mapTo(linkedSetOf()) { it.exerciseId }
        val exercises = when {
            !options.includeExercises -> emptyList()
            options.includeUnreferencedExercises -> allExercises
            else -> allExercises.filter { it.id in referencedIds }
        }
        val templates = if (options.includeTemplates) repository.getTemplates() else emptyList()
        // Run Mode data is part of the store: full runs (with traces) + saved routes go in the bundle,
        // so both a manual export and the automatic backup capture them.
        val runs = if (options.includeRuns) runRepository.exportRuns() else emptyList()
        val routes = if (options.includeRuns) runRepository.exportRoutes() else emptyList()
        val bundle = ExportBundle(
            exercises = exercises,
            templates = templates,
            sessions = sessions.sortedByDescending(WorkoutSession::startedAtUtc),
            runs = runs,
            routes = routes,
        )
        return ExportArtifact(
            fileName = options.fileName.ensureExtension("json"),
            mimeType = DataFormat.WorkoutJson.mimeType,
            format = DataFormat.WorkoutJson,
            text = JsonExporter.toJson(bundle),
            recordCount = sessions.size + runs.size,
        )
    }

    suspend fun exportCsv(options: CsvExportOptions = CsvExportOptions()): ExportArtifact =
        CsvExporter.export(repository.getSessions(options.includeDiscardedSessions), options)

    private fun String.ensureExtension(extension: String): String =
        if (endsWith(".$extension", ignoreCase = true)) this else "$this.$extension"
}
