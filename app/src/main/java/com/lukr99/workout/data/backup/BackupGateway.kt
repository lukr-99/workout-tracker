package com.lukr99.workout.data.backup

import android.content.ContentResolver
import android.net.Uri

internal interface BackupGateway {
    suspend fun list(treeUri: String): List<BackupDocument>
    suspend fun write(treeUri: String, fileName: String, bytes: ByteArray): BackupDocument
    suspend fun delete(document: BackupDocument)
}

internal fun ContentResolver.persistBackupTreePermission(uri: Uri) {
    takePersistableUriPermission(
        uri,
        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
            android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
    )
}
