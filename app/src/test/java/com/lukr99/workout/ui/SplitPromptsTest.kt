package com.lukr99.workout.ui

import com.lukr99.workout.data.RoomWorkoutRepository
import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lukr99.workout.data.RoomTransactionRunner
import com.lukr99.workout.data.WorkoutDb
import com.lukr99.workout.data.WorkoutRepository
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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.SQLiteMode

/** The live workout's "Track per gym?" question against a real database. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class SplitPromptsTest {

    private lateinit var db: WorkoutDb
    private lateinit var repo: WorkoutRepository
    private lateinit var prompts: SplitPrompts
    private lateinit var brno: Location
    private lateinit var other: Location

    @Before
    fun setUp(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val direct = Executor { it.run() }
        db = Room.inMemoryDatabaseBuilder(context, WorkoutDb::class.java)
            .allowMainThreadQueries().setQueryExecutor(direct).setTransactionExecutor(direct).build()
        repo = RoomWorkoutRepository(db.workoutDao(), RoomTransactionRunner(db))
        prompts = SplitPrompts(repo)
        repo.saveExercise(Exercise(id = FLY, name = "Cable Fly"))
        brno = repo.saveLocation(Location(name = "Brno gym"))
        other = repo.saveLocation(Location(name = "Other gym"))
        repo.saveWorkoutSession(workout("w1", brno.id, 15.0, at = 1_000))
        repo.saveWorkoutSession(workout("w2", brno.id, 15.0, at = 2_000))
        Unit
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun asksOnceWhenTheNumbersAreFarApart() = runBlocking {
        val live = workout("live", other.id, 59.0, at = 3_000, status = WorkoutSessionStatus.Active)
        val prompt = prompts.check(live, "live-e")!!
        assertEquals("Cable Fly", prompt.exerciseName)
        assertEquals("Brno gym", prompt.otherLocationName)
        assertEquals(59.0, prompt.hereKg, 0.0)
        assertEquals(15.0, prompt.otherKg, 0.0)

        assertNull("once per workout", prompts.check(live, "live-e"))
        prompts.reset()
        assertEquals(FLY, prompts.check(live, "live-e")?.exerciseId)
    }

    @Test
    fun staysQuietWithoutALocationOrForAnExerciseAlreadyDecided() = runBlocking {
        val live = workout("live", other.id, 59.0, at = 3_000, status = WorkoutSessionStatus.Active)
        assertNull(prompts.check(live.copy(locationId = null), "live-e"))

        repo.muteSplitPrompt(FLY)
        assertNull(prompts.check(live, "live-e"))

        repo.saveExercise(Exercise(id = FLY, name = "Cable Fly", trackPerLocation = true))
        assertNull(prompts.check(live, "live-e"))
    }

    private fun workout(
        id: String,
        location: String?,
        kg: Double,
        at: Long,
        status: WorkoutSessionStatus = WorkoutSessionStatus.Completed,
    ) = WorkoutSession(
        id = id,
        name = id,
        startedAtUtc = at - 500,
        completedDateUtc = at.takeIf { status == WorkoutSessionStatus.Completed },
        status = status,
        locationId = location,
        entries = listOf(
            WorkoutEntry(
                id = "$id-e",
                workoutSessionId = id,
                exerciseId = FLY,
                exerciseSnapshotName = "Cable Fly",
                strengthSets = listOf(StrengthSet(reps = 10, weightKg = kg, performedAtUtc = at)),
            ),
        ),
    )

    private companion object {
        const val FLY = "cable-fly"
    }
}
