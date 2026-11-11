package com.lukr99.workout.data.claude

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * The FCM token from Firebase, as in Lumen. A build without the owner's google-services.json has no
 * Firebase app, so this returns null and Ember runs without push: it answers Claude's requests when
 * it is opened instead.
 */
class FirebasePushTokenSource(private val context: Context) : PushTokenSource {
    override suspend fun token(): String? {
        if (FirebaseApp.getApps(context).isEmpty() && FirebaseApp.initializeApp(context) == null) return null
        return try {
            suspendCancellableCoroutine { continuation ->
                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    continuation.resume(if (task.isSuccessful) task.result else null)
                }
            }
        } catch (_: Exception) {
            // Play services missing or unreachable: no push on this device for now.
            null
        }
    }
}
