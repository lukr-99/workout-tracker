package com.lukr99.workout.data.export

import com.lukr99.workout.data.transfer.CsvColumn
import com.lukr99.workout.data.transfer.CsvExportOptions
import com.lukr99.workout.data.transfer.DataFormat
import com.lukr99.workout.data.transfer.ExportArtifact
import com.lukr99.workout.data.transfer.WeightUnit
import com.lukr99.workout.domain.Units
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.domain.effectiveTags
import com.lukr99.workout.domain.query.WorkoutDataPoint
import com.lukr99.workout.domain.query.WorkoutQueryEngine
import com.lukr99.workout.domain.formatFixed
import com.lukr99.workout.domain.padded
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetAt
import kotlinx.datetime.toLocalDateTime

/** Configurable, flat, one-row-per-set/cardio-entry spreadsheet export. */
object CsvExporter {
    fun export(
        sessions: Iterable<WorkoutSession>,
        options: CsvExportOptions = CsvExportOptions(),
    ): ExportArtifact {
        require(options.delimiter !in setOf('"', '\r', '\n')) { "Invalid CSV delimiter." }
        val zone = runCatching { TimeZone.of(options.timeZoneId) }.getOrDefault(TimeZone.currentSystemDefault())
        val points = WorkoutQueryEngine.select(sessions, options.query).points
        val lineEnding = options.lineEnding.value
        val output = buildString {
            if (options.includeUtf8Bom) append('\uFEFF')
            if (options.includeHeader) {
                append(options.columns.joinToString(options.delimiter.toString()) { column ->
                    escape(header(column, options.weightUnit), options.delimiter)
                })
                append(lineEnding)
            }
            points.forEachIndexed { index, point ->
                append(options.columns.joinToString(options.delimiter.toString()) { column ->
                    escape(value(column, point, options.weightUnit, zone), options.delimiter)
                })
                if (index != points.lastIndex) append(lineEnding)
            }
        }
        return ExportArtifact(
            fileName = options.fileName.ensureExtension("csv"),
            mimeType = DataFormat.WorkoutCsv.mimeType,
            format = DataFormat.WorkoutCsv,
            text = output,
            recordCount = points.size,
        )
    }

    private fun header(column: CsvColumn, unit: WeightUnit): String = when (column) {
        CsvColumn.Weight -> if (unit == WeightUnit.Pounds) "Weight (lb)" else "Weight (kg)"
        CsvColumn.Bodyweight -> if (unit == WeightUnit.Pounds) "Bodyweight (lb)" else "Bodyweight (kg)"
        else -> column.header
    }

    private fun value(
        column: CsvColumn,
        point: WorkoutDataPoint,
        unit: WeightUnit,
        zone: TimeZone,
    ): String {
        val session = point.session
        val entry = point.entry
        val set = point.strengthSet
        val cardio = point.cardio
        return when (column) {
            CsvColumn.SessionId -> session.id
            CsvColumn.SessionName -> session.name
            CsvColumn.StartedAt -> formatDate(session.startedAtUtc, zone)
            CsvColumn.CompletedAt -> session.completedDateUtc?.let { formatDate(it, zone) }.orEmpty()
            CsvColumn.SessionDurationSeconds -> session.durationSeconds.toString()
            CsvColumn.SessionStatus -> session.status.name
            CsvColumn.SessionRpe -> session.perceivedEffort?.toString().orEmpty()
            CsvColumn.Bodyweight -> session.bodyweightKg?.let { formatWeight(it, unit) }.orEmpty()
            CsvColumn.ExerciseId -> entry?.exerciseId.orEmpty()
            CsvColumn.ExerciseName -> entry?.exerciseSnapshotName.orEmpty()
            CsvColumn.Category -> entry?.entryType?.name.orEmpty()
            CsvColumn.BodyPart -> entry?.exerciseSnapshotPrimaryBodyPart.orEmpty()
            CsvColumn.SupersetGroup -> entry?.supersetGroup?.toString().orEmpty()
            CsvColumn.SetNumber -> set?.setNumber?.toString().orEmpty()
            CsvColumn.SetType -> set?.setType?.name.orEmpty()
            CsvColumn.SetTags -> set?.effectiveTags?.sortedBy { it.ordinal }?.joinToString("|") { it.name }.orEmpty()
            CsvColumn.IsWarmup -> set?.isWarmup?.toString().orEmpty()
            CsvColumn.IsPr -> set?.isPr?.toString().orEmpty()
            CsvColumn.Reps -> set?.reps?.toString().orEmpty()
            CsvColumn.Weight -> set?.weightKg?.let { formatWeight(it, unit) }.orEmpty()
            CsvColumn.SetDurationSeconds -> set?.durationSeconds?.toString().orEmpty()
            CsvColumn.Rir -> set?.rir?.let(::formatNumber).orEmpty()
            CsvColumn.Rpe -> set?.rpe?.let(::formatNumber).orEmpty()
            CsvColumn.Distance -> cardio?.distanceKm?.let(::formatNumber).orEmpty()
            CsvColumn.CardioDurationSeconds -> cardio?.durationSeconds?.toString().orEmpty()
            CsvColumn.Calories -> cardio?.calories?.let(::formatNumber).orEmpty()
            CsvColumn.Notes -> set?.notes?.takeIf(String::isNotBlank)
                ?: cardio?.notes?.takeIf(String::isNotBlank)
                ?: entry?.notes?.takeIf(String::isNotBlank)
                ?: session.notes
        }
    }

    /** Like `DateTimeFormatter.ISO_OFFSET_DATE_TIME`: seconds always, a fraction only when not zero. */
    private fun formatDate(epochMillis: Long, zone: TimeZone): String {
        val instant = Instant.fromEpochMilliseconds(epochMillis)
        val local = instant.toLocalDateTime(zone)
        val fraction = if (local.nanosecond == 0) "" else "." + padded(local.nanosecond, 9).trimEnd('0')
        val offsetSeconds = zone.offsetAt(instant).totalSeconds
        return padded(local.year, 4) + "-" + padded(local.monthNumber, 2) + "-" + padded(local.dayOfMonth, 2) +
            "T" + padded(local.hour, 2) + ":" + padded(local.minute, 2) + ":" + padded(local.second, 2) +
            fraction + formatOffset(offsetSeconds)
    }

    private fun formatOffset(totalSeconds: Int): String {
        if (totalSeconds == 0) return "Z"
        val abs = kotlin.math.abs(totalSeconds)
        val seconds = abs % 60
        return (if (totalSeconds < 0) "-" else "+") + padded(abs / 3_600, 2) + ":" + padded(abs / 60 % 60, 2) +
            if (seconds == 0) "" else ":" + padded(seconds, 2)
    }

    private fun formatWeight(kg: Double, unit: WeightUnit): String =
        formatNumber(if (unit == WeightUnit.Pounds) Units.kgToLb(kg) else kg)

    private fun formatNumber(value: Double): String =
        formatFixed(value, 6).trimEnd('0').trimEnd('.')

    private fun escape(value: String, delimiter: Char): String {
        if (value.none { it == delimiter || it == '"' || it == '\r' || it == '\n' }) return value
        return "\"${value.replace("\"", "\"\"")}\""
    }

    private fun String.ensureExtension(extension: String): String =
        if (endsWith(".$extension", ignoreCase = true)) this else "$this.$extension"
}
