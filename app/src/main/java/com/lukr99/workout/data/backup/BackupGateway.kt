package com.lukr99.workout.data.backup

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract

internal interface BackupGateway {
    suspend fun list(treeUri: String): List<BackupDocument>
    suspend fun write(treeUri: String, fileName: String, bytes: ByteArray): BackupDocument
    suspend fun delete(document: BackupDocument)
}

internal class SafBackupGateway(context: Context) : BackupGateway {
    private val resolver = context.applicationContext.contentResolver

    override suspend fun list(treeUri: String): List<BackupDocument> {
        val tree = Uri.parse(treeUri)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            tree,
            DocumentsContract.getTreeDocumentId(tree),
        )
        val documents = mutableListOf<BackupDocument>()
        resolver.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            ),
            null,
            null,
            null,
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameIndex =
                cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val modifiedIndex =
                cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
            while (cursor.moveToNext()) {
                val documentUri = DocumentsContract.buildDocumentUriUsingTree(
                    tree,
                    cursor.getString(idIndex),
                )
                documents += BackupDocument(
                    uri = documentUri.toString(),
                    name = cursor.getString(nameIndex).orEmpty(),
                    lastModifiedUtcMillis =
                        if (modifiedIndex >= 0 && !cursor.isNull(modifiedIndex)) {
                            cursor.getLong(modifiedIndex)
                        } else {
                            0
                        },
                )
            }
        }
        return documents
    }

    override suspend fun write(
        treeUri: String,
        fileName: String,
        bytes: ByteArray,
    ): BackupDocument {
        val tree = Uri.parse(treeUri)
        val parent = DocumentsContract.buildDocumentUriUsingTree(
            tree,
            DocumentsContract.getTreeDocumentId(tree),
        )
        val uri = requireNotNull(
            DocumentsContract.createDocument(resolver, parent, JsonMimeType, fileName),
        ) { "The selected backup folder did not create '$fileName'." }
        resolver.openOutputStream(uri, "w").use { output ->
            requireNotNull(output) { "The selected backup file could not be opened." }
            output.write(bytes)
        }
        return BackupDocument(uri.toString(), fileName, System.currentTimeMillis())
    }

    override suspend fun delete(document: BackupDocument) {
        check(DocumentsContract.deleteDocument(resolver, Uri.parse(document.uri))) {
            "Could not remove expired backup '${document.name}'."
        }
    }

    companion object {
        const val JsonMimeType = "application/json"
    }
}

internal fun ContentResolver.persistBackupTreePermission(uri: Uri) {
    takePersistableUriPermission(
        uri,
        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
            android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
    )
}
