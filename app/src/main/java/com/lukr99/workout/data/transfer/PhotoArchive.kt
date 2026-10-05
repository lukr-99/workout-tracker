package com.lukr99.workout.data.transfer

/**
 * The file side of personal exercise photos, as backup and restore need it. Paths are the absolute
 * paths stored in `Exercise.localImagePath`. The Android implementation is
 * [com.lukr99.workout.data.images.ExercisePhotoStore]; tests use a fake.
 */
interface PhotoArchive {
    /** JPEG bytes for a backup, scaled down to fit; null when the file is missing or unreadable. */
    fun exportBytes(path: String): ByteArray?

    fun exists(path: String): Boolean

    /**
     * Writes [bytes] as a new photo file for [exerciseId] and returns its path. The name is fresh,
     * so an existing photo is never overwritten before the database points at the new one.
     */
    fun stage(exerciseId: String, bytes: ByteArray): String

    fun delete(path: String)

    /** Deletes every stored photo whose path is not in [keep]. */
    fun deleteAllExcept(keep: Set<String>)
}
