package com.lukr99.workout.data.transfer

enum class DataFormat(
    val defaultExtension: String,
    val mimeType: String,
) {
    WorkoutJson("json", "application/json"),
    WorkoutCsv("csv", "text/csv"),
    LyftaCsv("csv", "text/csv"),
}
