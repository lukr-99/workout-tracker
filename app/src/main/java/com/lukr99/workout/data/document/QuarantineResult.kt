package com.lukr99.workout.data.document

/** The outcome of [DocumentStore.quarantine]. */
sealed interface QuarantineResult {
    data object Added : QuarantineResult

    /** An entry with this key was set aside earlier. It was left as it is. */
    data object AlreadyThere : QuarantineResult

    data class Failed(val reason: String) : QuarantineResult
}
