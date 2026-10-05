package com.lukr99.workout.data.transfer

import com.lukr99.workout.data.WorkoutRepository
import com.lukr99.workout.data.export.CsvExporter
import com.lukr99.workout.data.export.ExercisePhoto
import com.lukr99.workout.data.export.ExportBundle
import com.lukr99.workout.data.export.JsonExporter
import com.lukr99.workout.data.importer.BundleTextImporter
import com.lukr99.workout.data.importer.LyftaCsvImporter
import com.lukr99.workout.data.run.RunRepository
import com.lukr99.workout.domain.Exercise
import com.lukr99.workout.domain.ExerciseFilter
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.domain.query.WorkoutQueryEngine
import java.util.Base64

/**
 * High-level transfer API: detect -> parse -> preview -> plan -> atomic commit, plus JSON and CSV
 * exports. The automatic backup writes through [exportJson] too, so a backup is the whole store:
 * workouts, templates, the catalog, runs and routes, the owner's settings and personal photos.
 *
 * A commit is all or nothing. Photos are written to fresh files first; one database transaction
 * then writes every row (and, for [RestoreMode.Replace], deletes the old ones first). If anything
 * fails, the new photo files are deleted and the database is left as it was.
 */
class DataTransferService(
    private val repository: WorkoutRepository,
    private val runRepository: RunRepository,
    importers: Iterable<TextDataImporter> = listOf(BundleTextImporter, LyftaCsvImporter),
    private val photos: PhotoArchive = NoPhotoArchive,
    private val settings: SettingsArchive? = null,
    private val appVersion: String? = null,
) {
    private val importers = importers.toList()

    suspend fun previewImport(
        text: String,
        fileName: String? = null,
        options: ImportOptions = ImportOptions(),
    ): ImportPreview {
        val context = ImportContext(
            exercises = repository.getExercises(ExerciseFilter(includeArchived = true)),
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
                    mode = options.mode,
                ),
                ImportSummary(),
            )
        }
        val payload = importer.parse(text, context, options, fileName)
        return when (options.mode) {
            RestoreMode.Merge -> ImportPlanner.plan(payload, context, options)
            RestoreMode.Replace -> ReplacePlanner.plan(payload, currentCounts())
        }
    }

    suspend fun commitImport(preview: ImportPreview): ImportCommitResult {
        require(preview.canCommit) { "Import preview contains errors and cannot be committed." }
        val plan = preview.plan
        val replacing = plan.mode == RestoreMode.Replace

        // 1. Photos go to fresh files outside the database. Nothing points at them yet.
        val staged = mutableListOf<String>()
        val exercises = try {
            withRestoredPhotos(plan, staged)
        } catch (failure: Throwable) {
            staged.forEach(photos::delete)
            throw failure
        }

        var insertedExercises = 0
        var changedExercises = 0
        var insertedTemplates = 0
        var changedTemplates = 0
        var insertedSessions = 0
        var changedSessions = 0
        var skippedSessions = 0

        // 2. Every row in one transaction, runs and routes included.
        try {
            repository.inTransaction {
                if (replacing) {
                    deleteAllWorkoutData()
                    runRepository.deleteAll()
                }
                exercises.forEach { planned ->
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
                plan.templates.forEach { planned ->
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
                plan.sessions.forEach { planned ->
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
                // Routes first, so a run's routeId points at a route that is already there.
                plan.routes.forEach { runRepository.saveRoute(it) }
                plan.runs.forEach { runRepository.saveRun(it) }
            }
        } catch (failure: Throwable) {
            staged.forEach(photos::delete)
            throw failure
        }

        // 3. After the commit: a replace takes the backup's settings and drops photos nobody uses.
        var restoredSettings = false
        if (replacing) {
            plan.settings?.let { snapshot ->
                settings?.let {
                    it.restore(snapshot)
                    restoredSettings = true
                }
            }
            val inUse = repository.getExercises(ExerciseFilter(includeArchived = true))
                .mapNotNullTo(mutableSetOf(), Exercise::localImagePath)
            photos.deleteAllExcept(inUse)
        }

        return ImportCommitResult(
            format = plan.format,
            insertedExercises = insertedExercises,
            changedExercises = changedExercises,
            insertedTemplates = insertedTemplates,
            changedTemplates = changedTemplates,
            insertedSessions = insertedSessions,
            changedSessions = changedSessions,
            skippedSessions = skippedSessions,
            insertedRuns = plan.runs.size,
            insertedRoutes = plan.routes.size,
            restoredPhotos = staged.size,
            restoredSettings = restoredSettings,
            mode = plan.mode,
            issues = plan.issues,
        )
    }

    /** What the store holds now: shown before a replace-restore or "Delete all data". */
    suspend fun currentCounts(): StoreCounts = StoreCounts(
        exercises = repository.countExercises(),
        templates = repository.countTemplates(),
        workouts = repository.countWorkouts(),
        runs = runRepository.countRuns(),
        routes = runRepository.countRoutes(),
    )

    suspend fun exportJson(options: JsonExportOptions = JsonExportOptions()): ExportArtifact {
        val allSessions = repository.getSessions(options.includeDiscardedSessions)
        val sessions = WorkoutQueryEngine.filterSessions(allSessions, options.query)
        val allExercises = repository.getExercises(ExerciseFilter(includeArchived = true))
        val referencedIds = sessions.flatMap(WorkoutSession::entries)
            .mapTo(linkedSetOf()) { it.exerciseId }
        val exercises = when {
            !options.includeExercises -> emptyList()
            options.includeUnreferencedExercises -> allExercises
            else -> allExercises.filter { it.id in referencedIds }
        }
        val templates = if (options.includeTemplates) repository.getTemplates() else emptyList()
        val runs = if (options.includeRuns) runRepository.exportRuns() else emptyList()
        val routes = if (options.includeRuns) runRepository.exportRoutes() else emptyList()
        val bundle = ExportBundle(
            exercises = exercises,
            templates = templates,
            sessions = sessions.sortedByDescending(WorkoutSession::startedAtUtc),
            runs = runs,
            routes = routes,
            appVersion = appVersion,
            settings = if (options.includeSettings) settings?.snapshot() else null,
            photos = if (options.includePhotos) exportPhotos(exercises) else emptyList(),
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

    private fun exportPhotos(exercises: List<Exercise>): List<ExercisePhoto> =
        exercises.mapNotNull { exercise ->
            val bytes = exercise.localImagePath?.let(photos::exportBytes) ?: return@mapNotNull null
            ExercisePhoto(exerciseId = exercise.id, dataBase64 = Base64.getEncoder().encodeToString(bytes))
        }

    /**
     * Points each saved exercise at a photo that exists on this phone. On a merge, a photo the
     * exercise already has here wins. Otherwise the backup's photo is staged, and a path that only
     * existed on another phone is cleared rather than left dangling.
     */
    private fun withRestoredPhotos(plan: ImportPlan, staged: MutableList<String>): List<PlannedExercise> {
        val photoByTargetId = plan.photos.associateBy { photo ->
            plan.exerciseIdMap[photo.exerciseId] ?: photo.exerciseId
        }
        return plan.exercises.map { planned ->
            if (planned.action == PlannedAction.Skip) return@map planned
            val current = planned.value.localImagePath?.takeIf(photos::exists)
            val photo = photoByTargetId[planned.value.id]
            val path = when {
                current != null && plan.mode == RestoreMode.Merge -> current
                photo != null -> photos.stage(planned.value.id, Base64.getDecoder().decode(photo.dataBase64))
                    .also(staged::add)
                else -> current
            }
            if (path == planned.value.localImagePath) planned
            else planned.copy(value = planned.value.copy(localImagePath = path))
        }
    }

    private fun String.ensureExtension(extension: String): String =
        if (endsWith(".$extension", ignoreCase = true)) this else "$this.$extension"
}
