package com.lukr99.workout.data.claude

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** [ClaudeLinkStore] in memory, for tests. */
class MemoryClaudeLinkStore(
    link: ClaudeLink? = ClaudeLink("https://claude.test", "wt1.token"),
    mode: ShareMode = ShareMode.Ask,
) : ClaudeLinkStore {
    private val linkState = MutableStateFlow(link)
    private val modeState = MutableStateFlow(mode)

    override val link: Flow<ClaudeLink?> = linkState
    override val shareMode: Flow<ShareMode> = modeState

    override suspend fun setLink(link: ClaudeLink?) {
        linkState.value = link
    }

    override suspend fun setShareMode(mode: ShareMode) {
        modeState.value = mode
    }
}
