package com.lukr99.workout.data.importer

import com.lukr99.workout.data.export.ExportBundle
import com.lukr99.workout.data.export.JsonExporter
import com.lukr99.workout.data.transfer.DataFormat
import com.lukr99.workout.data.transfer.ImportContext
import com.lukr99.workout.data.transfer.ImportOptions
import com.lukr99.workout.data.transfer.ImportedPayload
import com.lukr99.workout.data.transfer.TextDataImporter
import com.lukr99.workout.data.transfer.TransferIssue
import com.lukr99.workout.data.transfer.TransferIssueSeverity

/** Side-effect-free JSON adapter used by the preview/plan/commit pipeline. */
internal object BundleTextImporter : TextDataImporter {
    override val format = DataFormat.WorkoutJson

    override fun confidence(text: String, fileName: String?): Int {
        var score = 0
        if (text.trimStart().startsWith('{')) score += 20
        if ("\"exportFormatVersion\"" in text) score += 50
        if ("\"sessions\"" in text && "\"exercises\"" in text) score += 20
        if (fileName?.endsWith(".json", ignoreCase = true) == true) score += 10
        return score.coerceAtMost(100)
    }

    override fun parse(
        text: String,
        context: ImportContext,
        options: ImportOptions,
        sourceLabel: String?,
    ): ImportedPayload {
        val bundle = runCatching { JsonExporter.fromJson(text) }.getOrElse {
            return ImportedPayload(
                format = format,
                issues = listOf(
                    TransferIssue(
                        "json.parse",
                        it.message ?: "Could not parse workout JSON.",
                        TransferIssueSeverity.Error,
                    ),
                ),
                sourceLabel = sourceLabel,
            )
        }
        if (bundle.exportFormatVersion !in ExportBundle.SUPPORTED_VERSIONS) {
            return ImportedPayload(
                format = format,
                issues = listOf(
                    TransferIssue(
                        "json.version",
                        "Unsupported export version '${bundle.exportFormatVersion}'.",
                        TransferIssueSeverity.Error,
                    ),
                ),
                sourceLabel = sourceLabel,
            )
        }
        return ImportedPayload(
            format = format,
            exercises = bundle.exercises,
            templates = bundle.templates,
            sessions = bundle.sessions,
            runs = bundle.runs,
            routes = bundle.routes,
            photos = bundle.photos,
            settings = bundle.settings,
            sourceLabel = sourceLabel,
            metadata = buildMap {
                put("exportFormatVersion", bundle.exportFormatVersion)
                bundle.appVersion?.let { put("appVersion", it) }
                put("exportedAtUtc", bundle.exportedAtUtc)
            },
        )
    }
}
