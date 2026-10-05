package com.lukr99.workout.data.transfer

enum class CsvLineEnding(val value: String) {
    Lf("\n"),
    CrLf("\r\n"),
    Platform(System.lineSeparator()),
}
