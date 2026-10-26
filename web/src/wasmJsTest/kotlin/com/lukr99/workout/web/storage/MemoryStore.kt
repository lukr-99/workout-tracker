package com.lukr99.workout.web.storage

import com.lukr99.workout.data.document.DocumentLoad
import com.lukr99.workout.data.document.DocumentSave
import com.lukr99.workout.data.document.DocumentStore
import com.lukr99.workout.data.document.QuarantineEntry
import com.lukr99.workout.data.document.QuarantineResult

/** A [DocumentStore] in memory with IndexedDB's rules, shared by two "tabs" in a test. */
class MemoryStore(var text: String? = null, var revision: Long = 0) : DocumentStore {
    val quarantine = linkedMapOf<String, QuarantineEntry>()
    var failSaves: String? = null

    override suspend fun load(): DocumentLoad = text?.let { DocumentLoad.Found(it, revision) } ?: DocumentLoad.Empty

    override suspend fun save(text: String, expectedRevision: Long): DocumentSave {
        failSaves?.let { return DocumentSave.Failed(it) }
        if (expectedRevision != revision) return DocumentSave.Conflict(revision)
        this.text = text
        revision++
        return DocumentSave.Saved(revision)
    }

    override suspend fun quarantine(entry: QuarantineEntry): QuarantineResult {
        if (entry.key in quarantine) return QuarantineResult.AlreadyThere
        quarantine[entry.key] = entry
        return QuarantineResult.Added
    }

    override suspend fun quarantined(): List<QuarantineEntry> = quarantine.values.toList()
}
