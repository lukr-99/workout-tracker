package com.lukr99.workout.domain.stats

import com.lukr99.workout.domain.query.WorkoutDataPoint
import java.time.ZoneId

interface DimensionProvider {
    val key: String
    fun resolve(point: WorkoutDataPoint, zoneId: ZoneId): String
}
