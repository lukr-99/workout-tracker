package com.lukr99.workout.data.importer

import com.lukr99.workout.data.transfer.DataFormat
import com.lukr99.workout.data.transfer.ImportContext
import com.lukr99.workout.data.transfer.ImportOptions
import com.lukr99.workout.data.transfer.ImportedPayload
import com.lukr99.workout.data.transfer.TextDataImporter
import com.lukr99.workout.data.transfer.TransferIssue
import com.lukr99.workout.data.transfer.TransferIssueSeverity
import com.lukr99.workout.data.transfer.WeightUnit
import com.lukr99.workout.domain.CardioEntryData
import com.lukr99.workout.domain.Exercise
import com.lukr99.workout.domain.ExerciseCategory
import com.lukr99.workout.domain.ExerciseSource
import com.lukr99.workout.domain.SetType
import com.lukr99.workout.domain.StrengthSet
import com.lukr99.workout.domain.Units
import com.lukr99.workout.domain.WorkoutEntry
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.domain.WorkoutSessionStatus
import com.lukr99.workout.domain.newId
import java.time.ZoneId

/**
 * Lyfta CSV -> portable domain mapping. Parsing is side-effect free: it returns a staged payload
 * for [com.lukr99.workout.data.transfer.DataTransferService] to dedupe, preview, and atomically
 * commit. Header order, casing, leading spaces, quoted fields, and common aliases are tolerated.
 */
internal object LyftaCsvImporter : TextDataImporter {
    override val format = DataFormat.LyftaCsv

    override fun confidence(text: String, fileName: String?): Int {
        val header = text.lineSequence().firstOrNull().orEmpty()
        val normalized = CsvReader.normalizeHeader(header)
        var score = 0
        if ("supersetid" in normalized) score += 35
        if ("settype" in normalized) score += 35
        if ("exercise" in normalized) score += 10
        if ("duration" in normalized && "weight" in normalized && "reps" in normalized) score += 15
        if (fileName?.contains("lyfta", ignoreCase = true) == true) score += 10
        return score.coerceAtMost(100)
    }

    override fun parse(
        text: String,
        context: ImportContext,
        options: ImportOptions,
        sourceLabel: String?,
    ): ImportedPayload {
        val issues = mutableListOf<TransferIssue>()
        val table = runCatching { CsvReader.parse(text) }.getOrElse {
            return ImportedPayload(
                format = format,
                issues = listOf(
                    TransferIssue(
                        "csv.parse",
                        it.message ?: "Could not parse CSV.",
                        TransferIssueSeverity.Error,
                    ),
                ),
                sourceLabel = sourceLabel,
            )
        }
        val required = mapOf(
            "title" to listOf("title", "workout", "workouttitle", "session"),
            "date" to listOf("date", "startdate", "startedat"),
            "exercise" to listOf("exercise", "exercisename", "movement"),
        )
        required.forEach { (label, aliases) ->
            if (aliases.none { CsvReader.normalizeHeader(it) in table.headers }) {
                issues += TransferIssue(
                    "csv.missing_header",
                    "Required '$label' column is missing.",
                    TransferIssueSeverity.Error,
                    field = label,
                )
            }
        }
        if (issues.any { it.severity == TransferIssueSeverity.Error }) {
            return ImportedPayload(format, issues = issues, sourceRows = table.records.size)
        }

        val zone = runCatching { ZoneId.of(options.sourceTimeZoneId) }.getOrElse {
            issues += TransferIssue(
                "time_zone.invalid",
                "Unknown timezone '${options.sourceTimeZoneId}'; device timezone was used.",
                TransferIssueSeverity.Warning,
            )
            ZoneId.systemDefault()
        }
        val sourceUnit = resolveWeightUnit(table, options)
        val rows = table.records.mapNotNull { record -> parseRow(record, zone, sourceUnit, options, issues) }
        val resolver = ImportExerciseMatcher(context.exercises, options)
        val newExercises = linkedMapOf<String, Exercise>()
        val grouped = linkedMapOf<SessionKey, MutableList<LyftaRow>>()
        rows.forEach { row -> grouped.getOrPut(SessionKey(row.title, row.startedAtUtc)) { mutableListOf() } += row }

        val sessions = grouped.map { (key, sessionRows) ->
            val sessionId = newId()
            val duration = sessionRows.firstNotNullOfOrNull(LyftaRow::sessionDurationSeconds) ?: 0L
            val entryGroups = linkedMapOf<String, MutableList<LyftaRow>>()
            sessionRows.forEach { row ->
                entryGroups.getOrPut(normalizeExerciseName(row.exerciseName)) { mutableListOf() } += row
            }
            val supersetIds = sessionRows.mapNotNull(LyftaRow::supersetId)
                .distinct()
                .mapIndexed { index, raw -> raw to (raw.toIntOrNull() ?: index + 1) }
                .toMap()

            val entries = entryGroups.values.mapIndexed { index, exerciseRows ->
                val rawName = exerciseRows.first().exerciseName
                val category = inferCategory(rawName, exerciseRows, options)
                val exercise = resolver.resolve(rawName, category) ?: newExercises.getOrPut(normalizeExerciseName(rawName)) {
                    Exercise(
                        name = rawName.trim(),
                        category = category,
                        primaryBodyPart = if (category == ExerciseCategory.Cardio) "Cardio" else "Full Body",
                        source = ExerciseSource.Custom,
                        externalSourceId = "lyfta:${normalizeExerciseName(rawName).replace(' ', '-')}",
                    )
                }
                val entryId = newId()

                if (category == ExerciseCategory.Cardio) {
                    WorkoutEntry(
                        id = entryId,
                        workoutSessionId = sessionId,
                        exerciseId = exercise.id,
                        // Preserve the exact historical Lyfta label even when it aliases to a
                        // normalized catalog exercise.
                        exerciseSnapshotName = rawName.trim(),
                        exerciseSnapshotCategory = category,
                        exerciseSnapshotPrimaryBodyPart = exercise.primaryBodyPart,
                        sortOrder = index,
                        entryType = category,
                        supersetGroup = exerciseRows.firstNotNullOfOrNull(LyftaRow::supersetId)
                            ?.let(supersetIds::get),
                        cardioData = CardioEntryData(
                            workoutEntryId = entryId,
                            durationSeconds = exerciseRows.sumOf { it.workSeconds ?: 0 },
                            distanceKm = exerciseRows.mapNotNull(LyftaRow::distanceKm)
                                .takeIf(List<Double>::isNotEmpty)?.sum(),
                        ),
                    )
                } else {
                    WorkoutEntry(
                        id = entryId,
                        workoutSessionId = sessionId,
                        exerciseId = exercise.id,
                        exerciseSnapshotName = rawName.trim(),
                        exerciseSnapshotCategory = category,
                        exerciseSnapshotPrimaryBodyPart = exercise.primaryBodyPart,
                        sortOrder = index,
                        entryType = category,
                        supersetGroup = exerciseRows.firstNotNullOfOrNull(LyftaRow::supersetId)
                            ?.let(supersetIds::get),
                        strengthSets = exerciseRows.mapIndexed { setIndex, row ->
                            StrengthSet(
                                id = newId(),
                                workoutEntryId = entryId,
                                setNumber = setIndex + 1,
                                reps = row.reps ?: 0,
                                weightKg = row.weightKg ?: 0.0,
                                isWarmup = row.setType == SetType.Warmup,
                                durationSeconds = row.workSeconds,
                                setType = row.setType,
                            )
                        },
                    )
                }
            }
            WorkoutSession(
                id = sessionId,
                name = key.title.trim().ifBlank { "Imported Workout" },
                startedAtUtc = key.startedAtUtc,
                endedAtUtc = key.startedAtUtc + duration * 1_000,
                completedDateUtc = key.startedAtUtc + duration * 1_000,
                durationSeconds = duration,
                notes = "Imported from Lyfta",
                status = WorkoutSessionStatus.Completed,
                entries = entries,
            )
        }

        if (options.strict && rows.size != table.records.size) {
            issues += TransferIssue(
                "csv.strict_rows",
                "${table.records.size - rows.size} invalid row(s) prevented strict import.",
                TransferIssueSeverity.Error,
            )
        }
        return ImportedPayload(
            format = format,
            exercises = newExercises.values.toList(),
            sessions = sessions,
            issues = issues,
            sourceRows = table.records.size,
            sourceLabel = sourceLabel,
            metadata = mapOf(
                "delimiter" to table.delimiter.toString(),
                "sourceWeightUnit" to sourceUnit.name,
                "timeZone" to zone.id,
            ),
        )
    }

    private fun parseRow(
        record: CsvRecord,
        zone: ZoneId,
        unit: WeightUnit,
        options: ImportOptions,
        issues: MutableList<TransferIssue>,
    ): LyftaRow? {
        val title = record["title", "workout", "workout title", "session"].orEmpty()
        val date = record["date", "start date", "started at"]
        val exercise = record["exercise", "exercise name", "movement"].orEmpty()
        if (date == null || exercise.isBlank()) {
            issues += TransferIssue(
                "lyfta.required_value",
                "Row is missing a date or exercise and was skipped.",
                if (options.strict) TransferIssueSeverity.Error else TransferIssueSeverity.Warning,
                row = record.rowNumber,
            )
            return null
        }
        val startedAt = LyftaValues.parseDate(date, zone)
        if (startedAt == null) {
            issues += TransferIssue(
                "lyfta.date",
                "Date '$date' is not recognized; row was skipped.",
                if (options.strict) TransferIssueSeverity.Error else TransferIssueSeverity.Warning,
                row = record.rowNumber,
                field = "Date",
            )
            return null
        }
        val rawWeight = LyftaValues.parseNumber(record["weight", "weight kg", "weight lbs"])
        val rawSetType = record["set type", "settype", "type"]
        val type = LyftaValues.parseSetType(rawSetType)
        if (rawSetType != null && type == null) {
            issues += TransferIssue(
                "lyfta.set_type",
                "Unknown set type '$rawSetType'; NORMAL was used.",
                TransferIssueSeverity.Warning,
                row = record.rowNumber,
                field = "Set Type",
            )
        }
        return LyftaRow(
            title = title.ifBlank { "Imported Workout" },
            startedAtUtc = startedAt,
            sessionDurationSeconds = LyftaValues.parseDuration(record["duration", "workout duration"])?.toLong(),
            exerciseName = exercise,
            supersetId = record["superset id", "superset", "supersetid"],
            weightKg = rawWeight?.let { if (unit == WeightUnit.Pounds) Units.lbToKg(it) else it },
            reps = LyftaValues.parseNumber(record["reps", "repetitions"])?.toInt(),
            distanceKm = LyftaValues.parseNumber(record["distance", "distance km", "kilometers"]),
            workSeconds = LyftaValues.parseDuration(record["time", "set time", "set duration"]),
            setType = type ?: SetType.Normal,
        )
    }

    private fun resolveWeightUnit(table: CsvTable, options: ImportOptions): WeightUnit {
        if (options.sourceWeightUnit != WeightUnit.Auto) return options.sourceWeightUnit
        return if (table.headers.any { "lb" in it || "pound" in it }) WeightUnit.Pounds
        else WeightUnit.Kilograms
    }

    private fun inferCategory(
        name: String,
        rows: List<LyftaRow>,
        options: ImportOptions,
    ): ExerciseCategory {
        val normalized = normalizeExerciseName(name)
        options.exerciseCategoryOverrides.entries.firstOrNull {
            normalizeExerciseName(it.key) == normalized
        }?.let { return it.value }
        if (rows.any { it.reps != null || it.weightKg != null }) return ExerciseCategory.Strength
        if (rows.any { it.distanceKm != null }) return ExerciseCategory.Cardio
        return if (CARDIO_WORDS.any(normalized::contains)) ExerciseCategory.Cardio
        else ExerciseCategory.Strength
    }

    private data class SessionKey(val title: String, val startedAtUtc: Long)

    private data class LyftaRow(
        val title: String,
        val startedAtUtc: Long,
        val sessionDurationSeconds: Long?,
        val exerciseName: String,
        val supersetId: String?,
        val weightKg: Double?,
        val reps: Int?,
        val distanceKm: Double?,
        val workSeconds: Int?,
        val setType: SetType,
    )

    private val CARDIO_WORDS = setOf(
        "run", "walk", "bike", "cycling", "rower", "rowing", "treadmill", "elliptical",
        "stair", "swim", "jump rope",
    )
}
