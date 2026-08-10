package com.lukr99.workout.data.transfer

import com.lukr99.workout.domain.Exercise
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.domain.WorkoutTemplate
import com.lukr99.workout.domain.query.WorkoutQuery
import com.lukr99.workout.domain.run.Route
import com.lukr99.workout.domain.run.Run
import java.time.ZoneId

enum class DataFormat(
    val defaultExtension: String,
    val mimeType: String,
) {
    WorkoutJson("json", "application/json"),
    WorkoutCsv("csv", "text/csv"),
    LyftaCsv("csv", "text/csv"),
}

enum class WeightUnit { Auto, Kilograms, Pounds }

enum class ConflictPolicy { Skip, Merge, Replace, KeepBoth }

enum class ExerciseMatchMode { Exact, Normalized, Aliases, Fuzzy, AlwaysCreate }

data class ImportOptions(
    val formatHint: DataFormat? = null,
    val sourceWeightUnit: WeightUnit = WeightUnit.Auto,
    val sourceTimeZoneId: String = ZoneId.systemDefault().id,
    val sessionConflictPolicy: ConflictPolicy = ConflictPolicy.Skip,
    val catalogConflictPolicy: ConflictPolicy = ConflictPolicy.Merge,
    val exerciseMatchMode: ExerciseMatchMode = ExerciseMatchMode.Aliases,
    val fuzzyMatchThreshold: Double = 0.84,
    val exerciseAliases: Map<String, String> = emptyMap(),
    val exerciseCategoryOverrides: Map<String, com.lukr99.workout.domain.ExerciseCategory> =
        emptyMap(),
    val strict: Boolean = false,
)

data class JsonExportOptions(
    val query: WorkoutQuery = WorkoutQuery(includeEmptySessions = true),
    val includeExercises: Boolean = true,
    val includeTemplates: Boolean = true,
    val includeUnreferencedExercises: Boolean = true,
    val includeDiscardedSessions: Boolean = false,
    /** Include Run Mode data (runs with traces + saved routes) in the bundle. */
    val includeRuns: Boolean = true,
    val fileName: String = "workout-backup.json",
)

data class CsvExportOptions(
    val query: WorkoutQuery = WorkoutQuery(),
    val includeDiscardedSessions: Boolean = false,
    val weightUnit: WeightUnit = WeightUnit.Kilograms,
    val timeZoneId: String = ZoneId.systemDefault().id,
    val delimiter: Char = ',',
    val includeHeader: Boolean = true,
    val includeUtf8Bom: Boolean = false,
    val lineEnding: CsvLineEnding = CsvLineEnding.Platform,
    val columns: List<CsvColumn> = CsvColumn.entries,
    val fileName: String = "workout-history.csv",
)

enum class CsvLineEnding(val value: String) {
    Lf("\n"),
    CrLf("\r\n"),
    Platform(System.lineSeparator()),
}

enum class CsvColumn(val header: String) {
    SessionId("Session ID"),
    SessionName("Session"),
    StartedAt("Started At"),
    CompletedAt("Completed At"),
    SessionDurationSeconds("Session Duration Seconds"),
    SessionStatus("Session Status"),
    SessionRpe("Session RPE"),
    Bodyweight("Bodyweight"),
    ExerciseId("Exercise ID"),
    ExerciseName("Exercise"),
    Category("Category"),
    BodyPart("Body Part"),
    SupersetGroup("Superset Group"),
    SetNumber("Set Number"),
    SetType("Set Type"),
    IsWarmup("Is Warmup"),
    IsPr("Is PR"),
    Reps("Reps"),
    Weight("Weight"),
    SetDurationSeconds("Set Duration Seconds"),
    Rir("RIR"),
    Rpe("RPE"),
    Distance("Distance"),
    CardioDurationSeconds("Cardio Duration Seconds"),
    Calories("Calories"),
    Notes("Notes"),
}

enum class TransferIssueSeverity { Info, Warning, Error }

data class TransferIssue(
    val code: String,
    val message: String,
    val severity: TransferIssueSeverity,
    val row: Int? = null,
    val field: String? = null,
)

enum class PlannedAction { Insert, Update, Merge, Replace, Skip, KeepBoth }

data class PlannedExercise(
    val value: Exercise,
    val action: PlannedAction,
    val targetId: String? = null,
    val reason: String = "",
)

data class PlannedTemplate(
    val value: WorkoutTemplate,
    val action: PlannedAction,
    val targetId: String? = null,
    val reason: String = "",
)

data class PlannedSession(
    val value: WorkoutSession,
    val action: PlannedAction,
    val targetId: String? = null,
    val reason: String = "",
)

data class ImportPlan(
    val format: DataFormat,
    val exercises: List<PlannedExercise> = emptyList(),
    val templates: List<PlannedTemplate> = emptyList(),
    val sessions: List<PlannedSession> = emptyList(),
    /** Runs to insert (those whose id isn't already present); restored as-is with their traces. */
    val runs: List<Run> = emptyList(),
    /** Saved routes to insert (those whose id isn't already present). */
    val routes: List<Route> = emptyList(),
    val issues: List<TransferIssue> = emptyList(),
    val sourceLabel: String? = null,
)

data class ImportPreview(
    val plan: ImportPlan,
    val summary: ImportSummary,
) {
    val canCommit: Boolean
        get() = plan.issues.none { it.severity == TransferIssueSeverity.Error }
}

data class ImportSummary(
    val sourceRows: Int = 0,
    val parsedSessions: Int = 0,
    val insertedSessions: Int = 0,
    val changedSessions: Int = 0,
    val skippedSessions: Int = 0,
    val insertedExercises: Int = 0,
    val matchedExercises: Int = 0,
    val templates: Int = 0,
    val setCount: Int = 0,
    val insertedRuns: Int = 0,
    val insertedRoutes: Int = 0,
    val dateFromUtc: Long? = null,
    val dateToUtc: Long? = null,
    val metadata: Map<String, String> = emptyMap(),
)

data class ImportCommitResult(
    val format: DataFormat,
    val insertedExercises: Int,
    val changedExercises: Int,
    val insertedTemplates: Int,
    val changedTemplates: Int,
    val insertedSessions: Int,
    val changedSessions: Int,
    val skippedSessions: Int,
    val insertedRuns: Int = 0,
    val insertedRoutes: Int = 0,
    val issues: List<TransferIssue>,
)

data class ExportArtifact(
    val fileName: String,
    val mimeType: String,
    val format: DataFormat,
    val text: String,
    val recordCount: Int,
)

data class ImportedPayload(
    val format: DataFormat,
    val exercises: List<Exercise> = emptyList(),
    val templates: List<WorkoutTemplate> = emptyList(),
    val sessions: List<WorkoutSession> = emptyList(),
    val runs: List<Run> = emptyList(),
    val routes: List<Route> = emptyList(),
    val issues: List<TransferIssue> = emptyList(),
    val sourceRows: Int = 0,
    val sourceLabel: String? = null,
    val metadata: Map<String, String> = emptyMap(),
)

data class ImportContext(
    val exercises: List<Exercise>,
    val templates: List<WorkoutTemplate>,
    val sessions: List<WorkoutSession>,
    /** Ids of runs/routes already in the store, so a restore only inserts the ones that are missing. */
    val existingRunIds: Set<String> = emptySet(),
    val existingRouteIds: Set<String> = emptySet(),
)

interface TextDataImporter {
    val format: DataFormat
    fun confidence(text: String, fileName: String? = null): Int
    fun parse(
        text: String,
        context: ImportContext,
        options: ImportOptions,
        sourceLabel: String? = null,
    ): ImportedPayload
}
