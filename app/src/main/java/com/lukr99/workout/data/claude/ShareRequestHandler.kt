package com.lukr99.workout.data.claude

import kotlinx.coroutines.flow.first

/**
 * Answers Claude's share requests (docs/claude-connector.md, "Sharing on request"): by the share
 * mode, or with the owner's Allow or Deny.
 */
class ShareRequestHandler(
    private val api: ClaudeApi,
    private val builder: ClaudeShareBuilder,
    private val store: ClaudeLinkStore,
) {
    /** A request that came by push or was found open, answered by the share mode. */
    suspend fun handle(requestId: String): ShareAnswer = when (store.shareMode.first()) {
        ShareMode.Automatic -> allow(requestId)
        ShareMode.Never -> deny(requestId)
        ShareMode.Ask -> ShareAnswer.NeedsOwner
    }

    /** The owner tapped Allow. */
    suspend fun allow(requestId: String): ShareAnswer = answer {
        api.share(requestId, builder.build())
        ShareAnswer.Shared
    }

    /** The owner tapped Deny. */
    suspend fun deny(requestId: String): ShareAnswer = answer {
        api.decline(requestId)
        ShareAnswer.Declined
    }

    /** The owner shares from Settings before a chat, unasked. */
    suspend fun shareNow(): ShareStatus = api.share(null, builder.build())

    /**
     * Requests the phone missed (no push, or Ember was closed): answered by the share mode. Returns
     * the ones that wait for the owner; nothing before the phone is connected.
     */
    suspend fun catchUp(): List<ShareRequest> {
        if (store.link.first() == null) return emptyList()
        return api.shareRequests().filter { handle(it.id) == ShareAnswer.NeedsOwner }
    }

    private suspend fun answer(block: suspend () -> ShareAnswer): ShareAnswer = try {
        block()
    } catch (e: ClaudeApiException) {
        if (e.code == "expired" || e.code == "not_found") ShareAnswer.Expired else throw e
    }
}
