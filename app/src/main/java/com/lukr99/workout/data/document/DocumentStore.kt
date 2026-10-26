package com.lukr99.workout.data.document

/**
 * Where the [WorkoutDocument] text lives: one record with a revision number, plus a quarantine for
 * data that could not be read. The web edition uses IndexedDB; tests use an in-memory fake.
 * [DocumentDatabase] holds all the logic; a store only reads and writes.
 */
interface DocumentStore {
    suspend fun load(): DocumentLoad

    /**
     * Writes [text] as revision [expectedRevision] + 1, but only if the stored revision is still
     * [expectedRevision] (0 when nothing is stored). The check and the write are one atomic step.
     */
    suspend fun save(text: String, expectedRevision: Long): DocumentSave

    /** Adds [entry]. An existing entry with the same key is never overwritten. */
    suspend fun quarantine(entry: QuarantineEntry): QuarantineResult

    /** Everything set aside so far, oldest first. */
    suspend fun quarantined(): List<QuarantineEntry>
}
