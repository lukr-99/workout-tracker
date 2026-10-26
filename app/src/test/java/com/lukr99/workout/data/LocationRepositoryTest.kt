package com.lukr99.workout.data

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lukr99.workout.data.export.ExportBundle
import com.lukr99.workout.domain.Exercise
import com.lukr99.workout.domain.Location
import com.lukr99.workout.domain.StrengthSet
import com.lukr99.workout.domain.WorkoutEntry
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.domain.WorkoutSessionStatus
import java.util.concurrent.Executor
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.SQLiteMode

/** Locations through the real Room queries: defaults, moves, and last time in both modes. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class LocationRepositoryTest {

    private lateinit var db: WorkoutDb
    private lateinit var repo: WorkoutRepository

    @Before
    fun setUp(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val direct = Executor { it.run() }
        db = Room.inMemoryDatabaseBuilder(context, WorkoutDb::class.java)
            .allowMainThreadQueries().setQueryExecutor(direct).setTransactionExecutor(direct).build()
        repo = RoomWorkoutRepository(db.workoutDao(), RoomTransactionRunner(db))
        repo.ensureSeeded()
        repo.saveExercise(Exercise(id = FLY, name = "Cable Fly"))
        Unit
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun homeAndOutdoorsAreThereFromTheStart() = runBlocking {
        assertEquals(listOf(Location.HOME_ID, Location.OUTDOORS_ID), repo.getLocations().map { it.id })
        // Seeding again keeps a renamed built-in as it is.
        repo.saveLocation(repo.getLocation(Location.HOME_ID)!!.copy(name = "Garage"))
        repo.ensureSeeded()
        assertEquals("Garage", repo.getLocation(Location.HOME_ID)!!.name)
    }

    @Test
    fun aNewWorkoutStartsAtTheLastUsedPlace() = runBlocking {
        assertNull(repo.createWorkoutSession().locationId)
        repo.deleteWorkoutSession(repo.getActiveSession()!!.id)

        val brno = repo.saveLocation(Location(name = "  Brno gym "))
        assertEquals("Brno gym", brno.name)
        repo.saveWorkoutSession(workout("w1", at = 1_000, location = brno.id, kg = 15.0))
        repo.saveWorkoutSession(workout("w2", at = 2_000, location = null, kg = 15.0))
        assertEquals(brno.id, repo.getLastUsedLocationId())
        assertEquals(brno.id, repo.createWorkoutSession().locationId)
    }

    @Test
    fun anArchivedPlaceIsNotTheDefault() = runBlocking {
        val brno = repo.saveLocation(Location(name = "Brno gym"))
        repo.saveWorkoutSession(workout("w1", at = 1_000, location = brno.id, kg = 15.0))
        repo.setLocationArchived(brno.id, true)
        assertNull(repo.getLastUsedLocationId())
        assertTrue(repo.getLocation(brno.id)!!.isArchived)
        // The workout still points at it.
        assertEquals(brno.id, repo.getSession("w1")!!.locationId)
    }

    @Test
    fun movingAndAssigningWorkouts() = runBlocking {
        val brno = repo.saveLocation(Location(name = "Brno gym"))
        val other = repo.saveLocation(Location(name = "Other gym"))
        repo.saveWorkoutSession(workout("w1", at = 1_000, location = null, kg = 15.0))
        repo.saveWorkoutSession(workout("w2", at = 2_000, location = null, kg = 59.0))
        repo.saveWorkoutSession(workout("w3", at = 3_000, location = null, kg = 15.0))
        assertEquals(3, repo.countWorkoutsWithoutLocation())

        assertEquals(1, repo.moveWorkoutsToLocation(listOf("w2", "w2", ""), other.id))
        assertEquals(2, repo.assignUnknownWorkoutsTo(brno.id))
        assertEquals(0, repo.countWorkoutsWithoutLocation())
        assertEquals(listOf(brno.id, other.id, brno.id), listOf("w1", "w2", "w3").map { repo.getSession(it)!!.locationId })

        assertEquals(1, repo.moveWorkoutsToLocation(listOf("w3"), null))
        assertNull(repo.getSession("w3")!!.locationId)
    }

    @Test
    fun lastTimeInBothModes() = runBlocking {
        val brno = repo.saveLocation(Location(name = "Brno gym"))
        val other = repo.saveLocation(Location(name = "Other gym"))
        repo.saveWorkoutSession(workout("w1", at = 1_000, location = brno.id, kg = 15.0, note = "double pulley"))
        repo.saveWorkoutSession(workout("w2", at = 2_000, location = other.id, kg = 59.0, note = "single pulley"))

        // Shared: the newest workout anywhere.
        assertEquals(59.0, repo.getLastSets(listOf(FLY), brno.id).getValue(FLY).single().weightKg, 0.0)
        assertEquals("single pulley", repo.getPreviousEntryNotes(listOf(FLY), "live", brno.id).getValue(FLY).text)

        // Per location: only this gym. The flag is all that changes.
        repo.setTrackPerLocation(FLY, true)
        assertEquals(15.0, repo.getLastSets(listOf(FLY), brno.id).getValue(FLY).single().weightKg, 0.0)
        assertEquals("double pulley", repo.getPreviousEntryNotes(listOf(FLY), "live", brno.id).getValue(FLY).text)
        assertTrue(repo.getLastSets(listOf(FLY), null).isEmpty())
        assertTrue(repo.getPreviousEntryNotes(listOf(FLY), "live", null).isEmpty())
        assertEquals(1, repo.getExerciseProgress(FLY, repo.locationScope(FLY, other.id)).size)

        // Back to shared joins them again.
        repo.setTrackPerLocation(FLY, false)
        assertEquals(59.0, repo.getLastSets(listOf(FLY), brno.id).getValue(FLY).single().weightKg, 0.0)
        assertEquals(2, repo.getExerciseProgress(FLY, repo.locationScope(FLY, other.id)).size)
    }

    @Test
    fun muteSplitPromptSticks() = runBlocking {
        repo.muteSplitPrompt(FLY)
        assertTrue(repo.getExercise(FLY)!!.splitPromptMuted)
    }

    @Test
    fun deleteAllKeepsTheBuiltInsAndTheBundleCarriesLocations() = runBlocking {
        val brno = repo.saveLocation(Location(name = "Brno gym"))
        repo.saveWorkoutSession(workout("w1", at = 1_000, location = brno.id, kg = 15.0))
        val bundle: ExportBundle = repo.createExportBundle()
        assertEquals(3, bundle.locations.size)

        repo.deleteAllWorkoutData()
        assertEquals(listOf(Location.HOME_ID, Location.OUTDOORS_ID), repo.getLocations().map { it.id })

        repo.importBundle(bundle)
        assertEquals("Brno gym", repo.getLocation(brno.id)!!.name)
        assertEquals(brno.id, repo.getSession("w1")!!.locationId)
    }

    private fun workout(id: String, at: Long, location: String?, kg: Double, note: String = "") = WorkoutSession(
        id = id,
        name = id,
        startedAtUtc = at - 500,
        completedDateUtc = at,
        status = WorkoutSessionStatus.Completed,
        locationId = location,
        entries = listOf(
            WorkoutEntry(
                exerciseId = FLY,
                exerciseSnapshotName = "Cable Fly",
                notes = note,
                strengthSets = listOf(StrengthSet(reps = 10, weightKg = kg, performedAtUtc = at)),
            ),
        ),
    )

    private companion object {
        const val FLY = "cable-fly"
    }
}
