package com.lukr99.workout.data.images

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import com.lukr99.workout.data.export.ExercisePhoto
import com.lukr99.workout.data.transfer.PhotoArchive
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

/** App-private file boundary for personal exercise photos, and their side of backup and restore. */
class ExercisePhotoStore(private val context: Context) : PhotoArchive {
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

    // --- Backup and restore (PhotoArchive) ------------------------------------------------------

    override fun exportBytes(path: String): ByteArray? {
        val file = File(path).takeIf { it.parentFile == photosDirectory && it.isFile } ?: return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val longEdge = maxOf(bounds.outWidth, bounds.outHeight)
        // Decode at a power-of-two reduction first so a 12 MP photo never needs a full-size bitmap.
        var sample = 1
        while (longEdge / (sample * 2) >= ExercisePhoto.MAX_EDGE_PX) sample *= 2
        val decoded = BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: return null
        val scale = ExercisePhoto.MAX_EDGE_PX.toFloat() / maxOf(decoded.width, decoded.height)
        val sized = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                decoded,
                (decoded.width * scale).toInt().coerceAtLeast(1),
                (decoded.height * scale).toInt().coerceAtLeast(1),
                true,
            )
        } else {
            decoded
        }
        return ByteArrayOutputStream().use { out ->
            sized.compress(Bitmap.CompressFormat.JPEG, EXPORT_JPEG_QUALITY, out)
            if (sized !== decoded) sized.recycle()
            decoded.recycle()
            out.toByteArray()
        }
    }

    override fun exists(path: String): Boolean = File(path).let { it.parentFile == photosDirectory && it.isFile }

    override fun stage(exerciseId: String, bytes: ByteArray): String {
        require(exerciseId.matches(Regex("[A-Za-z0-9_-]+"))) { "Invalid exercise id." }
        require(bytes.size in 1..ExercisePhoto.MAX_BYTES) { "A photo in the backup is empty or too large." }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "A photo in the backup is not an image." }
        val destination = File(photosDirectory, "$exerciseId-${UUID.randomUUID().toString().take(8)}.jpg")
        val pending = File(photosDirectory, "${destination.nameWithoutExtension}.pending")
        runCatching { pending.writeBytes(bytes) }.onFailure {
            pending.delete()
            throw it
        }
        check(pending.renameTo(destination)) { "Could not save a restored photo." }
        return destination.absolutePath
    }

    override fun delete(path: String) = removePhoto(path)

    override fun deleteAllExcept(keep: Set<String>) {
        photosDirectory.listFiles().orEmpty()
            .filter { it.isFile && it.absolutePath !in keep }
            .forEach(File::delete)
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

    private companion object {
        const val EXPORT_JPEG_QUALITY = 85
    }
}
