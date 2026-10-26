package com.lukr99.workout.data.document

/** What [DocumentStore.load] found. */
sealed interface DocumentLoad {
    /** Nothing was ever saved here. */
    data object Empty : DocumentLoad

    /** The stored text and its revision. The text may still turn out to be unreadable. */
    data class Found(val text: String, val revision: Long) : DocumentLoad

    /** The browser refuses storage (blocked, private mode, no IndexedDB). */
    data class Unavailable(val reason: String) : DocumentLoad
}
