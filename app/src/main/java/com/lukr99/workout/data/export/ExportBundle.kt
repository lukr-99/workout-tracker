package com.lukr99.workout.data.export

import com.lukr99.workout.domain.Exercise
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.domain.WorkoutTemplate
import java.time.Instant
import kotlinx.serialization.Serializable

/**
 * The cross-device contract (01-architecture.md "Portability seam"): a versioned, `@Serializable`
 * snapshot of the whole store. Mirrors the MAUI `ExportBundle`, referencing the same portable
 * `domain/` models (which serialize enums as Int ordinals and timestamps as ISO-8601 — matching the
 * `v1.0` wire format).
 *
 * **Version 1.1** — the rework's additive fields (03-data-model.md) now ship. The reader
 * ([BundleImporter]) accepts `1.0` **and** `1.1` and ignores unknown fields, so older exports and
 * any future desktop tool interoperate.
 */
@Serializable
data class ExportBundle(
    val exportedAtUtc: String = Instant.now().toString(),
    val exportFormatVersion: String = CURRENT_VERSION,
    val exercises: List<Exercise> = emptyList(),
    val templates: List<WorkoutTemplate> = emptyList(),
    val sessions: List<WorkoutSession> = emptyList(),
) {
    companion object {
        const val CURRENT_VERSION = "1.2"
        val SUPPORTED_VERSIONS = setOf("1.0", "1.1", "1.2")
    }
}
