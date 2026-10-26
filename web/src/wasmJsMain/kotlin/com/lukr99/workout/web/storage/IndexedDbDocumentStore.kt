package com.lukr99.workout.web.storage

import com.lukr99.workout.data.document.DocumentLoad
import com.lukr99.workout.data.document.DocumentSave
import com.lukr99.workout.data.document.DocumentStore
import com.lukr99.workout.data.document.QuarantineEntry
import com.lukr99.workout.data.document.QuarantineResult
import kotlinx.coroutines.await

/**
 * The [DocumentStore] in IndexedDB: database [databaseName] with the stores `data` (one record,
 * `current`: `{ revision, format, text }`), `photos` (empty until exercise photos come to the web)
 * and `quarantine` (add only). Each save checks the revision and writes in one `readwrite`
 * transaction, and then tells other tabs on the `ember-data` channel.
 *
 * When the database cannot be opened (no IndexedDB, blocked storage, a private window that refuses
 * it), [load] says [DocumentLoad.Unavailable], and the [com.lukr99.workout.data.document.DocumentDatabase]
 * runs from memory and says so on screen.
 */
class IndexedDbDocumentStore(
    private val databaseName: String = "ember",
    private val openTimeoutMs: Int = 10_000,
) : DocumentStore {

    private var db: JsAny? = null
    private var openFailure: String? = null

    private suspend fun database(): JsAny? {
        db?.let { return it }
        if (openFailure != null) return null
        val opened = idbOpen(databaseName, VERSION, openTimeoutMs).await<JsAny>()
        val handle = anyField(opened, "db")
        if (handle != null && textField(opened, "ok") == "true") {
            db = handle
        } else {
            openFailure = textField(opened, "error").ifBlank { "IndexedDB could not be opened" }
        }
        return db
    }

    override suspend fun load(): DocumentLoad {
        val handle = database() ?: return DocumentLoad.Unavailable(openFailure.orEmpty())
        val result = idbLoad(handle).await<JsAny>()
        return when (textField(result, "state")) {
            "empty" -> DocumentLoad.Empty
            "found" -> DocumentLoad.Found(textField(result, "text"), numberField(result, "revision").toLong())
            else -> DocumentLoad.Unavailable(textField(result, "error"))
        }
    }

    override suspend fun save(text: String, expectedRevision: Long): DocumentSave {
        val handle = database() ?: return DocumentSave.Failed(openFailure.orEmpty())
        val result = idbSave(handle, text, expectedRevision.toDouble()).await<JsAny>()
        return when (textField(result, "state")) {
            "saved" -> {
                val revision = numberField(result, "revision").toLong()
                postDataChanged(revision.toDouble())
                DocumentSave.Saved(revision)
            }
            "conflict" -> DocumentSave.Conflict(numberField(result, "revision").toLong())
            else -> DocumentSave.Failed(textField(result, "error"))
        }
    }

    override suspend fun quarantine(entry: QuarantineEntry): QuarantineResult {
        val handle = database() ?: return QuarantineResult.Failed(openFailure.orEmpty())
        val result = idbQuarantine(handle, entry.key, entry.text, entry.reason, entry.atUtc.toDouble()).await<JsAny>()
        return when (textField(result, "state")) {
            "added" -> QuarantineResult.Added
            "exists" -> QuarantineResult.AlreadyThere
            else -> QuarantineResult.Failed(textField(result, "error"))
        }
    }

    override suspend fun quarantined(): List<QuarantineEntry> {
        val handle = database() ?: return emptyList()
        val result = idbQuarantined(handle).await<JsAny>()
        val items = anyField(result, "items") ?: return emptyList()
        return List(arrayLength(items)) { index ->
            val item = arrayItem(items, index)
            QuarantineEntry(
                key = textField(item, "key"),
                text = textField(item, "text"),
                reason = textField(item, "reason"),
                atUtc = numberField(item, "atUtc").toLong(),
            )
        }
    }

    private companion object {
        /** Bump with an upgrade step in [idbOpen] when the stores change. */
        const val VERSION = 1
    }
}
