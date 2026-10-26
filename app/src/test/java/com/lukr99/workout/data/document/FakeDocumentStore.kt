package com.lukr99.workout.data.document

/**
 * An in-memory [DocumentStore] with the IndexedDB store's rules: a revision check on every save and
 * an add-only quarantine. Two [DocumentDatabase]s over one fake behave like two browser tabs.
 */
class FakeDocumentStore(
    var text: String? = null,
    var revision: Long = 0,
) {
    val quarantine = linkedMapOf<String, QuarantineEntry>()
    var saves = 0
        private set

    /** When set, saves fail with this reason (quota, blocked storage). */
    var failSaves: String? = null

    /** When set, the browser refuses storage altogether. */
    var unavailable: String? = null

    /** When set, setting data aside fails with this reason. */
    var failQuarantine: String? = null

    fun asStore(): DocumentStore = object : DocumentStore {
        override suspend fun load(): DocumentLoad {
            unavailable?.let { return DocumentLoad.Unavailable(it) }
            val stored = text ?: return DocumentLoad.Empty
            return DocumentLoad.Found(stored, revision)
        }

        override suspend fun save(text: String, expectedRevision: Long): DocumentSave {
            failSaves?.let { return DocumentSave.Failed(it) }
            if (expectedRevision != revision) return DocumentSave.Conflict(revision)
            this@FakeDocumentStore.text = text
            revision++
            saves++
            return DocumentSave.Saved(revision)
        }

        override suspend fun quarantine(entry: QuarantineEntry): QuarantineResult {
            failQuarantine?.let { return QuarantineResult.Failed(it) }
            if (entry.key in quarantine) return QuarantineResult.AlreadyThere
            quarantine[entry.key] = entry
            return QuarantineResult.Added
        }

        override suspend fun quarantined(): List<QuarantineEntry> = quarantine.values.toList()
    }

    /** The stored document, decoded. */
    fun document(): WorkoutDocument = DocumentCodec.decode(checkNotNull(text) { "nothing stored" })
}
