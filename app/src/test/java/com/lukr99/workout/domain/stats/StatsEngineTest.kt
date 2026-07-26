package com.lukr99.workout.domain.stats

import com.lukr99.workout.domain.ExerciseCategory
import com.lukr99.workout.domain.SetType
import com.lukr99.workout.domain.StrengthSet
import com.lukr99.workout.domain.WorkoutEntry
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.domain.WorkoutSessionStatus
import com.lukr99.workout.domain.query.WorkoutCriterion
import com.lukr99.workout.domain.query.WorkoutFilter
import com.lukr99.workout.domain.query.WorkoutQuery
import com.lukr99.workout.domain.query.asFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StatsEngineTest {
    private val sessions = listOf(
        session(
            "s1", 1_704_103_200_000L, "Bench", "Chest",
            listOf(
                StrengthSet(reps = 10, weightKg = 20.0, setType = SetType.Warmup, isWarmup = true),
                StrengthSet(reps = 5, weightKg = 100.0, isPr = true, rpe = 9.0),
            ),
        ),
        session(
            "s2", 1_704_708_000_000L, "Squat", "Legs",
            listOf(StrengthSet(reps = 5, weightKg = 120.0, rpe = 8.0)),
        ),
    )

    @Test
    fun combinesFiltersDimensionsAndMetrics() {
        val nonWarmup = WorkoutCriterion.Warmup(false).asFilter()
        val strength = WorkoutCriterion.Categories(setOf(ExerciseCategory.Strength)).asFilter()
        val report = StatsEngine().calculate(
            sessions,
            StatsRequest(
                query = WorkoutQuery(WorkoutFilter.And(listOf(nonWarmup, strength))),
                metrics = listOf(MetricKeys.Workouts, MetricKeys.Sets, MetricKeys.VolumeKg, MetricKeys.BestE1rmKg),
                dimensions = listOf(DimensionKeys.BodyPart),
            ),
        )

        assertEquals(2, report.rows.size)
        val chest = report.rows.single { it.dimensions[DimensionKeys.BodyPart] == "Chest" }
        assertEquals(500.0, chest.metrics.getValue(MetricKeys.VolumeKg).value, 1e-9)
        assertEquals(1.0, chest.metrics.getValue(MetricKeys.Sets).value, 0.0)
        assertTrue(chest.metrics.getValue(MetricKeys.BestE1rmKg).value > 110.0)
    }

    @Test
    fun customMetricProvider_extendsEngineWithoutChangingRequestShape() {
        val custom = object : MetricProvider {
            override val key = "heavy_sets"
            override fun calculate(points: List<com.lukr99.workout.domain.query.WorkoutDataPoint>) =
                MetricValue(points.count { (it.strengthSet?.weightKg ?: 0.0) >= 100 }.toDouble())
        }
        val engine = StatsEngine(BuiltInMetrics.all + custom)
        val report = engine.calculate(sessions, StatsRequest(metrics = listOf(custom.key)))
        assertEquals(2.0, report.rows.single().metrics.getValue(custom.key).value, 0.0)
    }

    @Test
    fun smoothedE1rmUsesOneChronologicalBestEstimatePerSession() {
        val report = StatsEngine().calculate(
            sessions,
            StatsRequest(metrics = listOf(MetricKeys.SmoothedE1rmKg)),
        )

        // Bench 5x100 = 116.67, then squat 5x120 = 140; EWMA alpha 0.35 = 124.83.
        assertEquals(
            124.833333,
            report.rows.single().metrics.getValue(MetricKeys.SmoothedE1rmKg).value,
            1e-5,
        )
    }

    private fun session(
        id: String,
        started: Long,
        exercise: String,
        bodyPart: String,
        sets: List<StrengthSet>,
    ): WorkoutSession {
        val entryId = "e$id"
        return WorkoutSession(
            id = id,
            name = id,
            startedAtUtc = started,
            completedDateUtc = started + 3_600_000,
            durationSeconds = 3_600,
            status = WorkoutSessionStatus.Completed,
            entries = listOf(
                WorkoutEntry(
                    id = entryId,
                    workoutSessionId = id,
                    exerciseId = exercise.lowercase(),
                    exerciseSnapshotName = exercise,
                    exerciseSnapshotPrimaryBodyPart = bodyPart,
                    entryType = ExerciseCategory.Strength,
                    strengthSets = sets.mapIndexed { index, set ->
                        set.copy(id = "$entryId-$index", workoutEntryId = entryId, setNumber = index + 1)
                    },
                ),
            ),
        )
    }
}
