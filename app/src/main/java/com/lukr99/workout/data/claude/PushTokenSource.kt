package com.lukr99.workout.data.claude

/** Where the phone's push token comes from; null when this build or device has no push. */
fun interface PushTokenSource {
    suspend fun token(): String?
}
