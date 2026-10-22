package com.lukr99.workout.domain

import com.lukr99.workout.domain.records.RecordsEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Last time's sets, records and progress in both modes: a shared exercise reads every workout, an
 * exercise tracked per location only reads workouts at the given place.
 */
class LocationScopeTest {

    // Cable fly: light at the Brno gym (double pulley), heavy at the other gym, one unknown workout.
    private val history = listOf(
        workout("brno-1", at = 1_000, location = BRNO, kg = 15.0),
        workout("other-1", at = 2_000, location = OTHER, kg = 59.0),
        workout("unknown-1", at = 3_000, location = null, kg = 40.0),
        workout("brno-2", at = 4_000, location = BRNO, kg = 17.5),
        workout("other-2", at = 5_000, location = OTHER, kg = 61.0),
    )

    private val shared = Exercise(id = FLY, name = "Cable Fly")
    private val perGym = shared.copy(trackPerLocation = true)

    @Test
    fun sharedExerciseCountsEveryWorkout() {
        val scope = LocationScope.of(shared, BRNO)
        assertEquals(LocationScope.Shared, scope)
        assertEquals(5, scope.apply(history).size)
        assertEquals(61.0, scope.apply(history).lastSetsFor(FLY).single().weightKg, 0.0)
    }

    @Test
    fun perLocationExerciseOnlyCountsWorkoutsThere() {
        val brno = LocationScope.of(perGym, BRNO).apply(history)
        assertEquals(listOf("brno-1", "brno-2"), brno.map { it.id })
        assertEquals(17.5, brno.lastSetsFor(FLY).single().weightKg, 0.0)

        val other = LocationScope.of(perGym, OTHER).apply(history)
        assertEquals(61.0, other.lastSetsFor(FLY).single().weightKg, 0.0)
    }

    @Test
    fun perLocationWithoutAPlaceOnlyCountsUnknownWorkouts() {
        val unknown = LocationScope.of(perGym, null).apply(history)
        assertEquals(listOf("unknown-1"), unknown.map { it.id })
    }

    @Test
    fun aNewPlaceStartsWithNoHistory() {
        val fresh = LocationScope.of(perGym, "new-gym").apply(history)
        assertTrue(fresh.lastSetsFor(FLY).isEmpty())
        assertTrue(RecordsEngine.evaluateSet(fresh, FLY, set(5.0)).isPersonalRecord)
    }

    @Test
    fun recordsFollowTheScope() {
        val candidate = set(20.0)
        // Shared: 20 kg is nowhere near the 61 kg from the other gym.
        assertFalse(RecordsEngine.evaluateSet(LocationScope.of(shared, BRNO).apply(history), FLY, candidate).isPersonalRecord)
        // Per gym: 20 kg beats Brno's 17.5 kg.
        val brno = RecordsEngine.evaluateSet(LocationScope.of(perGym, BRNO).apply(history), FLY, candidate)
        assertTrue(brno.isPersonalRecord)

        assertEquals(61.0, RecordsEngine.forExercise(LocationScope.Shared.apply(history), FLY).heaviestSet!!.value, 0.0)
        assertEquals(17.5, RecordsEngine.forExercise(LocationScope.of(perGym, BRNO).apply(history), FLY).heaviestSet!!.value, 0.0)
    }

    @Test
    fun progressAndOutingsFollowTheScope() {
        assertEquals(5, Progression.forExercise(LocationScope.Shared.apply(history), FLY).size)
        assertEquals(2, Progression.forExercise(LocationScope.of(perGym, OTHER).apply(history), FLY).size)
        val outings = LocationScope.of(perGym, BRNO).apply(history).recentOutings(FLY)
        assertEquals(listOf(4_000L, 1_000L), outings.map { it.atUtc })
    }

    @Test
    fun switchingBackToSharedJoinsTheViewsAgain() {
        val split = LocationScope.of(perGym, BRNO).apply(history)
        val joined = LocationScope.of(perGym.copy(trackPerLocation = false), BRNO).apply(history)
        assertEquals(2, split.size)
        assertEquals(history, joined)
    }

    private fun set(kg: Double, reps: Int = 10) = StrengthSet(reps = reps, weightKg = kg)

    private fun workout(id: String, at: Long, location: String?, kg: Double) = WorkoutSession(
        id = id,
        name = id,
        startedAtUtc = at - 500,
        completedDateUtc = at,
        status = WorkoutSessionStatus.Completed,
        locationId = location,
        entries = listOf(
            WorkoutEntry(
                id = "$id-e",
                workoutSessionId = id,
                exerciseId = FLY,
                exerciseSnapshotName = "Cable Fly",
                strengthSets = listOf(StrengthSet(id = "$id-s", reps = 10, weightKg = kg)),
            ),
        ),
    )

    private companion object {
        const val FLY = "cable-fly"
        const val BRNO = "brno"
        const val OTHER = "other"
    }
}
