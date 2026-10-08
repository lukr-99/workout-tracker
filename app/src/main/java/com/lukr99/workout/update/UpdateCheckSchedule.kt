package com.lukr99.workout.update

/** When Ember looks for a new release by itself: on launch, with the switch on, once a day at most. */
object UpdateCheckSchedule {

    const val INTERVAL_MS = 24L * 60 * 60 * 1000

    /** Whether to check now: [enabled], and never checked or last checked [INTERVAL_MS] ago or more. */
    fun isDue(enabled: Boolean, lastCheckUtc: Long?, nowUtc: Long): Boolean =
        enabled && (lastCheckUtc == null || nowUtc - lastCheckUtc >= INTERVAL_MS || nowUtc < lastCheckUtc)
}
