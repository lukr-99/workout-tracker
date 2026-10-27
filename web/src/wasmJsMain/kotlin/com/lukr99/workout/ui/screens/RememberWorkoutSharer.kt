package com.lukr99.workout.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.ui.components.LocalToast
import com.lukr99.workout.web.WebWorkoutShareCard
import com.lukr99.workout.web.platform.canCopyText
import com.lukr99.workout.web.platform.canShareText
import com.lukr99.workout.web.platform.copyTextToClipboard
import com.lukr99.workout.web.platform.downloadBase64
import com.lukr99.workout.web.platform.shareImageFile
import com.lukr99.workout.web.platform.shareTextNatively
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.coroutines.await
import kotlinx.coroutines.launch

/**
 * The platform's way to share a workout. The card image is drawn on a Skia surface and shared as a
 * PNG file through the browser's share sheet, or saved as a download when the browser cannot share
 * files. The text goes to the share sheet, else to the clipboard (and says so).
 *
 * Each platform supplies this file.
 */
@OptIn(ExperimentalEncodingApi::class)
@Composable
fun rememberWorkoutSharer(): WorkoutSharer {
    val toast = LocalToast.current
    val scope = rememberCoroutineScope()
    return remember(toast, scope) {
        object : WorkoutSharer {
            override suspend fun prepareImage(session: WorkoutSession, imperial: Boolean): PreparedWorkoutShare {
                val card = WebWorkoutShareCard.render(session, imperial)
                val base64 = Base64.Default.encode(card.png)
                val fileName = "ember-workout-${session.id.take(8)}.png"
                return PreparedWorkoutShare(card.image.toComposeImageBitmap()) {
                    // The share sheet has to open inside the tap, so it starts here and only the wait is launched.
                    val outcome = runCatching { shareImageFile(fileName, base64, "image/png", "Share workout") }.getOrNull()
                    scope.launch {
                        val result = outcome?.let { runCatching { it.await<JsString>().toString() }.getOrNull() }
                        if (result != "shared" && result != "cancelled") {
                            downloadBase64(fileName, base64, "image/png")
                            toast("Saved the workout image.")
                        }
                    }
                    true
                }
            }

            override fun shareText(subject: String, text: String): Boolean = when {
                canShareText() -> {
                    // The share sheet has to open inside the tap, so it starts here and only the wait is launched.
                    val outcome = shareTextNatively(subject, text)
                    scope.launch {
                        if (outcome.await<JsString>().toString() == "failed") copy(text)
                    }
                    true
                }
                canCopyText() -> {
                    scope.launch { copy(text) }
                    true
                }
                else -> false
            }

            private suspend fun copy(text: String) {
                val copied = canCopyText() && copyTextToClipboard(text).await<JsBoolean>().toBoolean()
                toast(if (copied) "Workout copied to the clipboard." else "Could not share or copy the workout.")
            }
        }
    }
}
