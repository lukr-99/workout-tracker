package com.lukr99.workout.ui.screens

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.lukr99.workout.data.WorkoutShareCardRenderer
import com.lukr99.workout.domain.WorkoutSession

/**
 * The platform's way to share a workout: the card image and the text.
 *
 * Each platform supplies this file; the web edition's is under web/.
 */
@Composable
fun rememberWorkoutSharer(): WorkoutSharer {
    val context = LocalContext.current
    return remember(context) {
        val renderer = WorkoutShareCardRenderer(context)
        object : WorkoutSharer {
            override suspend fun prepareImage(session: WorkoutSession, imperial: Boolean): PreparedWorkoutShare {
                val prepared = renderer.prepare(session, imperial)
                return PreparedWorkoutShare(prepared.bitmap.asImageBitmap()) {
                    runCatching { context.startActivity(Intent.createChooser(prepared.intent, "Share workout")) }.isSuccess
                }
            }

            override fun shareText(subject: String, text: String): Boolean {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, subject)
                    putExtra(Intent.EXTRA_TEXT, text)
                }
                return runCatching { context.startActivity(Intent.createChooser(intent, "Share workout")) }.isSuccess
            }
        }
    }
}
