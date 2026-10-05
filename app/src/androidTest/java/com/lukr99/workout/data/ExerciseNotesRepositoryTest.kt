package com.lukr99.workout.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lukr99.workout.data.services.WorkoutDataService
import com.lukr99.workout.domain.Exercise
import com.lukr99.workout.domain.StrengthSet
import com.lukr99.workout.domain.WorkoutEntry
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.domain.WorkoutSessionStatus
import com.lukr99.workout.domain.creation.TemplateDraft
import com.lukr99.workout.domain.creation.TemplateExerciseDraft
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Richer notes (v8): guide fields persist, "Last time" notes, and template notes survive a save. */
@RunWith(AndroidJUnit4::class)
class ExerciseNotesRepositoryTest {

    private lateinit var db: WorkoutDb
    private lateinit var repo: WorkoutRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, WorkoutDb::class.java)
            .allowMainThreadQueries()
            .build()
        repo = WorkoutRepository(db.workoutDao(), RoomTransactionRunner(db))
    }

    @After
    fun teardown() = db.close()

    @Test
    fun guideFields_roundTripThroughRoom() = runTest {
        val saved = repo.saveExercise(
            Exercise(
                name = "Bench",
                notes = "Seat on 4",
                instructions = "Feet flat\nBar to lower chest",
                videoUrl = "https://youtu.be/abc",
            ),
        )

        val loaded = repo.getExercise(saved.id)!!
        assertEquals("Seat on 4", loaded.notes)
        assertEquals(listOf("Feet flat", "Bar to lower chest"), loaded.instructionSteps)
        assertEquals("https://youtu.be/abc", loaded.videoUrl)
    }

    @Test
    fun previousNotes_returnNewestCompletedNoteAndSkipTheLiveSession() = runTest {
        repo.saveWorkoutSession(session("old", 1_000L, "Felt heavy"))
        repo.saveWorkoutSession(session("newer", 5_000L, "Grip wider next time"))
        repo.saveWorkoutSession(session("blank", 9_000L, "   "))
        repo.saveWorkoutSession(
            session("discarded", 12_000L, "Should not show", WorkoutSessionStatus.Discarded),
        )
        val live = repo.saveWorkoutSession(
            session("live", 20_000L, "Typing this now", WorkoutSessionStatus.Active),
        )

        val notes = repo.getPreviousEntryNotes(listOf("bench", "squat", ""), live.id)

        assertEquals(setOf("bench"), notes.keys)
        assertEquals("Grip wider next time", notes.getValue("bench").text)
        assertEquals(5_000L, notes.getValue("bench").atUtc)
    }

    @Test
    fun templateNotes_surviveSavingThroughTheCreationService() = runTest {
        val data = WorkoutDataService(repo)
        val bench = repo.saveExercise(Exercise(name = "Bench"))
        val created = data.createTemplate(
            TemplateDraft(
                name = "Push",
                notes = "Created from workout on 2026-10-01",
                exercises = listOf(
                    TemplateExerciseDraft(exerciseId = bench.id, notes = "Pause on chest"),
                    TemplateExerciseDraft(exerciseId = bench.id, notes = "Back-off set"),
                ),
            ),
        ).requireValid()

        val loaded = repo.getTemplate(created.id)!!
        assertEquals("Created from workout on 2026-10-01", loaded.notes)
        assertEquals(listOf("Pause on chest", "Back-off set"), loaded.exercises.map { it.notes })
        assertTrue(loaded.exercises.all { it.exerciseId == bench.id })
    }

    @Test
    fun syncedDescription_doesNotDuplicateAnOlderNote() = runTest {
        repo.mergeExternalExercisesDetailed(
            listOf(Exercise(name = "Row", externalSourceId = "wger:1", notes = "Pull to hips")),
        )
        repo.mergeExternalExercisesDetailed(
            listOf(Exercise(name = "Row", externalSourceId = "wger:1", instructions = "Pull to hips")),
        )

        val row = repo.getExercises().single { it.name == "Row" }
        assertEquals("Pull to hips", row.notes)
        assertEquals("", row.instructions)
        assertNull(row.videoUrl)
    }

    private fun session(
        id: String,
        completedAt: Long,
        note: String,
        status: WorkoutSessionStatus = WorkoutSessionStatus.Completed,
    ) = WorkoutSession(
        id = id,
        name = id,
        startedAtUtc = completedAt - 100,
        endedAtUtc = completedAt.takeIf { status != WorkoutSessionStatus.Active },
        completedDateUtc = completedAt.takeIf { status == WorkoutSessionStatus.Completed },
        status = status,
        entries = listOf(
            WorkoutEntry(
                id = "$id-e",
                workoutSessionId = id,
                exerciseId = "bench",
                exerciseSnapshotName = "Bench",
                notes = note,
                strengthSets = listOf(StrengthSet(id = "$id-s", reps = 5, weightKg = 100.0)),
            ),
        ),
    )
}
