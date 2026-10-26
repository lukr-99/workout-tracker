package com.lukr99.workout.data.document

/** An in-memory [PhotoBlobStore]. Set [failPuts] to act like a full device. */
class FakePhotoBlobStore : PhotoBlobStore {
    val stored = linkedMapOf<String, ByteArray>()
    var failPuts: String? = null

    override suspend fun loadAll(): Map<String, ByteArray> = stored.toMap()

    override suspend fun put(key: String, bytes: ByteArray) {
        failPuts?.let { throw PhotoStorageException(it) }
        stored[key] = bytes
    }

    override suspend fun delete(keys: Collection<String>) {
        keys.forEach(stored::remove)
    }
}
