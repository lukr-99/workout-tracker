package com.lukr99.workout.data.document

/** Whether changes reach storage. The UI shows every state except [Saved] and [Opening]. */
sealed interface StorageState {
    /** The stored data is being read. */
    data object Opening : StorageState

    /** Everything is saved. */
    data object Saved : StorageState

    /** The last save failed. Changes are kept in memory and saved with the next change or a retry. */
    data class SaveFailed(val reason: String) : StorageState

    /** Nothing is saved: data lives in this page only until it closes. */
    data class MemoryOnly(val reason: String) : StorageState
}
