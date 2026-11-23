package com.lukr99.workout.data.claude

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.lukr99.workout.MainActivity
import com.lukr99.workout.R

/**
 * The connector's notifications: Claude asking for the training data, with Allow and Deny, and
 * Claude's proposals waiting in Settings, Claude. Without the notification permission they are not
 * shown, and Ember asks when it is opened instead.
 */
object ClaudeNotifications {
    const val ACTION_ALLOW = "com.lukr99.workout.claude.ALLOW"
    const val ACTION_DENY = "com.lukr99.workout.claude.DENY"
    const val EXTRA_REQUEST_ID = "requestId"

    private const val REQUEST_CHANNEL = "claude_requests"
    private const val INBOX_CHANNEL = "claude_inbox"
    private const val INBOX_ID = 4100

    /** Asks the owner to share: Allow or Deny. It goes away by itself when the request lapses. */
    fun showShareRequest(context: Context, requestId: String) {
        ensureChannels(context)
        val notification = NotificationCompat.Builder(context, REQUEST_CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_claude)
            .setContentTitle(context.getString(R.string.claude_share_title))
            .setContentText(context.getString(R.string.claude_share_text))
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    context.getString(R.string.claude_share_big),
                ),
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setTimeoutAfter(REQUEST_LIFETIME_MS)
            .setContentIntent(openEmber(context))
            .addAction(0, context.getString(R.string.claude_action_allow), answer(context, ACTION_ALLOW, requestId))
            .addAction(0, context.getString(R.string.claude_action_deny), answer(context, ACTION_DENY, requestId))
            .build()
        notify(context, notificationId(requestId), notification)
    }

    fun dismiss(context: Context, requestId: String) {
        NotificationManagerCompat.from(context).cancel(notificationId(requestId))
    }

    /** Says Claude proposed something; the owner accepts or rejects it in Settings, Claude. */
    fun showInbox(context: Context, pending: Int) {
        if (pending == 0) {
            NotificationManagerCompat.from(context).cancel(INBOX_ID)
            return
        }
        ensureChannels(context)
        val notification = NotificationCompat.Builder(context, INBOX_CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_claude)
            .setContentTitle(context.resources.getQuantityString(R.plurals.claude_inbox_title, pending, pending))
            .setContentText(context.getString(R.string.claude_inbox_text))
            .setAutoCancel(true)
            .setContentIntent(openEmber(context))
            .build()
        notify(context, INBOX_ID, notification)
    }

    private fun notify(context: Context, id: Int, notification: android.app.Notification) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        try {
            manager.notify(id, notification)
        } catch (_: SecurityException) {
            // The permission was taken back between the check and the call.
        }
    }

    private fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(REQUEST_CHANNEL, context.getString(R.string.claude_channel_requests), NotificationManager.IMPORTANCE_HIGH).apply {
                description = context.getString(R.string.claude_channel_requests_description)
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(INBOX_CHANNEL, context.getString(R.string.claude_channel_proposals), NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = context.getString(R.string.claude_channel_proposals_description)
            },
        )
    }

    private fun answer(context: Context, action: String, requestId: String): PendingIntent {
        val intent = Intent(context, ShareActionReceiver::class.java).setAction(action).putExtra(EXTRA_REQUEST_ID, requestId)
        val code = notificationId(requestId) * 2 + if (action == ACTION_ALLOW) 0 else 1
        return PendingIntent.getBroadcast(context, code, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun openEmber(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun notificationId(requestId: String): Int = 4000 + (requestId.hashCode() and 0x3F)

    private const val REQUEST_LIFETIME_MS = 10L * 60 * 1000
}
