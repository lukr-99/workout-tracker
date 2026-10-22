package com.lukr99.workout.ui

import com.lukr99.workout.domain.ExerciseCategory
import com.lukr99.workout.domain.Location
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.domain.nameOf

/**
 * Splits finished workouts into one line per place for each strength exercise. Every workout counts,
 * whether the exercise is shared or tracked per gym: this is the "All gyms" view, and the place a
 * workout was done at is the only thing that groups it. Workouts with no place form one "Unknown
 * location" line.
 */
object ExerciseSeries {

    /** Lines per exercise id, the place trained most recently first and "Unknown location" last. */
    fun byLocation(sessions: List<WorkoutSession>, locations: List<Location>): Map<String, List<LocationSeries>> {
        val points = LinkedHashMap<String, LinkedHashMap<String?, MutableList<ProgressPoint>>>()
        for (session in sessions.sortedBy { it.completedDateUtc ?: it.startedAtUtc }) {
            session.entries
                .filter { it.entryType == ExerciseCategory.Strength && it.exerciseId.isNotBlank() }
                .groupBy { it.exerciseId }
                .forEach { (exerciseId, entries) ->
                    val sets = entries.flatMap { it.strengthSets }
                    if (sets.isEmpty()) return@forEach
                    points.getOrPut(exerciseId) { LinkedHashMap() }
                        .getOrPut(session.locationId) { mutableListOf() }
                        .add(ProgressPoint.of(session, sets))
                }
        }
        return points.mapValues { (_, byPlace) ->
            byPlace.map { (locationId, list) -> LocationSeries(locationId, locations.nameOf(locationId), list) }
                .sortedWith(
                    compareBy<LocationSeries> { it.locationId == null }
                        .thenByDescending { it.points.last().dateMillis },
                )
        }
    }
}
