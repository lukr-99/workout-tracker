package com.lukr99.workout.web.storage

import com.lukr99.workout.data.document.PhotoBlobStore
import com.lukr99.workout.data.document.PhotoStorageException
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.coroutines.await

/**
 * The [PhotoBlobStore] in the `photos` store of the `ember` IndexedDB database. It keeps its own
 * connection to the database, next to the document store's.
 *
 * A photo that cannot be written (a full device, blocked storage) throws a [PhotoStorageException]
 * with a sentence the editor shows as it is.
 */
@OptIn(ExperimentalEncodingApi::class)
class IndexedDbPhotoBlobStore(
    private val databaseName: String = "ember",
    private val openTimeoutMs: Int = 10_000,
) : PhotoBlobStore {

    private var db: JsAny? = null
    private var openFailure: String? = null

    private suspend fun database(): JsAny? {
        db?.let { return it }
        if (openFailure != null) return null
        val opened = idbOpen(databaseName, DATABASE_VERSION, openTimeoutMs).await<JsAny>()
        val handle = anyField(opened, "db")
        if (handle != null && textField(opened, "ok") == "true") {
            db = handle
        } else {
            openFailure = textField(opened, "error").ifBlank { "IndexedDB could not be opened" }
        }
        return db
    }

    override suspend fun loadAll(): Map<String, ByteArray> {
        val handle = database() ?: return emptyMap()
        val result = idbPhotosLoadAll(handle).await<JsAny>()
        if (textField(result, "state") != "found") return emptyMap()
        val keys = textArrayField(result, "keys")
        val data = textArrayField(result, "data")
        return buildMap {
            repeat(arrayLength(keys)) { index ->
                val bytes = runCatching { Base64.Default.decode(arrayText(data, index)) }.getOrNull() ?: return@repeat
                put(arrayText(keys, index), bytes)
            }
        }
    }

    override suspend fun put(key: String, bytes: ByteArray) {
        val handle = database() ?: throw PhotoStorageException("Photos cannot be saved: ${openFailure.orEmpty()}.")
        val result = idbPhotoPut(handle, key, Base64.Default.encode(bytes)).await<JsAny>()
        if (textField(result, "state") == "saved") return
        throw PhotoStorageException(
            if (textField(result, "full") == "true") {
                "This device is out of storage space. Free some space, or remove photos you do not need, and try again."
            } else {
                "The photo could not be saved: ${textField(result, "error")}"
            },
        )
    }

    override suspend fun delete(keys: Collection<String>) {
        val handle = database() ?: return
        idbPhotosDelete(handle, keys.joinToString("\n")).await<JsAny>()
    }

    private companion object {
        /** Must match the version in [IndexedDbDocumentStore]. */
        const val DATABASE_VERSION = 1
    }
}
