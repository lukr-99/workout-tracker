package com.lukr99.workout.data.location

import com.lukr99.workout.domain.GeoPoint

/**
 * One-shot device position, behind an interface so the suggestion logic can be tested with a fake.
 * Never asks for permission: [hasPermission] only reports whether the app already has it.
 */
interface PositionProvider {
    fun hasPermission(): Boolean

    /** One fix, or null when there is none within [timeoutMs] or location is unavailable. */
    suspend fun currentPosition(quality: FixQuality, timeoutMs: Long): GeoPoint?
}
