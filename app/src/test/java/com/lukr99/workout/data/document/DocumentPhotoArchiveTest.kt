package com.lukr99.workout.data.document

import com.lukr99.workout.data.transfer.DataStoreSettingsArchive
import com.lukr99.workout.data.transfer.DataTransferService
import com.lukr99.workout.data.transfer.ImportOptions
import com.lukr99.workout.data.transfer.JsonExportOptions
import com.lukr99.workout.data.transfer.RestoreMode
import com.lukr99.workout.domain.Exercise
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentPhotoArchiveTest {
    private val blobs = FakePhotoBlobStore()
    private var counter = 0
    private val archive = DocumentPhotoArchive(blobs) { "k${counter++}" }
    private val jpeg = byteArrayOf(1, 2, 3, 4)

    @Test
    fun aSavedPhotoIsStoredAndReadable() = runBlocking {
        val key = archive.savePhoto("ex-1", jpeg)
        assertEquals("photo:ex-1-k0", key)
        assertArrayEquals(jpeg, archive.bytes(key))
        assertArrayEquals(jpeg, blobs.stored[key])
        assertTrue(archive.exists(key))
    }

    @Test
    fun aNewPhotoGetsANewKeyAndTheOldOneStaysUntilRemoved() = runBlocking {
        val first = archive.savePhoto("ex-1", jpeg)
        val second = archive.savePhoto("ex-1", byteArrayOf(9))
        assertNotEquals(first, second)
        assertTrue(archive.exists(first))
        archive.removePhoto(first)
        assertNull(archive.bytes(first))
        assertEquals(setOf(second), blobs.stored.keys)
        archive.removePhoto(null)
        archive.removePhoto("photo:unknown")
    }

    @Test
    fun aFullDeviceFailsTheSaveAndKeepsNothing() = runBlocking {
        blobs.failPuts = "The device storage is full."
        val failure = failure<PhotoStorageException> { archive.savePhoto("ex-1", jpeg) }
        assertEquals("The device storage is full.", failure.message)
        assertTrue(blobs.stored.isEmpty())
        assertNull(archive.bytes("photo:ex-1-k0"))
    }

    @Test
    fun badInputIsRefused() = runBlocking {
        failure<IllegalArgumentException> { archive.savePhoto("../x", jpeg) }
        failure<IllegalArgumentException> { archive.savePhoto("ex-1", ByteArray(0)) }
        failure<IllegalArgumentException> { archive.savePhoto("ex-1", ByteArray(9 * 1024 * 1024)) }
        Unit
    }

    @Test
    fun loadReadsEverythingThatWasStored() = runBlocking {
        blobs.stored["photo:a-1"] = jpeg
        archive.load()
        assertArrayEquals(jpeg, archive.exportBytes("photo:a-1"))
    }

    @Test
    fun deleteAllExceptKeepsOnlyTheListedPhotos() = runBlocking {
        val a = archive.savePhoto("a", jpeg)
        val b = archive.savePhoto("b", jpeg)
        archive.deleteAllExcept(setOf(b))
        assertTrue(!archive.exists(a) && archive.exists(b))
        assertEquals(setOf(b), blobs.stored.keys)
    }

    @Test
    fun aBackupCarriesThePhotosAndRestoresThemOnAnotherBrowser() = runBlocking {
        val sourceDb = DocumentDatabase(FakeDocumentStore().asStore())
        val source = DocumentWorkoutRepository(sourceDb)
        source.ensureSeeded()
        val key = archive.savePhoto("mine", jpeg)
        source.saveExercise(Exercise(id = "mine", name = "Mine", localImagePath = key))
        val json = service(source, sourceDb, archive).exportJson(JsonExportOptions(includePhotos = true)).text
        assertTrue("\"photos\"" in json && "AQIDBA==" in json)

        val otherBlobs = FakePhotoBlobStore()
        val other = DocumentPhotoArchive(otherBlobs) { "z" }
        val targetDb = DocumentDatabase(FakeDocumentStore().asStore())
        val target = DocumentWorkoutRepository(targetDb)
        target.ensureSeeded()
        val importer = service(target, targetDb, other)
        val preview = importer.previewImport(json, "backup.json", ImportOptions(mode = RestoreMode.Merge))
        importer.commitImport(preview)

        val restored = target.getExercise("mine")?.localImagePath.also { assertNotNull(it) }!!
        assertEquals("photo:mine-z", restored)
        assertArrayEquals(jpeg, otherBlobs.stored[restored])
    }

    private suspend inline fun <reified T : Throwable> failure(block: () -> Unit): T =
        try {
            block()
            throw AssertionError("Expected " + T::class.simpleName)
        } catch (e: Throwable) {
            if (e is T) e else throw e
        }

    private fun service(repo: DocumentWorkoutRepository, db: DocumentDatabase, photos: DocumentPhotoArchive) =
        DataTransferService(
            repo,
            DocumentRunRepository(db),
            photos = photos,
            settings = DataStoreSettingsArchive(DocumentSettingsStore(db)),
            appVersion = "1.0",
        )
}
