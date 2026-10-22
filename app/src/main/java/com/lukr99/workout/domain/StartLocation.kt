package com.lukr99.workout.domain

/**
 * The location to switch a new workout to after a GPS fix, or null to leave it alone. A fix near no
 * saved place changes nothing (the last used location stays), and so does a workout whose owner
 * already picked a place by hand while the fix was coming in.
 */
fun startLocationSwitch(currentId: String?, pickedByHand: Boolean, near: Location?): String? {
    if (near == null || pickedByHand || near.id == currentId) return null
    return near.id
}

/**
 * The place to offer saving the current position for: the workout's own location when it is a gym
 * or Home with no position yet, the fix is near no saved place, and the offer was not answered
 * before. Outdoors and archived places never get one.
 */
fun positionOfferFor(current: Location?, near: Location?, answered: Set<String>): Location? =
    current?.takeIf {
        near == null && it.kind != LocationKind.Outdoors && !it.isArchived && it.point == null && it.id !in answered
    }
