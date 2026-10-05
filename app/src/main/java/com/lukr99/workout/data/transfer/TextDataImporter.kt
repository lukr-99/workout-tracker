package com.lukr99.workout.data.transfer

interface TextDataImporter {
    val format: DataFormat
    fun confidence(text: String, fileName: String? = null): Int
    fun parse(
        text: String,
        context: ImportContext,
        options: ImportOptions,
        sourceLabel: String? = null,
    ): ImportedPayload
}
