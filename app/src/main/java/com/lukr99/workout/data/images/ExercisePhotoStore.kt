package com.lukr99.workout.data.images

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

data class PhotoCaptureTarget(
    val uri: Uri,
    val temporaryPath: String,
)

/** App-private file boundary for personal exercise photos. */
class ExercisePhotoStore(private val context: Context) {
    private val photosDirectory: File
        get() = File(context.filesDir, "exercise_images").also(File::mkdirs)
    private val capturesDirectory: File
        get() = File(context.cacheDir, "exercise-photo-captures").also(File::mkdirs)

    fun createCaptureTarget(): PhotoCaptureTarget {
        val file = File(capturesDirectory, "${UUID.randomUUID()}.jpg")
        check(file.createNewFile()) { "Could not create a camera image target." }
        return PhotoCaptureTarget(
            uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file),
            temporaryPath = file.absolutePath,
        )
    }

    fun commitCapture(exerciseId: String, temporaryPath: String): String {
        val source = File(temporaryPath)
        require(source.isFile && source.length() > 0) { "The camera did not return an image." }
        return replacePhoto(exerciseId) { destination ->
            source.inputStream().use { input ->
                destination.outputStream().use(input::copyTo)
            }
        }.also { source.delete() }
    }

    fun importPhoto(exerciseId: String, uri: Uri): String = replacePhoto(exerciseId) { destination ->
        val input = context.contentResolver.openInputStream(uri)
            ?: error("The selected image could not be opened.")
        input.use { destination.outputStream().use(it::copyTo) }
    }

    fun discardCapture(temporaryPath: String?) {
        temporaryPath?.let(::File)?.takeIf { it.parentFile == capturesDirectory }?.delete()
    }

    fun removePhoto(path: String?) {
        path?.let(::File)
            ?.takeIf { it.parentFile == photosDirectory }
            ?.delete()
    }

    private inline fun replacePhoto(exerciseId: String, write: (File) -> Unit): String {
        require(exerciseId.matches(Regex("[A-Za-z0-9_-]+"))) { "Invalid exercise id." }
        val destination = File(photosDirectory, "$exerciseId.jpg")
        val pending = File(photosDirectory, "$exerciseId.pending")
        runCatching { write(pending) }.onFailure {
            pending.delete()
            throw it
        }
        if (pending.length() == 0L) {
            pending.delete()
            error("The selected image was empty.")
        }
        if (destination.exists()) check(destination.delete()) { "Could not replace the old photo." }
        check(pending.renameTo(destination)) { "Could not save the exercise photo." }
        return destination.absolutePath
    }
}
