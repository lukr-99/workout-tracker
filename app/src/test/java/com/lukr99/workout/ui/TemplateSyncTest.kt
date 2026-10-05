package com.lukr99.workout.ui

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lukr99.workout.data.RoomTransactionRunner
import com.lukr99.workout.data.WorkoutDb
import com.lukr99.workout.data.WorkoutRepository
import com.lukr99.workout.domain.StrengthSet
import com.lukr99.workout.domain.WorkoutEntry
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.domain.WorkoutTemplate
import com.lukr99.workout.domain.WorkoutTemplateExercise
import java.util.concurrent.Executor
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.SQLiteMode

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class TemplateSyncTest {

    private lateinit var repo: WorkoutRepository
    private lateinit var sync: TemplateSync
    private lateinit var push: WorkoutTemplate
    private lateinit var workout: WorkoutSession

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val direct = Executor { it.run() }
        val db = Room.inMemoryDatabaseBuilder(context, WorkoutDb::class.java)
            .allowMainThreadQueries().setQueryExecutor(direct).setTransactionExecutor(direct).build()
        repo = WorkoutRepository(db.workoutDao(), RoomTransactionRunner(db))
        sync = TemplateSync(repo)
        push = repo.saveTemplate(
            WorkoutTemplate(
                name = "Push day",
                exercises = listOf(WorkoutTemplateExercise(exerciseId = "bench", exerciseName = "Bench press", targetSets = 3, repsMax = 8)),
            ),
        )
        workout = WorkoutSession(
            templateId = push.id,
            entries = listOf(
                WorkoutEntry(exerciseId = "bench", exerciseSnapshotName = "Bench press", strengthSets = List(4) { StrengthSet(reps = 8) }),
                WorkoutEntry(exerciseId = "fly", exerciseSnapshotName = "Cable fly", sortOrder = 1, strengthSets = List(3) { StrengthSet(reps = 12) }),
            ),
        )
    }

    @Test
    fun theWorkoutFindsItsTemplate() = runBlocking {
        assertEquals(push.id, sync.templateOf(workout)?.id)
        assertEquals(null, sync.templateOf(workout.copy(templateId = null)))
    }

    @Test
    fun updateRewritesTheTemplateInPlace() = runBlocking {
        sync.apply(workout, push, TemplateChoice.Update, newName = "")

        val updated = repo.getTemplate(push.id)!!
        assertEquals(listOf("bench", "fly"), updated.exercises.map { it.exerciseId })
        assertEquals(listOf(4, 3), updated.exercises.map { it.targetSets })
        assertEquals(1, repo.getTemplates().size)
    }

    @Test
    fun saveAsNewLeavesTheOriginalAlone() = runBlocking {
        sync.apply(workout, push, TemplateChoice.SaveAsNew, newName = "Push with fly")

        val templates = repo.getTemplates().associateBy { it.name }
        assertEquals(2, templates.size)
        val original = templates.getValue("Push day")
        val copy = templates.getValue("Push with fly")
        assertEquals(listOf("bench"), original.exercises.map { it.exerciseId })
        assertEquals(3, original.exercises.single().targetSets)
        assertEquals(listOf("bench", "fly"), copy.exercises.map { it.exerciseId })
        assertNotEquals(original.exercises.single().id, copy.exercises.first().id)
    }

    @Test
    fun keepChangesNothing() = runBlocking {
        sync.apply(workout, push, TemplateChoice.Keep, newName = "")

        assertEquals(push.exercises, repo.getTemplate(push.id)!!.exercises)
    }
}
