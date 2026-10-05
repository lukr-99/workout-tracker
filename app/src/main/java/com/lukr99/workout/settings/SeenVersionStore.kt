package com.lukr99.workout.settings

import kotlinx.coroutines.flow.Flow

/** Remembers the last version whose "what's new" card was dismissed. */
interface SeenVersionStore {
    /** The version code, or null when no card was ever dismissed. */
    val seen: Flow<Int?>

    suspend fun markSeen(versionCode: Int)
}
