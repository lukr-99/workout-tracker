package com.lukr99.workout.web.storage

import com.lukr99.workout.data.document.PhotoStorageException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

private fun indexedDbMissing(): Boolean = js("typeof indexedDB === 'undefined'")

/**
 * Node has no IndexedDB, which is a browser that refuses storage: reading finds no photos, and a
 * save fails with a sentence for the editor instead of pretending to work.
 */
class IndexedDbPhotoBlobStoreTest {
    @Test
    fun withoutIndexedDbThereAreNoPhotosAndSavingSaysWhy() = runTest {
        if (!indexedDbMissing()) return@runTest
        val store = IndexedDbPhotoBlobStore()
        assertTrue(store.loadAll().isEmpty())
        val failure = assertFailsWith<PhotoStorageException> { store.put("photo:a-1", byteArrayOf(1)) }
        assertEquals("Photos cannot be saved: IndexedDB is not available.", failure.message)
        store.delete(listOf("photo:a-1"))
    }

    @Test
    fun withoutIndexedDbAPhotoSaveFailsAndLeavesNothingInMemory() = runTest {
        if (!indexedDbMissing()) return@runTest
        val storage = WebStorage(backgroundScope)
        assertFailsWith<PhotoStorageException> { storage.photos.savePhoto("mine", byteArrayOf(1, 2)) }
        assertEquals(null, storage.photos.bytes("photo:mine-0"))
    }
}
