package com.lukr99.workout.web.storage

import com.lukr99.workout.data.document.DocumentLoad
import com.lukr99.workout.data.document.DocumentSave
import com.lukr99.workout.data.document.StorageState
import com.lukr99.workout.domain.Exercise
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

private fun indexedDbMissing(): Boolean = js("typeof indexedDB === 'undefined'")

/**
 * Node has no IndexedDB, which is exactly a browser that refuses storage: the store must say so
 * and the app must run from memory with the status set, never silently.
 */
class IndexedDbDocumentStoreTest {

    @Test
    fun withoutIndexedDbTheStoreSaysStorageIsUnavailable() = runTest {
        if (!indexedDbMissing()) return@runTest
        val store = IndexedDbDocumentStore()
        val load = store.load()
        assertIs<DocumentLoad.Unavailable>(load)
        assertEquals("IndexedDB is not available", load.reason)
        assertIs<DocumentSave.Failed>(store.save("{}", 0))
        assertTrue(store.quarantined().isEmpty())
    }

    @Test
    fun withoutIndexedDbTheAppRunsInMemoryAndSaysSo() = runTest {
        if (!indexedDbMissing()) return@runTest
        val storage = WebStorage(backgroundScope)
        storage.repository.ensureSeeded()
        storage.repository.saveExercise(Exercise(id = "x", name = "Only in memory"))
        assertIs<StorageState.MemoryOnly>(storage.status.value.state)
        assertEquals("Only in memory", storage.repository.getExercise("x")?.name)
        assertEquals(17, storage.repository.countExercises())
    }
}
