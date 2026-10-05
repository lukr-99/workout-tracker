package com.lukr99.workout.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

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
