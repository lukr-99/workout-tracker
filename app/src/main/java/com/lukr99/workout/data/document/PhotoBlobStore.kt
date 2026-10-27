package com.lukr99.workout.data.document

/**
 * Where the web edition's exercise photos live: one JPEG per key. IndexedDB in the browser, an
 * in-memory fake in tests. [DocumentPhotoArchive] holds the logic; a store only reads and writes.
 */
interface PhotoBlobStore {
    /** Every stored photo by key. Empty when storage cannot be read. */
    suspend fun loadAll(): Map<String, ByteArray>

    /** Writes [bytes] under [key]. Throws [PhotoStorageException] with a readable reason on failure. */
    suspend fun put(key: String, bytes: ByteArray)

    /** Removes these keys. A missing key is ignored. */
    suspend fun delete(keys: Collection<String>)
}
