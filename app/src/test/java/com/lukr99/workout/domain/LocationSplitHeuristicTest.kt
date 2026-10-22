package com.lukr99.workout.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** Pins the edges of the "Track separately per gym?" prompt. */
class LocationSplitHeuristicTest {

    private val heuristic = LocationSplitHeuristic()

    @Test
    fun asksWhenOneGymIsFarFromAnother() {
        // The owner's case: 15 kg at one gym, 59 kg at the other.
        val sessions = listOf(
            workout(1, A, 15.0),
            workout(2, A, 15.0),
            workout(3, B, 59.0, status = WorkoutSessionStatus.Active),
        )
        val suggestion = heuristic.evaluate(sessions, FLY, B)
        assertNotNull(suggestion)
        assertEquals(A, suggestion!!.otherLocationId)
        assertEquals(59.0, suggestion.hereTopKg, 0.0)
        assertEquals(15.0, suggestion.otherTopKg, 0.0)
    }

    @Test
    fun staysQuietForNormalProgression() {
        val sessions = listOf(
            workout(1, A, 60.0),
            workout(2, A, 62.5),
            workout(3, B, 65.0),
        )
        assertNull(heuristic.evaluate(sessions, FLY, B))
    }

    @Test
    fun theThresholdIsInclusive() {
        val atThreshold = listOf(workout(1, A, 50.0), workout(2, A, 50.0), workout(3, B, 70.0))
        assertNotNull(heuristic.evaluate(atThreshold, FLY, B))
        val justUnder = listOf(workout(1, A, 50.0), workout(2, A, 50.0), workout(3, B, 69.5))
        assertNull(heuristic.evaluate(justUnder, FLY, B))
    }

    @Test
    fun worksInBothDirections() {
        val sessions = listOf(workout(1, A, 60.0), workout(2, A, 60.0), workout(3, B, 20.0))
        assertNotNull(heuristic.evaluate(sessions, FLY, B))
    }

    @Test
    fun needsEnoughWorkoutsAtTheOtherPlace() {
        val one = listOf(workout(1, A, 15.0), workout(2, B, 59.0))
        assertNull(heuristic.evaluate(one, FLY, B))
        val relaxed = LocationSplitHeuristic(minWorkoutsElsewhere = 1)
        assertNotNull(relaxed.evaluate(one, FLY, B))
    }

    @Test
    fun needsAWorkoutHere() {
        val sessions = listOf(workout(1, A, 15.0), workout(2, A, 15.0))
        assertNull(heuristic.evaluate(sessions, FLY, B))
    }

    @Test
    fun usesTheMedianOfRecentWorkoutsSoOneOddDayDoesNotCount() {
        // One heavy outlier at A among light days; B is close to the usual A numbers.
        val sessions = listOf(
            workout(1, A, 20.0),
            workout(2, A, 60.0),
            workout(3, A, 21.0),
            workout(4, B, 22.0),
        )
        assertNull(heuristic.evaluate(sessions, FLY, B))
    }

    @Test
    fun onlyTheNewestWorkoutsCount() {
        // Long ago A was heavy; its newest five are close to B.
        val old = (1..5).map { workout(it.toLong(), A, 80.0) }
        val recent = (6..10).map { workout(it.toLong(), A, 30.0) }
        val sessions = old + recent + workout(11, B, 32.0)
        assertNull(heuristic.evaluate(sessions, FLY, B))
    }

    @Test
    fun ignoresWarmUpsBodyweightAndUnknownOrDiscardedWorkouts() {
        val sessions = listOf(
            workout(1, A, 15.0),
            workout(2, A, 15.0),
            workout(3, null, 59.0),
            workout(4, C, 59.0, status = WorkoutSessionStatus.Discarded),
            workout(5, C, 59.0, status = WorkoutSessionStatus.Discarded),
            workout(6, B, 15.0, warmUpKg = 60.0),
        )
        assertNull(heuristic.evaluate(sessions, FLY, B))
        assertNull(heuristic.evaluate(listOf(workout(1, A, 0.0), workout(2, A, 0.0), workout(3, B, 20.0)), FLY, B))
    }

    @Test
    fun noLocationMeansNoQuestion() {
        val sessions = listOf(workout(1, A, 15.0), workout(2, A, 15.0), workout(3, null, 59.0))
        assertNull(heuristic.evaluate(sessions, FLY, null))
    }

    @Test
    fun picksTheFurthestPlaceWhenSeveralQualify() {
        val sessions = listOf(
            workout(1, A, 30.0), workout(2, A, 30.0),
            workout(3, C, 10.0), workout(4, C, 10.0),
            workout(5, B, 50.0),
        )
        assertEquals(C, heuristic.evaluate(sessions, FLY, B)!!.otherLocationId)
    }

    private fun workout(
        n: Long,
        location: String?,
        kg: Double,
        status: WorkoutSessionStatus = WorkoutSessionStatus.Completed,
        warmUpKg: Double? = null,
    ): WorkoutSession {
        val sets = buildList {
            warmUpKg?.let { add(StrengthSet(reps = 5, weightKg = it, tags = setOf(SetTag.Warmup))) }
            add(StrengthSet(reps = 10, weightKg = kg))
            add(StrengthSet(reps = 10, weightKg = kg * 0.9))
        }
        return WorkoutSession(
            id = "w$n",
            startedAtUtc = n * 1_000,
            completedDateUtc = (n * 1_000 + 500).takeIf { status == WorkoutSessionStatus.Completed },
            status = status,
            locationId = location,
            entries = listOf(WorkoutEntry(exerciseId = FLY, strengthSets = sets)),
        )
    }

    private companion object {
        const val FLY = "cable-fly"
        const val A = "gym-a"
        const val B = "gym-b"
        const val C = "gym-c"
    }
}
