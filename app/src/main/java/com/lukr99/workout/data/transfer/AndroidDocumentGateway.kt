package com.lukr99.workout.data.transfer

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Storage Access Framework + share-sheet gateway. It is deliberately UI-framework agnostic:
 * Compose/ActivityResult code launches the returned intents and feeds the selected [Uri] back.
 */
class AndroidDocumentGateway(
    context: Context,
    private val fileProviderAuthority: String = "${context.packageName}.files",
    private val maximumImportBytes: Long = 64L * 1024 * 1024,
) {
    private val appContext = context.applicationContext

    fun openDocumentIntent(): Intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = "*/*"
        putExtra(
            Intent.EXTRA_MIME_TYPES,
            arrayOf(DataFormat.WorkoutJson.mimeType, DataFormat.LyftaCsv.mimeType, "text/*"),
        )
    }

    fun createDocumentIntent(artifact: ExportArtifact): Intent =
        Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = artifact.mimeType
            putExtra(Intent.EXTRA_TITLE, artifact.fileName)
        }

    suspend fun readText(uri: Uri): String = withContext(Dispatchers.IO) {
        appContext.contentResolver.openAssetFileDescriptor(uri, "r")?.use { descriptor ->
            if (descriptor.length > maximumImportBytes) {
                error("Import is larger than the ${maximumImportBytes / (1024 * 1024)} MB limit.")
            }
        }
        appContext.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { reader ->
            val output = StringBuilder()
            val buffer = CharArray(DEFAULT_BUFFER_SIZE)
            var total = 0L
            while (true) {
                val count = reader.read(buffer)
                if (count < 0) break
                total += count * 2L
                require(total <= maximumImportBytes * 2) { "Import exceeds the configured size limit." }
                output.append(buffer, 0, count)
            }
            output.toString()
        } ?: error("The selected document could not be opened.")
    }

    suspend fun writeText(uri: Uri, artifact: ExportArtifact) = withContext(Dispatchers.IO) {
        appContext.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter(Charsets.UTF_8)
            ?.use { it.write(artifact.text) }
            ?: error("The selected document could not be written.")
    }

    suspend fun shareIntent(artifact: ExportArtifact): Intent = withContext(Dispatchers.IO) {
        val shareDirectory = File(appContext.cacheDir, "shared-exports").apply { mkdirs() }
        val safeName = artifact.fileName.replace(Regex("[^A-Za-z0-9._ -]"), "_")
        val file = File(shareDirectory, safeName)
        file.writeText(artifact.text, Charsets.UTF_8)
        val uri = FileProvider.getUriForFile(appContext, fileProviderAuthority, file)
        Intent(Intent.ACTION_SEND).apply {
            type = artifact.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = android.content.ClipData.newRawUri(artifact.fileName, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
