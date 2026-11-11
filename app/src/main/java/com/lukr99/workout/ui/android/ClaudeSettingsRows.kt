package com.lukr99.workout.ui.android

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.lukr99.workout.data.claude.Proposal
import com.lukr99.workout.data.claude.ShareMode
import com.lukr99.workout.ui.components.LocalToast
import com.lukr99.workout.ui.components.SegmentedControl
import com.lukr99.workout.ui.settings.ButtonRow
import com.lukr99.workout.ui.settings.PillButton
import com.lukr99.workout.ui.settings.SettingsRow
import com.lukr99.workout.ui.theme.EmberTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** The rows of Settings, Claude; they reload from the server whenever the link changes. */
@Composable
fun ClaudeSettingsRows(vm: ClaudeSettingsViewModel) {
    val state by vm.ui.collectAsState()
    LaunchedEffect(state.link) { if (state.link != null) vm.refresh() }
    val link = state.link
    if (link == null) {
        ButtonRow(
            label = "Not connected",
            hint = state.error ?: "Let Claude read your workouts and runs when you allow it, and propose templates. Needs your own Claude server.",
            hintIsError = state.error != null,
            action = "Connect",
            first = true,
            onClick = vm::openConnect,
        )
        return
    }
    SettingsRow(
        label = "Connected",
        hint = state.error ?: (link.serverUrl.removePrefix("https://") + if (state.pushOn) "" else ". Push is off, so Ember asks when you open it."),
        hintIsError = state.error != null,
        first = true,
    ) { PillButton("Disconnect", busy = state.busy == "disconnect", onClick = vm::disconnect) }

    state.requests.forEach { request ->
        SettingsRow("Claude asks for your data", "Allow shares your workouts, runs and templates for 30 minutes.") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillButton("Deny", busy = state.busy == "request", onClick = { vm.answer(request.id, allow = false) })
                PillButton("Allow", busy = state.busy == "request", onClick = { vm.answer(request.id, allow = true) })
            }
        }
    }
    SettingsRow("When Claude asks for your data", "Ask me shows Allow and Deny. Shared data is deleted from the server after 30 minutes.")
    SegmentedControl(
        options = ShareMode.entries,
        selected = state.shareMode,
        onSelect = vm::setShareMode,
        label = {
            when (it) {
                ShareMode.Ask -> "Ask me"
                ShareMode.Automatic -> "Always share"
                ShareMode.Never -> "Never"
            }
        },
        modifier = Modifier.padding(bottom = 8.dp),
    )
    ButtonRow(
        label = "Shared data",
        hint = state.share?.let { "Shared at ${clock(it.sharedAt)}, deleted at ${clock(it.keepUntil)}." } ?: "Nothing is shared now. Share before a chat to skip the request.",
        action = "Share now",
        busy = state.busy == "share",
        busyLabel = "Sharing…",
        onClick = vm::shareNow,
    )
    if (state.share != null) {
        ButtonRow("Stop sharing", "Deletes the copy on the server now.", "Delete", busy = state.busy == "stop", onClick = vm::stopSharing)
    }
    ButtonRow(
        label = "Connect Claude",
        hint = "Shows the address and a one-time code for Claude's Connectors settings.",
        action = "Get code",
        busy = state.busy == "code",
        onClick = vm::getCode,
    )
    if (state.proposals.isEmpty()) {
        SettingsRow("Claude's proposals", "Nothing waits for you. Templates and exercises Claude proposes show here.")
    } else {
        state.proposals.forEach { ProposalRow(it, busy = state.busy == "proposal", onAccept = { vm.accept(it) }, onReject = { vm.reject(it) }) }
    }
    ButtonRow(
        label = "Remove Claude",
        hint = "Stops Claude reading Ember at once. Remove the connector in Claude too.",
        action = "Remove",
        busy = state.busy == "remove",
        onClick = vm::removeClaude,
    )
}

/**
 * The connect form and the connector code. They belong to the whole screen, so they outlive a scrolled
 * row. [onConnect] gets the typed address and secret.
 */
@Composable
fun ClaudeSettingsDialogs(vm: ClaudeSettingsViewModel, onConnect: (serverUrl: String, setupSecret: String) -> Unit) {
    val state by vm.ui.collectAsState()
    val toast = LocalToast.current
    LaunchedEffect(state.message) {
        state.message?.let {
            toast(it)
            vm.consumeMessage()
        }
    }
    if (state.connectOpen) {
        ClaudeConnectDialog(error = state.error, connecting = state.busy == "connect", onConnect = onConnect, onDismiss = vm::dismissConnect)
    }
    state.code?.let { code ->
        val clipboard = LocalClipboardManager.current
        AlertDialog(
            onDismissRequest = vm::dismissCode,
            title = { Text("Connect Claude") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("In Claude, open Settings, Connectors, Add custom connector, and paste this address:", color = EmberTheme.colors.textSecondary)
                    Text(code.mcpUrl, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
                    Text("On the sign-in page that opens, enter this code. It works once, until ${clock(code.expiresAt)}.", color = EmberTheme.colors.textSecondary)
                    Text(code.code, style = MaterialTheme.typography.headlineMedium, fontFamily = FontFamily.Monospace)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    clipboard.setText(AnnotatedString(code.mcpUrl))
                    toast("Address copied.")
                }) { Text("Copy address") }
            },
            dismissButton = { TextButton(onClick = vm::dismissCode) { Text("Done") } },
        )
    }
}

@Composable
private fun ProposalRow(proposal: Proposal, busy: Boolean, onAccept: () -> Unit, onReject: () -> Unit) {
    SettingsRow(
        label = (if (proposal.isTemplate) "Template: " else "Exercise: ") + proposal.title,
        hint = listOf(proposal.reason, proposalDetails(proposal)).filter(String::isNotBlank).joinToString("\n"),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PillButton("Reject", busy = busy, onClick = onReject)
            PillButton("Accept", busy = busy, onClick = onAccept)
        }
    }
}

private val CLOCK = DateTimeFormatter.ofPattern("HH:mm")

/** An ISO-8601 instant as this phone's local time, such as "14:32". */
private fun clock(iso: String): String = runCatching { CLOCK.format(Instant.parse(iso).atZone(ZoneId.systemDefault())) }.getOrDefault(iso)
