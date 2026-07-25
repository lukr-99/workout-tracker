package com.lukr99.workout.data.importer

import com.lukr99.workout.data.WorkoutRepository
import com.lukr99.workout.data.export.ExportBundle
import com.lukr99.workout.data.export.JsonExporter

/**
 * Reads our own `ExportBundle` JSON back into the store (round-trip / restore-from-backup).
 *
 * Accepts `exportFormatVersion` `1.0` and `1.1`; unknown fields are ignored by [JsonExporter]. Ids
 * are preserved so a restore is idempotent and cross-device merges keep stable identity. The Lyfta
 * CSV importer is a separate, later concern — see [LyftaCsvImporter].
 */
class BundleImporter(private val repository: WorkoutRepository) {

    /** Parse + validate + apply a bundle from raw JSON. Returns the counts written. */
    suspend fun importJson(text: String): ImportResult {
        val bundle = JsonExporter.fromJson(text)
        require(bundle.exportFormatVersion in ExportBundle.SUPPORTED_VERSIONS) {
            "Unsupported exportFormatVersion '${bundle.exportFormatVersion}' " +
                "(supported: ${ExportBundle.SUPPORTED_VERSIONS.joinToString()})"
        }
        return importBundle(bundle)
    }

    suspend fun importBundle(bundle: ExportBundle): ImportResult {
        repository.importBundle(bundle)
        return ImportResult(
            exercises = bundle.exercises.size,
            templates = bundle.templates.size,
            sessions = bundle.sessions.size,
        )
    }

    data class ImportResult(val exercises: Int, val templates: Int, val sessions: Int)
}
