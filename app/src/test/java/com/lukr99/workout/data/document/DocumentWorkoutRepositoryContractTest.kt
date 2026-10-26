package com.lukr99.workout.data.document

import com.lukr99.workout.data.WorkoutRepositoryContract
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals

/**
 * The repository contract against the web edition's document store. After each scenario, every
 * store is read back from its saved text, which must give the same document as the one in memory.
 */
class DocumentWorkoutRepositoryContractTest : WorkoutRepositoryContract() {

    private val opened = mutableListOf<Pair<FakeDocumentStore, DocumentDatabase>>()

    override fun newStore(): Store {
        val store = FakeDocumentStore()
        val database = DocumentDatabase(store.asStore())
        opened += store to database
        return Store(DocumentWorkoutRepository(database), DocumentRunRepository(database))
    }

    @After
    fun everythingWasSaved() = runBlocking {
        for ((store, database) in opened) {
            database.open()
            assertEquals(StorageStatus(StorageState.Saved), database.status.value)
            val reopened = DocumentDatabase(store.asStore())
            assertEquals(database.current(), reopened.current())
        }
    }
}
