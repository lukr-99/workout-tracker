package com.lukr99.workout.data.export

import com.lukr99.workout.domain.Exercise
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.domain.WorkoutTemplate
import com.lukr99.workout.domain.run.Route
import com.lukr99.workout.domain.run.Run
import java.time.Instant
import kotlinx.serialization.Serializable

/**
 * The cross-device contract (01-architecture.md "Portability seam"): a versioned, `@Serializable`
 * snapshot of the whole store. Mirrors the MAUI `ExportBundle`, referencing the same portable
 * `domain/` models (which serialize enums as Int ordinals and timestamps as ISO-8601 — matching the
 * `v1.0` wire format).
 *
 * **Version 1.5** adds Run Mode: `runs` and `routes` arrays (each carrying its full trace/points).
 * Older exports omit them (they default empty), and the reader accepts all earlier published
 * versions and ignores unknown fields, so older exports and future tools interoperate.
 */
@Serializable
data class ExportBundle(
    val exportedAtUtc: String = Instant.now().toString(),
    val exportFormatVersion: String = CURRENT_VERSION,
    val exercises: List<Exercise> = emptyList(),
    val templates: List<WorkoutTemplate> = emptyList(),
    val sessions: List<WorkoutSession> = emptyList(),
    val runs: List<Run> = emptyList(),
    val routes: List<Route> = emptyList(),
) {
    companion object {
        const val CURRENT_VERSION = "1.5"
        val SUPPORTED_VERSIONS = setOf("1.0", "1.1", "1.2", "1.3", "1.4", "1.5")
    }
}
