package com.lukr99.workout.data.importer

import com.lukr99.workout.data.transfer.DataFormat
import com.lukr99.workout.data.transfer.ExerciseMatchMode
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
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlin.math.max

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
        val resolver = ExerciseResolver(context.exercises, options)
        val newExercises = linkedMapOf<String, Exercise>()
        val grouped = linkedMapOf<SessionKey, MutableList<LyftaRow>>()
        rows.forEach { row -> grouped.getOrPut(SessionKey(row.title, row.startedAtUtc)) { mutableListOf() } += row }

        val sessions = grouped.map { (key, sessionRows) ->
            val sessionId = newId()
            val duration = sessionRows.firstNotNullOfOrNull(LyftaRow::sessionDurationSeconds) ?: 0L
            val entryGroups = linkedMapOf<String, MutableList<LyftaRow>>()
            sessionRows.forEach { row ->
                entryGroups.getOrPut(normalizeName(row.exerciseName)) { mutableListOf() } += row
            }
            val supersetIds = sessionRows.mapNotNull(LyftaRow::supersetId)
                .distinct()
                .mapIndexed { index, raw -> raw to (raw.toIntOrNull() ?: index + 1) }
                .toMap()

            val entries = entryGroups.values.mapIndexed { index, exerciseRows ->
                val rawName = exerciseRows.first().exerciseName
                val category = inferCategory(rawName, exerciseRows, options)
                val exercise = resolver.resolve(rawName, category) ?: newExercises.getOrPut(normalizeName(rawName)) {
                    Exercise(
                        name = rawName.trim(),
                        category = category,
                        primaryBodyPart = if (category == ExerciseCategory.Cardio) "Cardio" else "Full Body",
                        source = ExerciseSource.Custom,
                        externalSourceId = "lyfta:${normalizeName(rawName).replace(' ', '-')}",
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
        val startedAt = parseDate(date, zone)
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
        val rawWeight = parseNumber(record["weight", "weight kg", "weight lbs"])
        val rawSetType = record["set type", "settype", "type"]
        val type = parseSetType(rawSetType)
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
            sessionDurationSeconds = parseDuration(record["duration", "workout duration"])?.toLong(),
            exerciseName = exercise,
            supersetId = record["superset id", "superset", "supersetid"],
            weightKg = rawWeight?.let { if (unit == WeightUnit.Pounds) Units.lbToKg(it) else it },
            reps = parseNumber(record["reps", "repetitions"])?.toInt(),
            distanceKm = parseNumber(record["distance", "distance km", "kilometers"]),
            workSeconds = parseDuration(record["time", "set time", "set duration"]),
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
        val normalized = normalizeName(name)
        options.exerciseCategoryOverrides.entries.firstOrNull {
            normalizeName(it.key) == normalized
        }?.let { return it.value }
        if (rows.any { it.reps != null || it.weightKg != null }) return ExerciseCategory.Strength
        if (rows.any { it.distanceKm != null }) return ExerciseCategory.Cardio
        return if (CARDIO_WORDS.any(normalized::contains)) ExerciseCategory.Cardio
        else ExerciseCategory.Strength
    }

    private fun parseSetType(value: String?): SetType? = when (
        value?.trim()?.uppercase()?.replace('-', '_')?.replace(' ', '_')
    ) {
        null, "" -> SetType.Normal
        "NORMAL", "NORMAL_SET", "WORKING_SET" -> SetType.Normal
        "WARMUP", "WARM_UP", "WARMUP_SET", "WARM_UP_SET" -> SetType.Warmup
        "DROP", "DROP_SET" -> SetType.Drop
        "FAILURE", "FAILURE_SET", "TO_FAILURE" -> SetType.Failure
        "NEGATIVE", "NEGATIVE_SET", "NEGATIVE_REPS_SET" -> SetType.Negative
        "BACK_OFF", "BACKOFF", "BACK_OFF_SET", "BACKOFF_SET" -> SetType.BackOff
        else -> null
    }

    private fun parseDate(value: String, zone: ZoneId): Long? {
        runCatching { return Instant.parse(value).toEpochMilli() }
        runCatching { return OffsetDateTime.parse(value).toInstant().toEpochMilli() }
        DATE_FORMATS.forEach { formatter ->
            try {
                return LocalDateTime.parse(value.trim(), formatter).atZone(zone).toInstant().toEpochMilli()
            } catch (_: DateTimeParseException) {
                // Try the next supported format.
            }
        }
        return null
    }

    private fun parseDuration(value: String?): Int? {
        val clean = value?.trim()?.takeUnless { it.isBlank() || it.equals("null", true) } ?: return null
        clean.toIntOrNull()?.let { return it }
        val pieces = clean.split(':').mapNotNull(String::toIntOrNull)
        if (pieces.size != clean.count { it == ':' } + 1) return null
        return when (pieces.size) {
            3 -> pieces[0] * 3_600 + pieces[1] * 60 + pieces[2]
            2 -> pieces[0] * 60 + pieces[1]
            1 -> pieces[0]
            else -> null
        }
    }

    private fun parseNumber(value: String?): Double? {
        val clean = value?.trim()?.takeUnless { it.isBlank() || it.equals("null", true) } ?: return null
        return clean.removeSuffix("kg").removeSuffix("lbs").removeSuffix("lb")
            .trim().replace(',', '.').toDoubleOrNull()
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

    private class ExerciseResolver(
        catalog: List<Exercise>,
        private val options: ImportOptions,
    ) {
        private val catalog = catalog
        private val exact = catalog.associateBy { it.name.trim().lowercase() }
        private val normalized = catalog.associateBy { normalizeName(it.name) }
        private val aliases = (BUILT_IN_ALIASES + options.exerciseAliases.mapKeys { normalizeName(it.key) })
            .mapValues { normalizeName(it.value) }

        fun resolve(name: String, category: ExerciseCategory): Exercise? {
            if (options.exerciseMatchMode == ExerciseMatchMode.AlwaysCreate) return null
            exact[name.trim().lowercase()]?.let { return it }
            if (options.exerciseMatchMode == ExerciseMatchMode.Exact) return null
            val key = normalizeName(name)
            normalized[key]?.let { return it }
            if (options.exerciseMatchMode in setOf(ExerciseMatchMode.Aliases, ExerciseMatchMode.Fuzzy)) {
                aliases[key]?.let(normalized::get)?.let { return it }
            }
            if (options.exerciseMatchMode == ExerciseMatchMode.Fuzzy) {
                return catalog.asSequence()
                    .filter { it.category == category }
                    .map { it to similarity(key, normalizeName(it.name)) }
                    .maxByOrNull(Pair<Exercise, Double>::second)
                    ?.takeIf { it.second >= options.fuzzyMatchThreshold }
                    ?.first
            }
            return null
        }
    }

    private fun normalizeName(value: String): String = value.lowercase()
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()
        .replace(Regex("\\s+"), " ")

    private fun similarity(left: String, right: String): Double {
        if (left == right) return 1.0
        val a = left.split(' ').filter(String::isNotBlank).toSet()
        val b = right.split(' ').filter(String::isNotBlank).toSet()
        if (a.isEmpty() || b.isEmpty()) return 0.0
        val tokenScore = a.intersect(b).size.toDouble() / a.union(b).size
        val prefix = left.zip(right).takeWhile { it.first == it.second }.size.toDouble() /
            max(left.length, right.length).coerceAtLeast(1)
        return tokenScore * 0.85 + prefix * 0.15
    }

    private val DATE_FORMATS = listOf(
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
        DateTimeFormatter.ISO_LOCAL_DATE_TIME,
    )

    private val CARDIO_WORDS = setOf(
        "run", "walk", "bike", "cycling", "rower", "rowing", "treadmill", "elliptical",
        "stair", "swim", "jump rope",
    )

    private val BUILT_IN_ALIASES = mapOf(
        "bench press" to "barbell bench press",
        "barbell squat" to "back squat",
        "cable seated row" to "seated cable row",
        "stationary bicycle" to "stationary bike",
        "running treadmill" to "treadmill run",
    )
}
