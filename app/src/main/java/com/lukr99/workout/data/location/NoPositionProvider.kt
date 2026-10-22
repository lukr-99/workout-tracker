package com.lukr99.workout.data.location

import com.lukr99.workout.domain.GeoPoint

/** Used where no real provider is wired, such as screenshot tests: never has a position. */
object NoPositionProvider : PositionProvider {
    override fun hasPermission() = false
    override suspend fun currentPosition(quality: FixQuality, timeoutMs: Long): GeoPoint? = null
}
