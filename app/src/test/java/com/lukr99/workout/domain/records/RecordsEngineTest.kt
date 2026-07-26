package com.lukr99.workout.domain.records

import com.lukr99.workout.domain.SetType
import com.lukr99.workout.domain.StrengthSet
import com.lukr99.workout.domain.WorkoutEntry
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.domain.WorkoutSessionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordsEngineTest {
    @Test
    fun calculatesAllRecordsAndExcludesWarmups() {
        val records = RecordsEngine.forExercise(
            listOf(
                session("older", 1_000, listOf(set(150.0, 1, warmup = true), set(100.0, 5), set(90.0, 8))),
                session("newer", 2_000, listOf(set(105.0, 5), set(80.0, 12), set(110.0, 3))),
            ),
            ExerciseId,
        )

        assertEquals(110.0, records.heaviestSet!!.value, 0.001)
        assertEquals("newer", records.heaviestSet.source.sessionId)
        assertEquals(122.5, records.bestEstimated1Rm!!.value, 0.001)
        assertEquals(960.0, records.bestSetVolume!!.value, 0.001)
        assertEquals(1_815.0, records.bestSessionVolume!!.volumeKg, 0.001)
        assertEquals(105.0, records.repMaxes.single { it.reps == 5 }.weightKg, 0.001)
    }

    @Test
    fun tiesUseOldestSourceAndLiveEvaluationRequiresImprovement() {
        val history = listOf(
            session("z-later-id", 2_000, listOf(set(100.0, 5))),
            session("a-earlier", 1_000, listOf(set(100.0, 5))),
        )

        assertEquals(
            "a-earlier",
            RecordsEngine.forExercise(history, ExerciseId).heaviestSet!!.source.sessionId,
        )
        assertFalse(RecordsEngine.evaluateSet(history, ExerciseId, set(100.0, 5)).isPersonalRecord)
        val improved = RecordsEngine.evaluateSet(history, ExerciseId, set(102.5, 5))
        assertTrue(improved.isPersonalRecord)
        assertTrue(RecordKind.HeaviestSet in improved.kinds)
        assertEquals(setOf(5), improved.repMaxReps)
    }

    private fun session(id: String, date: Long, sets: List<StrengthSet>) = WorkoutSession(
        id = id,
        name = id,
        startedAtUtc = date,
        completedDateUtc = date,
        status = WorkoutSessionStatus.Completed,
        entries = listOf(
            WorkoutEntry(
                id = "entry-$id",
                exerciseId = ExerciseId,
                exerciseSnapshotName = "Bench press",
                exerciseSnapshotPrimaryBodyPart = "Chest",
                strengthSets = sets.mapIndexed { index, set ->
                    set.copy(id = "$id-set-$index", setNumber = index + 1)
                },
            ),
        ),
    )

    private fun set(weight: Double, reps: Int, warmup: Boolean = false) = StrengthSet(
        weightKg = weight,
        reps = reps,
        isWarmup = warmup,
        setType = if (warmup) SetType.Warmup else SetType.Normal,
    )

    private companion object {
        const val ExerciseId = "bench"
    }
}
