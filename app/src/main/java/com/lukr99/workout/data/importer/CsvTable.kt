package com.lukr99.workout.data.importer

internal data class CsvTable(
    val headers: List<String>,
    val records: List<CsvRecord>,
    val delimiter: Char,
)
