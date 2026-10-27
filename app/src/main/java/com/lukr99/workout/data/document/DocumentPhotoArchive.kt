package com.lukr99.workout.data.document

import com.lukr99.workout.data.export.ExercisePhoto
import com.lukr99.workout.data.images.ExercisePhotos
import com.lukr99.workout.data.transfer.PhotoArchive
import com.lukr99.workout.domain.newId

/**
 * The web edition's photos, for the editor ([ExercisePhotos]) and for backup and restore
 * ([PhotoArchive]). Every photo is also kept in memory, because the screens read them while
 * drawing; [load] fills the memory before the first frame. A write goes to the [store] first and is
 * only visible once it is stored, so a full device fails the save with a message instead of
 * showing a photo that would be gone after a restart.
 *
 * A key looks like `photo:<exercise id>-<random>`. A new photo gets a new key, so the old photo
 * stays until the exercise points at the new one.
 */
class DocumentPhotoArchive(
    private val store: PhotoBlobStore,
    private val newKeyPart: () -> String = { newId().take(8) },
) : ExercisePhotos, PhotoArchive {
    private val photos = linkedMapOf<String, ByteArray>()

    /** Reads every stored photo into memory. Call before the first frame, and again when another tab saved. */
    suspend fun load() {
        val stored = store.loadAll()
        photos.clear()
        photos.putAll(stored)
    }

    /** The bytes of the photo with this key, or null when it is not stored. */
    fun bytes(key: String): ByteArray? = photos[key]

    override suspend fun savePhoto(exerciseId: String, bytes: ByteArray): String = write(exerciseId, bytes)

    override suspend fun removePhoto(path: String?) {
        if (path != null) delete(path)
    }

    override fun exportBytes(path: String): ByteArray? = photos[path]

    override fun exists(path: String): Boolean = path in photos

    override suspend fun stage(exerciseId: String, bytes: ByteArray): String = write(exerciseId, bytes)

    override suspend fun delete(path: String) {
        if (photos.remove(path) != null) store.delete(listOf(path))
    }

    override suspend fun deleteAllExcept(keep: Set<String>) {
        val drop = photos.keys.filter { it !in keep }
        if (drop.isEmpty()) return
        drop.forEach(photos::remove)
        store.delete(drop)
    }

    private suspend fun write(exerciseId: String, bytes: ByteArray): String {
        require(exerciseId.matches(ID)) { "Invalid exercise id." }
        require(bytes.isNotEmpty()) { "The selected image was empty." }
        require(bytes.size <= ExercisePhoto.MAX_BYTES) { "A photo in the backup is too large." }
        val key = "$PREFIX$exerciseId-${newKeyPart()}"
        store.put(key, bytes)
        photos[key] = bytes
        return key
    }

    private companion object {
        const val PREFIX = "photo:"
        val ID = Regex("[A-Za-z0-9_-]+")
    }
}
