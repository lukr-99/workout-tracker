package com.lukr99.workout.data.claude

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.lukr99.workout.WorkoutApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * The Allow and Deny buttons on Claude's share request. Android creates it, so it reads the container
 * from [WorkoutApp] (ARCHITECTURE.md). A failed answer is caught up the next time Ember opens.
 */
class ShareActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val requestId = intent.getStringExtra(ClaudeNotifications.EXTRA_REQUEST_ID) ?: return
        ClaudeNotifications.dismiss(context, requestId)
        val allow = intent.action == ClaudeNotifications.ACTION_ALLOW
        val sharing = (context.applicationContext as WorkoutApp).container.claude.sharing
        // A receiver has about ten seconds; goAsync keeps the process up while the share uploads.
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                withTimeout(BUDGET_MS) { if (allow) sharing.allow(requestId) else sharing.deny(requestId) }
            } catch (e: Exception) {
                Log.w("EmberClaude", "Answering share request failed: ${e.message}")
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val BUDGET_MS = 9_000L
    }
}
