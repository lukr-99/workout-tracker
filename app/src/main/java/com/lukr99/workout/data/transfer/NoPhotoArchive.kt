package com.lukr99.workout.data.transfer

/** A [PhotoArchive] with no photos, for callers and tests that do not deal with files. */
object NoPhotoArchive : PhotoArchive {
    override fun exportBytes(path: String): ByteArray? = null
    override fun exists(path: String): Boolean = false
    override suspend fun stage(exerciseId: String, bytes: ByteArray): String =
        error("This archive cannot store photos.")
    override suspend fun delete(path: String) = Unit
    override suspend fun deleteAllExcept(keep: Set<String>) = Unit
}
