package com.lukr99.workout.ui

import com.lukr99.workout.domain.ExerciseCategory
import com.lukr99.workout.domain.Location
import com.lukr99.workout.domain.LocationScope
import com.lukr99.workout.domain.StrengthSet
import com.lukr99.workout.domain.WorkoutEntry
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.domain.WorkoutSessionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The "All gyms" chart: one line per place, whatever the exercise's mode. */
class ExerciseSeriesTest {

    private val locations = listOf(Location(id = BRNO, name = "Brno gym"), Location(id = OTHER, name = "Other gym")) + Location.builtIns

    private val history = listOf(
        workout("b1", at = 1_000, location = BRNO, kg = 15.0),
        workout("o1", at = 2_000, location = OTHER, kg = 59.0),
        workout("u1", at = 3_000, location = null, kg = 40.0),
        workout("b2", at = 4_000, location = BRNO, kg = 17.5),
        workout("gone", at = 5_000, location = "deleted-place", kg = 20.0),
    )

    @Test
    fun groupsWorkoutsByPlaceOldestFirst() {
        val series = ExerciseSeries.byLocation(history, locations).getValue(FLY)
        val brno = series.single { it.locationId == BRNO }
        assertEquals("Brno gym", brno.name)
        assertEquals(listOf(1_000L, 4_000L), brno.points.map { it.dateMillis })
        assertEquals(1, series.single { it.locationId == OTHER }.points.size)
        assertEquals(59.0, series.single { it.locationId == OTHER }.points.single().volumeKg / 10.0, 0.0)
    }

    @Test
    fun workoutsWithoutAPlaceShowAsUnknownLocationAndSortLast() {
        val series = ExerciseSeries.byLocation(history, locations).getValue(FLY)
        assertEquals(Location.UNKNOWN_NAME, series.last().name)
        assertNull(series.last().locationId)
        // A place that no longer exists reads as unknown rather than crashing.
        assertEquals(Location.UNKNOWN_NAME, series.single { it.locationId == "deleted-place" }.name)
    }

    @Test
    fun placesAreOrderedByMostRecentWorkout() {
        val ids = ExerciseSeries.byLocation(history, locations).getValue(FLY).map { it.locationId }
        assertEquals(listOf("deleted-place", BRNO, OTHER, null), ids)
    }

    @Test
    fun ignoresEmptyExercisesAndOtherCategories() {
        val cardio = WorkoutSession(
            id = "c", name = "run", startedAtUtc = 1, completedDateUtc = 2, status = WorkoutSessionStatus.Completed,
            locationId = BRNO,
            entries = listOf(
                WorkoutEntry(exerciseId = "bike", entryType = ExerciseCategory.Cardio),
                WorkoutEntry(exerciseId = "empty", entryType = ExerciseCategory.Strength),
            ),
        )
        assertTrue(ExerciseSeries.byLocation(listOf(cardio), locations).isEmpty())
    }

    @Test
    fun theSwitchIsOfferedWhenThereIsSomethingToSplit() {
        val series = ExerciseSeries.byLocation(history, locations).getValue(FLY)
        val shared = summary(LocationScope.Shared, series)
        assertTrue(shared.canSplitByGym)
        assertEquals("All gyms", shared.defaultViewLabel)
        assertEquals("By gym", shared.splitViewLabel)

        // One place only: nothing to compare for a shared exercise.
        assertFalse(summary(LocationScope.Shared, series.take(1)).canSplitByGym)

        val perGym = summary(LocationScope(perLocation = true, locationId = BRNO), series.take(1))
        assertTrue(perGym.canSplitByGym)
        assertEquals("This gym", perGym.defaultViewLabel)
        assertEquals("All gyms", perGym.splitViewLabel)
        assertFalse(summary(LocationScope(true, BRNO), emptyList()).canSplitByGym)
    }

    private fun summary(scope: LocationScope, series: List<LocationSeries>) = ExerciseProgressSummary(
        exerciseId = FLY, name = "Cable Fly", bodyPart = "Chest", category = ExerciseCategory.Strength,
        bestE1rmKg = 0.0, latestE1rmKg = 0.0, deltaKg = 0.0, points = emptyList(), scope = scope, series = series,
    )

    private fun workout(id: String, at: Long, location: String?, kg: Double) = WorkoutSession(
        id = id, name = id, startedAtUtc = at - 500, completedDateUtc = at,
        status = WorkoutSessionStatus.Completed, locationId = location,
        entries = listOf(
            WorkoutEntry(
                exerciseId = FLY, exerciseSnapshotName = "Cable Fly",
                strengthSets = listOf(StrengthSet(reps = 10, weightKg = kg, performedAtUtc = at)),
            ),
        ),
    )

    private companion object {
        const val FLY = "cable-fly"
        const val BRNO = "brno"
        const val OTHER = "other"
    }
}
