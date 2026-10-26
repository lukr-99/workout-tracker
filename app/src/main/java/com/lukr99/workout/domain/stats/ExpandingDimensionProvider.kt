package com.lukr99.workout.domain.stats

import com.lukr99.workout.domain.query.WorkoutDataPoint
import kotlinx.datetime.TimeZone

/**
 * A dimension that can map one observation to several groups (for example one exercise loading
 * both a primary and secondary muscle). Existing single-value providers remain unchanged.
 */
interface ExpandingDimensionProvider : DimensionProvider {
    fun resolveValues(point: WorkoutDataPoint, zoneId: TimeZone): Set<String>
    override fun resolve(point: WorkoutDataPoint, zoneId: TimeZone): String =
        resolveValues(point, zoneId).firstOrNull().orEmpty()
}
