package com.lukr99.workout.ui.android

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.lukr99.workout.ui.theme.EmberTheme

/**
 * The form that connects Ember to the Claude server. What is typed is kept with `rememberSaveable`,
 * so turning the phone keeps it (docs/pitfalls.md, "Turning the phone loses what was typed in a
 * dialog"); whether the dialog is open lives in [ClaudeSettingsViewModel].
 */
@Composable
fun ClaudeConnectDialog(
    error: String?,
    connecting: Boolean,
    onConnect: (serverUrl: String, setupSecret: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var server by rememberSaveable { mutableStateOf("") }
    var secret by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Connect to your Claude server") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "The server's address and its setup secret, from whoever set up the server (cloud/README.md).",
                    color = EmberTheme.colors.textSecondary,
                )
                OutlinedTextField(
                    value = server,
                    onValueChange = { server = it },
                    label = { Text("Server address") },
                    placeholder = { Text("workout-tracker.you.workers.dev") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth().testTag(ClaudeTags.SERVER_ADDRESS),
                )
                OutlinedTextField(
                    value = secret,
                    onValueChange = { secret = it },
                    label = { Text("Setup secret") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth().testTag(ClaudeTags.SETUP_SECRET),
                )
                error?.let { Text(it, color = EmberTheme.colors.danger) }
            }
        },
        confirmButton = {
            TextButton(
                enabled = server.isNotBlank() && secret.isNotBlank() && !connecting,
                onClick = { onConnect(server, secret) },
                modifier = Modifier.testTag(ClaudeTags.CONNECT),
            ) {
                Text(if (connecting) "Connecting…" else "Connect")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
