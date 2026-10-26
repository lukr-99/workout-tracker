package com.lukr99.workout.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.ui.components.LocalToast
import com.lukr99.workout.web.platform.canCopyText
import com.lukr99.workout.web.platform.canShareText
import com.lukr99.workout.web.platform.copyTextToClipboard
import com.lukr99.workout.web.platform.shareTextNatively
import kotlinx.coroutines.await
import kotlinx.coroutines.launch

/**
 * The platform's way to share a workout. The web edition shares the text only: the browser's share
 * sheet when there is one, else the clipboard (and says so). It has no share card image yet.
 *
 * Each platform supplies this file.
 */
@Composable
fun rememberWorkoutSharer(): WorkoutSharer {
    val toast = LocalToast.current
    val scope = rememberCoroutineScope()
    return remember(toast, scope) {
        object : WorkoutSharer {
            override suspend fun prepareImage(session: WorkoutSession, imperial: Boolean): PreparedWorkoutShare =
                error("The share image is not available in the web edition yet.")

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
