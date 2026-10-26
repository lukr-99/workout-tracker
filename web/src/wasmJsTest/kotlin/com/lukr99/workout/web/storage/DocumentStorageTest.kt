package com.lukr99.workout.web.storage

import com.lukr99.workout.data.document.DocumentCodec
import com.lukr99.workout.data.document.DocumentDatabase
import com.lukr99.workout.data.document.DocumentRunRepository
import com.lukr99.workout.data.document.DocumentWorkoutRepository
import com.lukr99.workout.data.document.StorageState
import com.lukr99.workout.data.document.WorkoutDocument
import com.lukr99.workout.data.export.ExportBundle
import com.lukr99.workout.data.export.SettingsSnapshot
import com.lukr99.workout.domain.CardioEntryData
import com.lukr99.workout.domain.Exercise
import com.lukr99.workout.domain.ExerciseCategory
import com.lukr99.workout.domain.SetTag
import com.lukr99.workout.domain.StrengthSet
import com.lukr99.workout.domain.WorkoutEntry
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.domain.WorkoutSessionStatus
import com.lukr99.workout.domain.run.Run
import com.lukr99.workout.domain.run.TracePoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

/** True when the browser's own strict `JSON.parse` accepts [text]. */
private fun browserCanParse(text: String): Boolean = js("(function () { try { JSON.parse(text); return true; } catch (e) { return false; } })()")

/** The shared storage logic, compiled for the browser: codec, conflicts, failed saves, quarantine. */
class DocumentStorageTest {

    @Test
    fun theCodecRoundTripsNumbersTimesAndRuns() {
        val document = WorkoutDocument(
            data = ExportBundle(
                exportedAtUtc = "2026-10-26T10:00:00Z",
                exercises = listOf(Exercise(id = "e", name = "Bench", secondaryBodyParts = listOf("Triceps"))),
                sessions = listOf(
                    WorkoutSession(
                        id = "s", startedAtUtc = 1_700_000_000_123L, completedDateUtc = 1_700_003_600_999L,
                        status = WorkoutSessionStatus.Completed, bodyweightKg = 80.15,
                        entries = listOf(
                            WorkoutEntry(
                                id = "en", strengthSets = listOf(StrengthSet(id = "x", weightKg = 0.1 + 0.2, rpe = 7.5, tags = setOf(SetTag.Drop, SetTag.Warmup))),
                            ),
                            WorkoutEntry(id = "c", entryType = ExerciseCategory.Cardio, cardioData = CardioEntryData(distanceKm = 1e-7, calories = 123456.789)),
                        ),
                    ),
                ),
                runs = listOf(Run(id = "r", distanceMeters = 5000.25, trace = listOf(TracePoint(t = 1, lat = 50.0812345, lon = 14.4212345)))),
                settings = SettingsSnapshot(themeMode = "Dark"),
            ),
            seenVersion = 12,
        )
        val text = DocumentCodec.encode(document)
        // 1e-7 above is the Kotlin/Wasm number bug that JsonNumberRepair works around.
        assertTrue(browserCanParse(text))
        assertEquals(document, DocumentCodec.decode(text))
        assertEquals(text, DocumentCodec.encode(DocumentCodec.decode(text)))
    }

    @Test
    fun twoTabsKeepEachOthersWrites() = runTest {
        val store = MemoryStore()
        val first = DocumentDatabase(store)
        val second = DocumentDatabase(store)
        DocumentWorkoutRepository(first).ensureSeeded()
        second.open()
        DocumentWorkoutRepository(first).saveExercise(Exercise(id = "a", name = "A"))
        DocumentWorkoutRepository(second).saveExercise(Exercise(id = "b", name = "B"))

        val stored = DocumentCodec.decode(store.text!!).data.exercises.map { it.id }
        assertTrue("a" in stored && "b" in stored)
        first.reload()
        assertNotNull(DocumentWorkoutRepository(first).getExercise("b"))
    }

    @Test
    fun aFailedSaveIsShownAndRetried() = runTest {
        val store = MemoryStore()
        val database = DocumentDatabase(store)
        val repo = DocumentWorkoutRepository(database)
        store.failSaves = "QuotaExceededError"
        repo.saveExercise(Exercise(id = "a", name = "A"))
        assertEquals(StorageState.SaveFailed("QuotaExceededError"), database.status.value.state)
        assertNotNull(repo.getExercise("a"))
        store.failSaves = null
        database.retrySave()
        assertEquals(StorageState.Saved, database.status.value.state)
        assertEquals("A", DocumentCodec.decode(store.text!!).data.exercises.single().name)
    }

    @Test
    fun unreadableDataIsSetAsideBeforeItIsReplaced() = runTest {
        val store = MemoryStore(text = "not json", revision = 3)
        val database = DocumentDatabase(store)
        DocumentWorkoutRepository(database).ensureSeeded()
        assertEquals("revision-3", database.status.value.setAside?.key)
        assertEquals("not json", store.quarantine.getValue("revision-3").text)
        assertEquals(4L, store.revision)
    }

    @Test
    fun runsFromABackupStayInTheDocument() = runTest {
        val run = Run(id = "r", encodedPolyline = "as-is", trace = listOf(TracePoint(t = 0, lat = 1.0, lon = 2.0)))
        val store = MemoryStore(text = DocumentCodec.encode(WorkoutDocument(data = ExportBundle(exportedAtUtc = "", runs = listOf(run)))), revision = 1)
        val database = DocumentDatabase(store)
        DocumentWorkoutRepository(database).ensureSeeded()
        assertEquals(listOf(run), DocumentCodec.decode(store.text!!).data.runs)
        assertEquals(listOf(run), DocumentRunRepository(database).exportRuns())
    }
}
