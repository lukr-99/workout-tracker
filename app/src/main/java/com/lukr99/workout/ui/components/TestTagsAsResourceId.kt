package com.lukr99.workout.ui.components

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId

/**
 * Makes the test tags below this node visible to UI automation as resource ids. A bottom sheet or
 * dialog is its own window, so it needs this again. Each platform supplies this file; the web
 * edition's is under web/ and does nothing.
 */
@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.exposeTestTags(): Modifier = semantics { testTagsAsResourceId = true }
