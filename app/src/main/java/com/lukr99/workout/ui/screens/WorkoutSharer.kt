package com.lukr99.workout.ui.screens

import com.lukr99.workout.domain.WorkoutSession

/** What the platform can do to share a finished workout. */
interface WorkoutSharer {
    /** Draws the share card for [session] and keeps it ready for [PreparedWorkoutShare.share]. */
    suspend fun prepareImage(session: WorkoutSession, imperial: Boolean): PreparedWorkoutShare

    /** Opens the share sheet with plain text. Returns false when it could not be opened. */
    fun shareText(subject: String, text: String): Boolean
}
