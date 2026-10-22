package com.lukr99.workout.data

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lukr99.workout.domain.Exercise
import com.lukr99.workout.domain.ExerciseLocationNote
import com.lukr99.workout.domain.Location
import java.util.concurrent.Executor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.SQLiteMode

/** Machine notes per exercise and location through the real Room queries. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class LocationNotesRepositoryTest {

    private lateinit var db: WorkoutDb
    private lateinit var repo: WorkoutRepository
    private lateinit var brno: Location
    private lateinit var other: Location

    @Before
    fun setUp(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val direct = Executor { it.run() }
        db = Room.inMemoryDatabaseBuilder(context, WorkoutDb::class.java)
            .allowMainThreadQueries().setQueryExecutor(direct).setTransactionExecutor(direct).build()
        repo = WorkoutRepository(db.workoutDao(), RoomTransactionRunner(db))
        repo.ensureSeeded()
        // One shared exercise and one tracked per gym: notes must work for both.
        repo.saveExercise(Exercise(id = FLY, name = "Cable Fly", trackPerLocation = true))
        repo.saveExercise(Exercise(id = ROW, name = "Row"))
        brno = repo.saveLocation(Location(name = "Brno gym"))
        other = repo.saveLocation(Location(name = "Other gym"))
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun aNoteBelongsToOneExerciseAtOnePlace() = runBlocking {
        repo.saveLocationNote(FLY, brno.id, "Double pulley, seat 4")
        repo.saveLocationNote(FLY, other.id, "Single pulley")
        repo.saveLocationNote(ROW, brno.id, "Plate loaded")

        assertEquals("Double pulley, seat 4", repo.observeLocationNote(FLY, brno.id).first())
        assertEquals("Single pulley", repo.observeLocationNote(FLY, other.id).first())
        assertEquals("", repo.observeLocationNote(FLY, Location.HOME_ID).first())
        assertEquals(
            mapOf(FLY to "Double pulley, seat 4", ROW to "Plate loaded"),
            repo.getLocationNotes(listOf(FLY, ROW, ROW), brno.id),
        )
        assertEquals(
            mapOf(brno.id to "Double pulley, seat 4", other.id to "Single pulley"),
            repo.observeLocationNotesFor(FLY).first(),
        )
    }

    @Test
    fun sharedExercisesGetNotesToo() = runBlocking {
        assertTrue(!repo.getExercise(ROW)!!.trackPerLocation)
        repo.saveLocationNote(ROW, other.id, "Handles on the lower hooks")
        assertEquals("Handles on the lower hooks", repo.getLocationNotes(listOf(ROW), other.id).getValue(ROW))
    }

    @Test
    fun savingAgainReplacesAndABlankNoteRemovesTheRow() = runBlocking {
        repo.saveLocationNote(FLY, brno.id, "  Seat 3  ")
        repo.saveLocationNote(FLY, brno.id, "Seat 4")
        assertEquals(listOf(ExerciseLocationNote(FLY, brno.id, "Seat 4")), repo.getAllLocationNotes())

        repo.saveLocationNote(FLY, brno.id, "   ")
        assertTrue(repo.getAllLocationNotes().isEmpty())
        assertTrue(repo.getLocationNotes(emptyList(), brno.id).isEmpty())
    }

    @Test
    fun theBundleCarriesNotesAndRestoreKeepsWhatIsAlreadyThere() = runBlocking {
        repo.saveLocationNote(FLY, brno.id, "Double pulley")
        repo.saveLocationNote(ROW, brno.id, "Plate loaded")
        val bundle = repo.createExportBundle()
        assertEquals(2, bundle.locationNotes.size)

        repo.deleteAllWorkoutData()
        assertTrue(repo.getAllLocationNotes().isEmpty())

        repo.importBundle(bundle)
        assertEquals(bundle.locationNotes.toSet(), repo.getAllLocationNotes().toSet())

        // A merge restore never overwrites a note the owner already has on this device.
        repo.saveLocationNote(FLY, brno.id, "Newer note")
        repo.restoreLocationNotes(bundle.locationNotes + ExerciseLocationNote(FLY, other.id, "Single pulley"))
        assertEquals("Newer note", repo.getLocationNotes(listOf(FLY), brno.id).getValue(FLY))
        assertEquals("Single pulley", repo.getLocationNotes(listOf(FLY), other.id).getValue(FLY))
    }

    private companion object {
        const val FLY = "cable-fly"
        const val ROW = "row"
    }
}
