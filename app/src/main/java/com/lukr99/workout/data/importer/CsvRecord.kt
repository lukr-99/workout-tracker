package com.lukr99.workout.data.importer

internal data class CsvRecord(
    val rowNumber: Int,
    val values: Map<String, String>,
    val rawValues: List<String>,
) {
    operator fun get(vararg aliases: String): String? {
        aliases.forEach { alias ->
            values[CsvReader.normalizeHeader(alias)]?.let { return it.trim().takeUnless(String::isBlank) }
        }
        return null
    }
}
