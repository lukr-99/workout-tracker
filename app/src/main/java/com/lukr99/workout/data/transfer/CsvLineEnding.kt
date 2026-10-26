package com.lukr99.workout.data.transfer

enum class CsvLineEnding(val value: String) {
    Lf("\n"),
    CrLf("\r\n"),

    /** Android always uses a line feed, so this is the same on every platform. */
    Platform("\n"),
}
