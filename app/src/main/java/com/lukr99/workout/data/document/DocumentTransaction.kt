package com.lukr99.workout.data.document

import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

/**
 * A transaction in progress on [owner], carried in the coroutine context. Reads inside it see
 * [working]; writes change [working] and are recorded, and nothing is saved until the block ends.
 */
internal class DocumentTransaction(
    val owner: DocumentDatabase,
    var working: WorkoutDocument,
) : AbstractCoroutineContextElement(Key) {

    val changes = mutableListOf<DocumentChange>()

    fun apply(change: DocumentChange) {
        working = change(working)
        changes += change
    }

    companion object Key : CoroutineContext.Key<DocumentTransaction>
}
