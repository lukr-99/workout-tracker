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
 * **Version 1.6** adds combinable set tags plus per-exercise unit and timing metadata.
 * **Version 1.7** adds exercise how-to steps (`instructions`) and a guide link (`videoUrl`).
 * **Version 1.8** makes the bundle a full backup: the app version that wrote it, the owner's
 * settings, and personal exercise photos (see [ExercisePhoto]).
 * **Version 1.9** adds a plan to template exercises: target sets, a rep range, rest and a superset
 * group (all optional).
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
    /** versionName of the app that wrote the bundle, like "2.6.0". Null before 1.8. */
    val appVersion: String? = null,
    val settings: SettingsSnapshot? = null,
    val photos: List<ExercisePhoto> = emptyList(),
) {
    companion object {
        const val CURRENT_VERSION = "1.9"
        val SUPPORTED_VERSIONS = setOf("1.0", "1.1", "1.2", "1.3", "1.4", "1.5", "1.6", "1.7", "1.8", "1.9")
    }
}
