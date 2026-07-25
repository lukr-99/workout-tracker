package com.lukr99.workout.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Transient top notification for confirmations (replaces MAUI inline status blocks —
 * 02-design-system.md). Provided app-wide via [LocalToast]; any screen calls `LocalToast.current(msg)`.
 */
class ToastState {
    var message by mutableStateOf<String?>(null)
        private set

    operator fun invoke(text: String) {
        message = text
    }

    internal fun clear() {
        message = null
    }
}

val LocalToast = staticCompositionLocalOf { ToastState() }

@Composable
fun rememberToastState(): ToastState = remember { ToastState() }

/** Renders the current toast pinned near the top; auto-dismisses after a short delay. */
@Composable
fun ToastHost(state: ToastState, modifier: Modifier = Modifier) {
    val message = state.message
    LaunchedEffect(message) {
        if (message != null) {
            delay(2_200)
            state.clear()
        }
    }
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        AnimatedVisibility(
            visible = message != null,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
        ) {
            Text(
                text = message.orEmpty(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .padding(top = 12.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 18.dp, vertical = 10.dp),
            )
        }
    }
}
