package com.lukr99.workout.data.transfer

import com.lukr99.workout.domain.query.WorkoutQuery
import java.time.ZoneId

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
