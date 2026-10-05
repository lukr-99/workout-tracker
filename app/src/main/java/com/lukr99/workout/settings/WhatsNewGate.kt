package com.lukr99.workout.settings

import com.lukr99.workout.domain.WhatsNew
import com.lukr99.workout.domain.WhatsNewNote
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Decides whether Home shows the "what's new" card for this build. [current] is this build's
 * `versionCode`; [freshInstall] is true when the app was installed, not updated, as this version.
 */
class WhatsNewGate(
    private val store: SeenVersionStore,
    private val current: Int,
    private val freshInstall: Boolean,
    private val notes: List<WhatsNewNote> = WhatsNew.notes,
) {
    val due: Flow<WhatsNewNote?> = store.seen.map { WhatsNew.due(it, current, freshInstall, notes) }

    /** Puts the card away until the next release that has a note. */
    suspend fun dismiss() = store.markSeen(current)
}
