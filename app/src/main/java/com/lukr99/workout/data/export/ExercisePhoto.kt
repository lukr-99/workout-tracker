package com.lukr99.workout.data.export

import kotlinx.serialization.Serializable

/**
 * A personal exercise photo inside a backup (export 1.8): JPEG bytes as Base64, scaled down to at
 * most [MAX_EDGE_PX] on the long side so a backup stays well under the import size limit.
 */
@Serializable
data class ExercisePhoto(
    val exerciseId: String,
    val mimeType: String = "image/jpeg",
    val dataBase64: String,
) {
    companion object {
        const val MAX_EDGE_PX = 1600

        /** A restored photo larger than this is refused rather than written to disk. */
        const val MAX_BYTES = 8 * 1024 * 1024
    }
}
