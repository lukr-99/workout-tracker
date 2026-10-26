package com.lukr99.workout.data.document

/** The outcome of [DocumentStore.save]. */
sealed interface DocumentSave {
    /** Written; [revision] is the new stored revision. */
    data class Saved(val revision: Long) : DocumentSave

    /** Another writer saved first: the stored revision is [storedRevision], not the expected one. */
    data class Conflict(val storedRevision: Long) : DocumentSave

    /** Not written, for example over quota or blocked storage. */
    data class Failed(val reason: String) : DocumentSave
}
