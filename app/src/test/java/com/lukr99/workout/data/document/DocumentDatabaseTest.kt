package com.lukr99.workout.data.document

import com.lukr99.workout.data.export.ExportBundle
import com.lukr99.workout.data.export.JsonExporter
import com.lukr99.workout.domain.Exercise
import com.lukr99.workout.domain.ExerciseFilter
import com.lukr99.workout.domain.Location
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.domain.WorkoutSessionStatus
import com.lukr99.workout.domain.run.Route
import com.lukr99.workout.domain.run.RoutePoint
import com.lukr99.workout.domain.run.Run
import com.lukr99.workout.domain.run.TracePoint
import com.lukr99.workout.settings.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** The storage rules the web edition adds on top of the repository contract. */
class DocumentDatabaseTest {

    private class Tab(store: FakeDocumentStore) {
        val database = DocumentDatabase(store.asStore(), now = { 42L })
        val repo = DocumentWorkoutRepository(database, now = { 1_000L })
        val runs = DocumentRunRepository(database)
        val settings = DocumentSettingsStore(database)
        val prefs = DocumentDevicePrefs(database)
        val seen = DocumentSeenVersionStore(database)
    }

    @Test
    fun twoTabsWithAStaleRevisionLoseNothing() = runBlocking {
        val store = FakeDocumentStore()
        val first = Tab(store)
        val second = Tab(store)
        first.repo.ensureSeeded()
        second.database.open()
        first.repo.saveExercise(Exercise(id = "a", name = "From the first tab"))

        // The second tab has not heard of either save yet, and saves on a stale revision.
        second.repo.saveExercise(Exercise(id = "b", name = "From the second tab"))

        val stored = store.document().data.exercises.map { it.id }
        assertTrue("a" in stored && "b" in stored)
        assertEquals(16 + 2, stored.size)
        assertEquals(StorageState.Saved, second.database.status.value.state)
        assertNotNull(second.repo.getExercise("a"))

        // The first tab reloads on the other tab's notice and its flows update.
        assertNull(first.repo.getExercise("b"))
        first.database.reload()
        assertEquals("From the second tab", first.repo.observeExercises(ExerciseFilter()).first().single { it.id == "b" }.name)
        assertEquals(3L, store.revision)
    }

    @Test
    fun aConflictKeepsOneLiveWorkout() = runBlocking {
        val store = FakeDocumentStore()
        val first = Tab(store)
        val second = Tab(store)
        first.database.open()
        second.database.open()
        val started = first.repo.createWorkoutSession(name = "First")
        val other = second.repo.createWorkoutSession(name = "Second")
        assertEquals(started.id, other.id)
        assertEquals(1, store.document().data.sessions.size)
    }

    @Test
    fun aFailedSaveIsShownAndKeepsTheChangeInMemory() = runBlocking {
        val store = FakeDocumentStore()
        val tab = Tab(store)
        tab.repo.ensureSeeded()
        store.failSaves = "QuotaExceededError: the quota has been exceeded"

        tab.repo.saveExercise(Exercise(id = "kept", name = "Not saved yet"))
        assertEquals(StorageState.SaveFailed("QuotaExceededError: the quota has been exceeded"), tab.database.status.value.state)
        assertEquals("Not saved yet", tab.repo.getExercise("kept")!!.name)
        assertNull(store.document().data.exercises.firstOrNull { it.id == "kept" })

        store.failSaves = null
        tab.database.retrySave()
        assertEquals(StorageState.Saved, tab.database.status.value.state)
        assertEquals("Not saved yet", store.document().data.exercises.single { it.id == "kept" }.name)
    }

    @Test
    fun changesKeptAfterAFailedSaveSurviveAConflict() = runBlocking {
        val store = FakeDocumentStore()
        val first = Tab(store)
        val second = Tab(store)
        first.database.open()
        second.database.open()
        store.failSaves = "blocked"
        first.repo.saveExercise(Exercise(id = "a", name = "A"))
        store.failSaves = null
        second.repo.saveExercise(Exercise(id = "b", name = "B"))
        first.repo.saveExercise(Exercise(id = "c", name = "C"))
        assertEquals(setOf("a", "b", "c"), store.document().data.exercises.map { it.id }.toSet())
    }

    @Test
    fun unreadableDataIsSetAsideOnceAndNeverOverwritten() = runBlocking {
        val broken = "{ this is not Ember data"
        val store = FakeDocumentStore(text = broken, revision = 7)

        val tab = Tab(store)
        tab.repo.ensureSeeded()
        val status = tab.database.status.value
        assertEquals(StorageState.Saved, status.state)
        assertEquals("revision-7", status.setAside?.key)
        assertEquals(broken, store.quarantine.getValue("revision-7").text)
        assertEquals(42L, store.quarantine.getValue("revision-7").atUtc)
        assertEquals(16, store.document().data.exercises.size)
        assertEquals(8L, store.revision)

        // Opening the same unreadable record again (a second tab) sets nothing aside twice.
        val again = FakeDocumentStore(text = broken, revision = 7).also { it.quarantine.putAll(store.quarantine) }
        val before = again.quarantine.getValue("revision-7")
        Tab(again).database.open()
        assertEquals(listOf(before), again.quarantine.values.toList())
        assertEquals(listOf(before), Tab(again).database.quarantined())

        // A later unreadable record gets its own entry; the first one stays as it was.
        store.text = "[]"
        Tab(store).database.open()
        assertEquals(listOf("revision-7", "revision-8"), store.quarantine.keys.toList())
        assertEquals(broken, store.quarantine.getValue("revision-7").text)
    }

    @Test
    fun unreadableDataThatCannotBeSetAsideIsNeverReplaced() = runBlocking {
        val store = FakeDocumentStore(text = "garbage", revision = 3).apply { failQuarantine = "blocked" }
        val tab = Tab(store)
        tab.repo.ensureSeeded()
        assertTrue(tab.database.status.value.state is StorageState.MemoryOnly)
        assertEquals("garbage", store.text)
        assertEquals(16, tab.repo.countExercises())
    }

    @Test
    fun withoutStorageTheAppRunsInMemoryAndSaysSo() = runBlocking {
        val store = FakeDocumentStore().apply { unavailable = "The user denied permission to access the database." }
        val tab = Tab(store)
        tab.repo.ensureSeeded()
        tab.repo.saveExercise(Exercise(id = "x", name = "Only in memory"))
        val state = tab.database.status.value.state
        assertTrue(state is StorageState.MemoryOnly)
        assertTrue((state as StorageState.MemoryOnly).reason.contains("denied permission"))
        assertEquals("Only in memory", tab.repo.getExercise("x")!!.name)
        assertEquals(0, store.saves)
    }

    @Test
    fun dataFromANewerBuildIsReadButNeverWritten() = runBlocking {
        val newer = DocumentCodec.encode(WorkoutDocument()).replaceFirst("\"schema\":1", "\"schema\":2")
        val store = FakeDocumentStore(text = newer, revision = 4)
        val tab = Tab(store)
        tab.repo.saveExercise(Exercise(id = "x", name = "X"))
        assertTrue(tab.database.status.value.state is StorageState.MemoryOnly)
        assertEquals(newer, store.text)

        // A tab that meets the newer data on a conflict stops saving too.
        val older = FakeDocumentStore()
        val tabA = Tab(older)
        tabA.database.open()
        older.text = newer
        older.revision = 9
        tabA.repo.saveExercise(Exercise(id = "y", name = "Y"))
        assertTrue(tabA.database.status.value.state is StorageState.MemoryOnly)
        assertEquals(newer, older.text)
    }

    @Test
    fun aTransactionSavesOnceOrNotAtAll() = runBlocking {
        val store = FakeDocumentStore()
        val tab = Tab(store)
        tab.repo.ensureSeeded()
        val saves = store.saves
        tab.repo.inTransaction {
            saveExercise(Exercise(id = "a", name = "A"))
            tab.runs.saveRun(Run(id = "r"))
            // Reads inside the transaction see its own writes.
            assertNotNull(getExercise("a"))
        }
        assertEquals(saves + 1, store.saves)
        assertNotNull(store.document().data.runs.singleOrNull { it.id == "r" })

        try {
            tab.repo.inTransaction {
                saveExercise(Exercise(id = "b", name = "B"))
                error("stop")
            }
            fail("The transaction should have failed")
        } catch (expected: IllegalStateException) {
            assertEquals("stop", expected.message)
        }
        assertNull(tab.repo.getExercise("b"))
        assertEquals(saves + 1, store.saves)
    }

    @Test
    fun aDocumentWithRunsKeepsThemExactlyAsTheyArrived() = runBlocking {
        val run = Run(
            id = "run1", startedAtUtc = 1_700_000_000_000L, durationSeconds = 600, distanceMeters = 1234.5,
            encodedPolyline = "kept-as-is", notes = "Evening\nnice", avgHr = 151, calories = 88.25,
            trace = listOf(TracePoint(t = 0, lat = 50.08, lon = 14.42, elevationM = 201.5), TracePoint(t = 9, lat = 50.081, lon = 14.421, segmentStart = true)),
        )
        val route = Route(id = "route1", name = "Loop", encodedPolyline = "route-line", points = listOf(RoutePoint(0, 50.0, 14.0)))
        val android = ExportBundle(exportedAtUtc = "", runs = listOf(run), routes = listOf(route))
        val store = FakeDocumentStore(text = DocumentCodec.encode(WorkoutDocument(data = android)), revision = 1)

        val tab = Tab(store)
        tab.repo.ensureSeeded()
        tab.repo.saveWorkoutSession(WorkoutSession(status = WorkoutSessionStatus.Completed, startedAtUtc = 5))
        tab.settings.setThemeMode(ThemeMode.Dark)

        val stored = store.document().data
        assertEquals(listOf(run), stored.runs)
        assertEquals(listOf(route), stored.routes)
        assertEquals(listOf(run), tab.runs.exportRuns())
        // A run saved again without its trace keeps the stored trace.
        tab.runs.saveRun(run.copy(trace = emptyList(), notes = "Edited"))
        assertEquals(run.trace, store.document().data.runs.single().trace)
    }

    @Test
    fun settingsPrefsAndTheSeenVersionAreStoredAndDeleteAllLeavesDevicePrefs() = runBlocking {
        val store = FakeDocumentStore()
        val tab = Tab(store)
        tab.settings.setThemeMode(ThemeMode.Light)
        tab.settings.setDefaultRestSeconds(99_999)
        tab.prefs.dismissLocationSetup()
        tab.prefs.markPositionOfferAnswered("gym")
        tab.seen.markSeen(77)

        val reopened = Tab(store)
        assertEquals(ThemeMode.Light, reopened.settings.settings.first().themeMode)
        assertEquals(3_600, reopened.settings.settings.first().defaultRestSeconds)
        assertTrue(reopened.prefs.locationSetupDismissed.first())
        assertEquals(setOf("gym"), reopened.prefs.positionOfferAnswered.first())
        assertEquals(77, reopened.seen.seen.first())

        reopened.repo.deleteAllWorkoutData()
        assertTrue(reopened.prefs.locationSetupDismissed.first())
        assertEquals(77, reopened.seen.seen.first())
        assertEquals(listOf(Location.HOME_ID, Location.OUTDOORS_ID), reopened.repo.getLocations().map { it.id })
    }

    @Test
    fun theStoredTextIsAnExportBundleThatTheExporterReads() = runBlocking {
        val store = FakeDocumentStore()
        val tab = Tab(store)
        tab.repo.ensureSeeded()
        val data = kotlinx.serialization.json.Json.parseToJsonElement(store.text!!)
            .let { it as kotlinx.serialization.json.JsonObject }.getValue("data").toString()
        val bundle = JsonExporter.fromJson(data)
        assertEquals(ExportBundle.CURRENT_VERSION, bundle.exportFormatVersion)
        assertEquals(16, bundle.exercises.size)
    }
}
