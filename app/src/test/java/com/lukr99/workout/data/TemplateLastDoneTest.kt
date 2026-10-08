package com.lukr99.workout.data

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lukr99.workout.domain.StrengthSet
import com.lukr99.workout.domain.WorkoutEntry
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.domain.WorkoutSessionStatus
import java.util.concurrent.Executor
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.SQLiteMode

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class TemplateLastDoneTest {

    @Test
    fun eachTemplateGetsItsNewestFinishedWorkout() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val direct = Executor { it.run() }
        val db = Room.inMemoryDatabaseBuilder(context, WorkoutDb::class.java)
            .allowMainThreadQueries().setQueryExecutor(direct).setTransactionExecutor(direct).build()
        val repo = WorkoutRepository(db.workoutDao(), RoomTransactionRunner(db))

        fun workout(template: String?, at: Long, status: WorkoutSessionStatus = WorkoutSessionStatus.Completed) = WorkoutSession(
            templateId = template,
            startedAtUtc = at - 1_000,
            completedDateUtc = at.takeIf { status == WorkoutSessionStatus.Completed },
            status = status,
            entries = listOf(WorkoutEntry(exerciseId = "bench", strengthSets = listOf(StrengthSet(reps = 5, performedAtUtc = at)))),
        )
        repo.saveWorkoutSession(workout("push", at = 1_000_000))
        repo.saveWorkoutSession(workout("push", at = 3_000_000))
        repo.saveWorkoutSession(workout("legs", at = 2_000_000))
        repo.saveWorkoutSession(workout(null, at = 5_000_000))
        repo.saveWorkoutSession(workout("legs", at = 9_000_000, status = WorkoutSessionStatus.Discarded))

        assertEquals(mapOf("push" to 3_000_000L, "legs" to 2_000_000L), repo.getTemplatesLastDone())
        db.close()
    }
}
