package com.lukr99.workout.domain.recovery

import com.lukr99.workout.domain.Exercise
import com.lukr99.workout.domain.SetType
import com.lukr99.workout.domain.StrengthSet
import com.lukr99.workout.domain.WorkoutEntry
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.domain.WorkoutSessionStatus
import com.lukr99.workout.domain.stats.BuiltInDimensions
import com.lukr99.workout.domain.stats.BuiltInMetrics
import com.lukr99.workout.domain.stats.StatsEngine
import com.lukr99.workout.domain.stats.StatsRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecoveryEngineTest {
    @Test
    fun readinessRecoversWithTimeAndSecondaryMusclesReceiveWeightedLoad() {
        val exercise = Exercise(
            id = ExerciseId,
            name = "Bench press",
            primaryBodyPart = "Chest",
            secondaryBodyParts = listOf("Triceps"),
        )
        val session = session(0)
        val recent = RecoveryEngine.calculate(listOf(session), listOf(exercise), nowUtcMillis = 0)
        val later = RecoveryEngine.calculate(
            listOf(session),
            listOf(exercise),
            nowUtcMillis = 36 * Hour,
        )

        assertTrue(recent.forBodyPart("Chest")!!.readiness < later.forBodyPart("Chest")!!.readiness)
        assertTrue(recent.forBodyPart("Triceps")!!.readiness > recent.forBodyPart("Chest")!!.readiness)
        assertEquals(1.0, recent.forBodyPart("Triceps")!!.weeklySetCount, 0.001)
    }

    @Test
    fun bodyPartStatsExpandPrimaryAndSecondaryAndIgnoreWarmups() {
        val exercise = Exercise(
            id = ExerciseId,
            primaryBodyPart = "Chest",
            secondaryBodyParts = listOf("Triceps"),
        )
        val engine = StatsEngine(
            BuiltInMetrics.all + BodyPartStatsProviders.metrics,
            BuiltInDimensions.all + BodyPartStatsProviders.bodyPartDimension(listOf(exercise)),
        )
        val report = engine.calculate(
            listOf(session(0)),
            StatsRequest(
                metrics = listOf(BodyPartStatsKeys.WorkingSetCount, BodyPartStatsKeys.WorkingVolumeKg),
                dimensions = listOf(BodyPartStatsKeys.AllBodyParts),
            ),
        )

        assertEquals(setOf("Chest", "Triceps"), report.rows.map { it.dimensions.values.single() }.toSet())
        report.rows.forEach {
            assertEquals(2.0, it.metrics.getValue(BodyPartStatsKeys.WorkingSetCount).value, 0.001)
            assertEquals(1_000.0, it.metrics.getValue(BodyPartStatsKeys.WorkingVolumeKg).value, 0.001)
        }
    }

    private fun session(date: Long) = WorkoutSession(
        id = "session",
        startedAtUtc = date,
        completedDateUtc = date,
        status = WorkoutSessionStatus.Completed,
        entries = listOf(
            WorkoutEntry(
                exerciseId = ExerciseId,
                exerciseSnapshotPrimaryBodyPart = "Chest",
                strengthSets = listOf(
                    StrengthSet(weightKg = 20.0, reps = 10, isWarmup = true, setType = SetType.Warmup),
                    StrengthSet(weightKg = 100.0, reps = 5),
                    StrengthSet(weightKg = 100.0, reps = 5),
                ),
            ),
        ),
    )

    private companion object {
        const val ExerciseId = "bench"
        const val Hour = 3_600_000L
    }
}
