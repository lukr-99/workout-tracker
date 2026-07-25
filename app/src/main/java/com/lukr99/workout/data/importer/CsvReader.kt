package com.lukr99.workout.data.importer

/**
 * Small RFC-4180 reader with quoted delimiters, escaped quotes, embedded newlines, BOM handling,
 * trailing empty fields, and configurable delimiters. Keeping it dependency-free makes importers
 * portable to a future desktop module.
 */
internal object CsvReader {
    fun parse(text: String, delimiter: Char = detectDelimiter(text)): CsvTable {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var quoted = false
        var index = 0
        val source = text.removePrefix("\uFEFF")

        fun endField() {
            row += field.toString()
            field.clear()
        }

        fun endRow() {
            endField()
            if (row.any(String::isNotEmpty)) rows += row.toList()
            row = mutableListOf()
        }

        while (index < source.length) {
            val char = source[index]
            when {
                quoted && char == '"' && index + 1 < source.length && source[index + 1] == '"' -> {
                    field.append('"')
                    index++
                }
                char == '"' -> quoted = !quoted
                !quoted && char == delimiter -> endField()
                !quoted && (char == '\r' || char == '\n') -> {
                    if (char == '\r' && index + 1 < source.length && source[index + 1] == '\n') index++
                    endRow()
                }
                else -> field.append(char)
            }
            index++
        }
        if (quoted) throw CsvParseException("Unclosed quoted field at end of CSV.")
        if (field.isNotEmpty() || row.isNotEmpty()) endRow()
        if (rows.isEmpty()) return CsvTable(emptyList(), emptyList(), delimiter)

        val headers = rows.first().mapIndexed { column, raw ->
            normalizeHeader(raw).ifBlank { "column$column" }
        }
        val records = rows.drop(1).mapIndexed { rowIndex, values ->
            CsvRecord(
                rowNumber = rowIndex + 2,
                values = headers.mapIndexed { column, header -> header to values.getOrElse(column) { "" } }
                    .toMap(),
                rawValues = values,
            )
        }
        return CsvTable(headers, records, delimiter)
    }

    fun detectDelimiter(text: String): Char {
        val firstLogicalLine = text.lineSequence().firstOrNull { it.isNotBlank() }.orEmpty()
        val candidates = listOf(',', ';', '\t', '|')
        return candidates.maxByOrNull { delimiter ->
            var count = 0
            var quoted = false
            firstLogicalLine.forEach { char ->
                if (char == '"') quoted = !quoted
                else if (!quoted && char == delimiter) count++
            }
            count
        } ?: ','
    }

    fun normalizeHeader(value: String): String = value
        .trim()
        .lowercase()
        .filter(Char::isLetterOrDigit)
}

internal data class CsvTable(
    val headers: List<String>,
    val records: List<CsvRecord>,
    val delimiter: Char,
)

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

internal class CsvParseException(message: String) : IllegalArgumentException(message)
