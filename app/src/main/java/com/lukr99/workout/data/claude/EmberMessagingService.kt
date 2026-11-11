package com.lukr99.workout.data.claude

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.lukr99.workout.WorkoutApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Receives the Claude server's content-free FCM messages (docs/claude-connector.md):
 * `{"t": "share", "r": <request id>}` when Claude asks for the data, and `{"t": "inbox"}` when Claude
 * proposed something. Android creates it, so it reads the container from [WorkoutApp].
 */
class EmberMessagingService : FirebaseMessagingService() {
    private val claude get() = (application as WorkoutApp).container.claude

    override fun onMessageReceived(message: RemoteMessage) {
        // FCM runs this on a background thread and gives a high-priority message about 20 seconds.
        runBlocking {
            withTimeoutOrNull(BUDGET_MS) {
                try {
                    when (message.data["t"]) {
                        "share" -> message.data["r"]?.let { requestId ->
                            if (claude.sharing.handle(requestId) == ShareAnswer.NeedsOwner) {
                                ClaudeNotifications.showShareRequest(this@EmberMessagingService, requestId)
                            }
                        }
                        "inbox" -> ClaudeNotifications.showInbox(this@EmberMessagingService, claude.inbox.pending().size)
                    }
                } catch (e: Exception) {
                    Log.w("EmberClaude", "Handling a push failed: ${e.message}")
                }
            }
        }
    }

    override fun onNewToken(token: String) {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching { claude.registerPush() }.onFailure { Log.w("EmberClaude", "Sending the push token failed: ${it.message}") }
        }
    }

    private companion object {
        const val BUDGET_MS = 15_000L
    }
}
