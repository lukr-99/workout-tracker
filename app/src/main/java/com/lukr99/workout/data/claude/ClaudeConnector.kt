package com.lukr99.workout.data.claude

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Ember's side of the Claude connector (docs/claude-connector.md), as the container hands it out:
 * the link and share mode, the server API, the share handler, the proposal inbox and push.
 */
class ClaudeConnector(
    val store: ClaudeLinkStore,
    val api: ClaudeApi,
    val sharing: ShareRequestHandler,
    val inbox: ProposalInbox,
    private val pushTokens: PushTokenSource,
) {
    val link: Flow<ClaudeLink?> get() = store.link

    /** Sets this phone up with the server and registers its push token. True when push works. */
    suspend fun connect(serverUrl: String, setupSecret: String, deviceName: String): Boolean {
        api.setUp(serverUrl, setupSecret, deviceName)
        return registerPush()
    }

    /** Forgets the server on this phone. Claude's access and the server's copy end on the server's side. */
    suspend fun disconnect() = store.setLink(null)

    /** Sends the current push token, so the server can wake this phone. False when there is none. */
    suspend fun registerPush(): Boolean {
        if (store.link.first() == null) return false
        val token = pushTokens.token() ?: return false
        api.setPushToken(token)
        return true
    }

    /** Stops every Claude connector at once; the owner removes it in Claude too. */
    suspend fun removeClaude(): Int {
        val connectors = api.devices().filter(ClaudeDevice::isActiveConnector)
        connectors.forEach { api.revokeDevice(it.id) }
        return connectors.size
    }
}
