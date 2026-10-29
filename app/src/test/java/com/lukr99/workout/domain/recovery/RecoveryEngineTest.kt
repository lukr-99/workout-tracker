package com.lukr99.workout.domain.recovery

import com.lukr99.workout.domain.Exercise
import com.lukr99.workout.domain.ExerciseCategory
import com.lukr99.workout.domain.SetTag
import com.lukr99.workout.domain.SetType
import com.lukr99.workout.domain.StrengthSet
import com.lukr99.workout.domain.WorkoutEntry
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.domain.WorkoutSessionStatus
import com.lukr99.workout.domain.stats.BuiltInDimensions
import com.lukr99.workout.domain.stats.BuiltInMetrics
import com.lukr99.workout.domain.stats.StatsEngine
import com.lukr99.workout.domain.stats.StatsRequest
import kotlin.math.exp
import kotlin.math.pow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecoveryEngineTest {
    private val bench = Exercise(id = "bench", name = "Bench press", primaryBodyPart = "Chest", secondaryBodyParts = listOf("Triceps", "Shoulders"))
    private val squat = Exercise(id = "squat", name = "Back squat", primaryBodyPart = "Legs", secondaryBodyParts = listOf("Glutes"))
    private val curl = Exercise(id = "curl", name = "Curl", primaryBodyPart = "Biceps brachii")

    @Test
    fun readinessRecoversWithTimeAndSecondaryMusclesReceiveHalfLoad() {
        val session = session(0, entry(bench, working(2)))
        val recent = RecoveryEngine.calculate(listOf(session), listOf(bench), nowUtcMillis = 0)
        val later = RecoveryEngine.calculate(listOf(session), listOf(bench), nowUtcMillis = 36 * Hour)

        assertTrue(recent.forBodyPart("Chest")!!.readiness < later.forBodyPart("Chest")!!.readiness)
        assertTrue(recent.forBodyPart("Triceps")!!.readiness > recent.forBodyPart("Chest")!!.readiness)
        assertEquals(2.0, recent.forBodyPart("Chest")!!.weeklySetCount, 0.001)
        assertEquals(1.0, recent.forBodyPart("Triceps")!!.weeklySetCount, 0.001)
    }

    @Test
    fun sixChestSetsAtDefaultEffortGiveTheDocumentedNumbers() {
        val snapshot = RecoveryEngine.calculate(listOf(session(0, entry(bench, working(6)))), listOf(bench), nowUtcMillis = 0)
        val chest = snapshot.forGroup(MuscleGroup.Chest)!!

        assertEquals(6.0, chest.fatigueLoad, 0.001)
        assertEquals(100 * exp(-6.0 / 5.0), chest.readiness, 0.01) // about 30 %
        assertEquals(RecoveryState.Fatigued, chest.state)
        // 20 h half-life: 6 sets fall to the ready level (1.116 sets) after about 48.5 h.
        assertEquals(48.5, chest.readyAtUtc!! / Hour.toDouble(), 0.2)

        val later = RecoveryEngine.calculate(listOf(session(0, entry(bench, working(6)))), listOf(bench), nowUtcMillis = 24 * Hour)
        assertEquals(6 * 0.5.pow(24.0 / 20.0), later.forGroup(MuscleGroup.Chest)!!.fatigueLoad, 0.001)
        assertEquals(RecoveryState.Recovering, later.forGroup(MuscleGroup.Chest)!!.state)
    }

    @Test
    fun legMusclesRecoverSlowerThanCalvesAfterTheSameWork() {
        val calfRaise = Exercise(id = "calf", primaryBodyPart = "Calves")
        val sessions = listOf(session(0, entry(squat, working(4)), entry(calfRaise, working(4))))
        val snapshot = RecoveryEngine.calculate(sessions, listOf(squat, calfRaise), nowUtcMillis = 36 * Hour)

        assertTrue(snapshot.forGroup(MuscleGroup.Quads)!!.readiness < snapshot.forGroup(MuscleGroup.Calves)!!.readiness)
        assertEquals(RecoveryState.Ready, snapshot.forGroup(MuscleGroup.Calves)!!.state)
    }

    @Test
    fun broadBodyPartsSpreadOverTheirMusclesAndAliasesResolve() {
        val snapshot = RecoveryEngine.calculate(
            listOf(session(0, entry(squat, working(3)), entry(curl, working(2)))),
            listOf(squat, curl),
            nowUtcMillis = Hour,
        )

        assertEquals(3.0, snapshot.forGroup(MuscleGroup.Quads)!!.weeklySetCount, 0.001)
        assertEquals(3.0, snapshot.forGroup(MuscleGroup.Hamstrings)!!.weeklySetCount, 0.001)
        // Glutes are both inside Legs (1) and secondary (0.5): the larger share wins.
        assertEquals(3.0, snapshot.forGroup(MuscleGroup.Glutes)!!.weeklySetCount, 0.001)
        assertEquals(2.0, snapshot.forBodyPart("Biceps")!!.weeklySetCount, 0.001)
        assertEquals(MuscleGroup.Biceps, snapshot.forBodyPart("brachialis")!!.group)
        assertNull(snapshot.forBodyPart("Legs"))
    }

    @Test
    fun effortFollowsRepsInReserveRpeAndFailureTags() {
        assertEquals(1.0, RecoveryEngine.effort(StrengthSet(reps = 8)), 0.001)
        assertEquals(1.3, RecoveryEngine.effort(StrengthSet(reps = 8, rir = 0.0)), 0.001)
        assertEquals(1.15, RecoveryEngine.effort(StrengthSet(reps = 8, rpe = 9.0)), 0.001)
        assertEquals(1.3, RecoveryEngine.effort(StrengthSet(reps = 8, tags = setOf(SetTag.ToFailure))), 0.001)
        assertEquals(1.3, RecoveryEngine.effort(StrengthSet(reps = 8, setType = SetType.Failure)), 0.001)
        assertEquals(0.85, RecoveryEngine.effort(StrengthSet(reps = 8, rir = 3.0)), 0.001)
        assertEquals(0.7, RecoveryEngine.effort(StrengthSet(reps = 8, rir = 4.0)), 0.001)
        assertEquals(0.5, RecoveryEngine.effort(StrengthSet(reps = 8, rir = 6.0)), 0.001)
        assertEquals(1.2, RecoveryEngine.effort(StrengthSet(reps = 8, tags = setOf(SetTag.SlowEccentric))), 0.001)
    }

    @Test
    fun easySetsAddFatigueButDoNotCountAsHardSets() {
        val sets = listOf(StrengthSet(weightKg = 60.0, reps = 10, rir = 6.0), StrengthSet(weightKg = 80.0, reps = 8, rir = 1.0))
        val chest = RecoveryEngine.calculate(listOf(session(0, entry(bench, sets))), listOf(bench), nowUtcMillis = 0)
            .forGroup(MuscleGroup.Chest)!!

        assertEquals(1.0, chest.weeklySetCount, 0.001)
        assertEquals(0.5 + 1.15, chest.fatigueLoad, 0.001)
        assertEquals(60.0 * 10 + 80.0 * 8, chest.weeklyVolumeKg, 0.001)
    }

    @Test
    fun warmupsEmptySetsAndOldOrPlannedWorkoutsAreIgnored() {
        val sets = listOf(
            StrengthSet(weightKg = 20.0, reps = 10, isWarmup = true, setType = SetType.Warmup),
            StrengthSet(weightKg = 20.0, reps = 10, tags = setOf(SetTag.Warmup)),
            StrengthSet(weightKg = 0.0, reps = 0),
            StrengthSet(weightKg = 100.0, reps = 5),
        )
        val old = session(-15 * 24 * Hour, entry(bench, working(5))).copy(id = "old")
        val planned = session(0, entry(bench, working(5))).copy(id = "planned", status = WorkoutSessionStatus.Active)
        val snapshot = RecoveryEngine.calculate(listOf(session(0, entry(bench, sets)), old, planned), listOf(bench), nowUtcMillis = 0)

        assertEquals(1.0, snapshot.forGroup(MuscleGroup.Chest)!!.weeklySetCount, 0.001)
    }

    @Test
    fun weeklyCountUsesTheLastSevenDaysAndRatesItAgainstTheTarget() {
        val sessions = listOf(
            session(0, entry(bench, working(6))).copy(id = "a"),
            session(3 * 24 * Hour, entry(bench, working(6))).copy(id = "b"),
            session(8 * 24 * Hour, entry(bench, working(6))).copy(id = "c"),
        )
        val snapshot = RecoveryEngine.calculate(sessions, listOf(bench), nowUtcMillis = 8 * 24 * Hour)

        // Session a is 8 days old: it still adds fatigue but leaves the weekly count.
        assertEquals(12.0, snapshot.forGroup(MuscleGroup.Chest)!!.weeklySetCount, 0.001)
        assertEquals(WeeklyVolumeStatus.InRange, snapshot.forGroup(MuscleGroup.Chest)!!.weeklyStatus)
        assertEquals(WeeklyVolumeStatus.Below, snapshot.forGroup(MuscleGroup.Triceps)!!.weeklyStatus)
        assertEquals(WeeklyVolumeStatus.None, RecoveryEngine.weeklyStatus(0.0))
        assertEquals(WeeklyVolumeStatus.Above, RecoveryEngine.weeklyStatus(20.5))
        assertEquals(WeeklyVolumeStatus.InRange, RecoveryEngine.weeklyStatus(20.0))
    }

    @Test
    fun allGroupsListsUntrainedMusclesAsFreshAndCustomPartsHaveNoTarget() {
        val neck = Exercise(id = "neck", primaryBodyPart = "Neck")
        val run = Exercise(id = "run", primaryBodyPart = "Cardio", category = ExerciseCategory.Cardio)
        val snapshot = RecoveryEngine.calculate(listOf(session(0, entry(neck, working(3)))), listOf(neck, run), nowUtcMillis = 0)

        assertEquals(MuscleGroup.entries.size, snapshot.allGroups().size)
        val abs = snapshot.allGroups().single { it.group == MuscleGroup.Abs }
        assertEquals(100.0, abs.readiness, 0.001)
        assertEquals(WeeklyVolumeStatus.None, abs.weeklyStatus)
        assertNull(snapshot.forBodyPart("Neck")!!.weeklyStatus)
    }

    @Test
    fun bodyPartStatsExpandPrimaryAndSecondaryAndIgnoreWarmups() {
        val exercise = Exercise(id = "bench", primaryBodyPart = "Chest", secondaryBodyParts = listOf("Triceps"))
        val engine = StatsEngine(
            BuiltInMetrics.all + BodyPartStatsProviders.metrics,
            BuiltInDimensions.all + BodyPartStatsProviders.bodyPartDimension(listOf(exercise)),
        )
        val sets = listOf(StrengthSet(weightKg = 20.0, reps = 10, isWarmup = true, setType = SetType.Warmup)) + working(2)
        val report = engine.calculate(
            listOf(session(0, entry(exercise, sets))),
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

    private fun working(count: Int) = List(count) { StrengthSet(weightKg = 100.0, reps = 5) }

    private fun entry(exercise: Exercise, sets: List<StrengthSet>) = WorkoutEntry(
        exerciseId = exercise.id,
        exerciseSnapshotPrimaryBodyPart = exercise.primaryBodyPart,
        strengthSets = sets,
    )

    private fun session(date: Long, vararg entries: WorkoutEntry) = WorkoutSession(
        id = "session",
        startedAtUtc = date,
        completedDateUtc = date,
        status = WorkoutSessionStatus.Completed,
        entries = entries.toList(),
    )

    private companion object {
        const val Hour = 3_600_000L
    }
}
