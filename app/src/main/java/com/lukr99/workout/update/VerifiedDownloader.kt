package com.lukr99.workout.update

import java.io.File
import java.io.IOException
import java.security.MessageDigest
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

/**
 * Downloads an update into a private cache folder and proves it is the published file: every byte
 * GitHub announced has to arrive, and the SHA-256 has to match the published checksum. The APK
 * streams to a `.part` file that is renamed into place only after both checks, so a partial or
 * wrong file can never reach the installer. A dropped connection ends the stream without an error,
 * which is why the length check matters (docs/pitfalls.md, 2.5.2).
 */
class VerifiedDownloader(
    private val http: UpdateHttp,
    private val directory: File,
) {
    suspend fun download(asset: ReleaseAsset, expectedSha256: String, onProgress: (Float) -> Unit = {}): File {
        directory.mkdirs()
        directory.listFiles()?.forEach { it.delete() } // drop any stale download
        val out = File(directory, asset.name.replace(Regex("[^A-Za-z0-9._-]"), "_"))
        val part = File(directory, out.name + ".part")
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            var copied = 0L
            http.open(asset.url).use { response ->
                val expected = if (asset.size > 0) asset.size else response.contentLength
                var lastPercent = -1
                part.outputStream().buffered(BUFFER_BYTES).use { output ->
                    val buffer = ByteArray(BUFFER_BYTES)
                    while (true) {
                        coroutineContext.ensureActive()
                        val read = response.body.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        digest.update(buffer, 0, read)
                        copied += read
                        if (expected > 0) {
                            val percent = ((copied * 100) / expected).toInt()
                            if (percent != lastPercent) {
                                lastPercent = percent
                                onProgress(copied.toFloat() / expected)
                            }
                        }
                    }
                }
                if (expected > 0 && copied != expected) {
                    throw IOException("The download stopped early: got $copied of $expected bytes.")
                }
            }
            val actual = digest.digest().joinToString("") { "%02x".format(it) }
            if (actual != expectedSha256.lowercase()) {
                throw IOException("The downloaded update does not match its published checksum.")
            }
            if (out.exists() && !out.delete()) throw IOException("Could not replace the previous download.")
            if (!part.renameTo(out)) throw IOException("Could not finish the downloaded update.")
            return out
        } catch (failure: Throwable) {
            part.delete()
            throw failure
        }
    }

    private companion object {
        const val BUFFER_BYTES = 64 * 1024
    }
}
