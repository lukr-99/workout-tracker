package com.lukr99.workout.domain.stats

import com.lukr99.workout.domain.query.WorkoutDataPoint
import kotlinx.datetime.TimeZone

interface DimensionProvider {
    val key: String
    fun resolve(point: WorkoutDataPoint, zoneId: TimeZone): String
}
