package com.lukr99.workout.data.transfer

data class ExportArtifact(
    val fileName: String,
    val mimeType: String,
    val format: DataFormat,
    val text: String,
    val recordCount: Int,
)
