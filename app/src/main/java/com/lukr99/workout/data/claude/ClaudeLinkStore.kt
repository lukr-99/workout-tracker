package com.lukr99.workout.data.claude

import kotlinx.coroutines.flow.Flow

/** This phone's link to the Claude server and its share mode. Not in backups: it belongs to one phone. */
interface ClaudeLinkStore {
    val link: Flow<ClaudeLink?>
    val shareMode: Flow<ShareMode>

    suspend fun setLink(link: ClaudeLink?)

    suspend fun setShareMode(mode: ShareMode)
}
