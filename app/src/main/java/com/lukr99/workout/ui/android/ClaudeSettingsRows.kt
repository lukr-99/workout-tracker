package com.lukr99.workout.ui.android

import com.lukr99.workout.ui.i18n.LocalLanguage
import com.lukr99.workout.ui.i18n.CommonText
import com.lukr99.workout.ui.i18n.ClaudeText
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
    val lang = LocalLanguage.current
    LaunchedEffect(state.link) { if (state.link != null) vm.refresh() }
    val link = state.link
    if (link == null) {
        ButtonRow(
            label = ClaudeText.notConnected.text(),
            hint = state.error ?: ClaudeText.notConnectedHint.text(),
            hintIsError = state.error != null,
            action = ClaudeText.connect.text(),
            first = true,
            onClick = vm::openConnect,
        )
        return
    }
    SettingsRow(
        label = ClaudeText.connected.text(),
        hint = state.error ?: (link.serverUrl.removePrefix("https://") + if (state.pushOn) "" else ClaudeText.pushOff.text()),
        hintIsError = state.error != null,
        first = true,
    ) { PillButton(ClaudeText.disconnect.text(), busy = state.busy == "disconnect", onClick = vm::disconnect) }

    state.requests.forEach { request ->
        SettingsRow(ClaudeText.asksTitle.text(), ClaudeText.asksHint.text()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillButton(ClaudeText.deny.text(), busy = state.busy == "request", onClick = { vm.answer(request.id, allow = false) })
                PillButton(ClaudeText.allow.text(), busy = state.busy == "request", onClick = { vm.answer(request.id, allow = true) })
            }
        }
    }
    SettingsRow(ClaudeText.whenAsksTitle.text(), ClaudeText.whenAsksHint.text())
    SegmentedControl(
        options = ShareMode.entries,
        selected = state.shareMode,
        onSelect = vm::setShareMode,
        label = {
            when (it) {
                ShareMode.Ask -> ClaudeText.modeAsk.get(lang)
                ShareMode.Automatic -> ClaudeText.modeAutomatic.get(lang)
                ShareMode.Never -> ClaudeText.modeNever.get(lang)
            }
        },
        modifier = Modifier.padding(bottom = 8.dp),
    )
    ButtonRow(
        label = ClaudeText.sharedData.text(),
        hint = state.share?.let { ClaudeText.sharedAt.text("from" to clock(it.sharedAt), "until" to clock(it.keepUntil)) } ?: ClaudeText.nothingShared.text(),
        action = ClaudeText.shareNow.text(),
        busy = state.busy == "share",
        busyLabel = ClaudeText.sharing.text(),
        onClick = vm::shareNow,
    )
    if (state.share != null) {
        ButtonRow(ClaudeText.stopSharing.text(), ClaudeText.stopSharingHint.text(), ClaudeText.delete.text(), busy = state.busy == "stop", onClick = vm::stopSharing)
    }
    ButtonRow(
        label = ClaudeText.connectClaude.text(),
        hint = ClaudeText.connectClaudeHint.text(),
        action = ClaudeText.getCode.text(),
        busy = state.busy == "code",
        onClick = vm::getCode,
    )
    if (state.proposals.isEmpty()) {
        SettingsRow(ClaudeText.proposalsTitle.text(), ClaudeText.proposalsEmpty.text())
    } else {
        state.proposals.forEach { ProposalRow(it, busy = state.busy == "proposal", onAccept = { vm.accept(it) }, onReject = { vm.reject(it) }) }
    }
    ButtonRow(
        label = ClaudeText.removeClaude.text(),
        hint = ClaudeText.removeHint.text(),
        action = ClaudeText.remove.text(),
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
        val addressCopied = ClaudeText.addressCopied.text()
        AlertDialog(
            onDismissRequest = vm::dismissCode,
            title = { Text(ClaudeText.connectClaude.text()) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(ClaudeText.codeIntro.text(), color = EmberTheme.colors.textSecondary)
                    Text(code.mcpUrl, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
                    Text(ClaudeText.codeSignIn.text("time" to clock(code.expiresAt)), color = EmberTheme.colors.textSecondary)
                    Text(code.code, style = MaterialTheme.typography.headlineMedium, fontFamily = FontFamily.Monospace)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    clipboard.setText(AnnotatedString(code.mcpUrl))
                    toast(addressCopied)
                }) { Text(ClaudeText.copyAddress.text()) }
            },
            dismissButton = { TextButton(onClick = vm::dismissCode) { Text(CommonText.done.text()) } },
        )
    }
}

@Composable
private fun ProposalRow(proposal: Proposal, busy: Boolean, onAccept: () -> Unit, onReject: () -> Unit) {
    SettingsRow(
        label = (if (proposal.isTemplate) ClaudeText.templatePrefix.text() else ClaudeText.exercisePrefix.text()) + proposal.title,
        hint = listOf(proposal.reason, proposalDetails(proposal)).filter(String::isNotBlank).joinToString("\n"),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PillButton(ClaudeText.reject.text(), busy = busy, onClick = onReject)
            PillButton(ClaudeText.accept.text(), busy = busy, onClick = onAccept)
        }
    }
}

private val CLOCK = DateTimeFormatter.ofPattern("HH:mm")

/** An ISO-8601 instant as this phone's local time, such as "14:32". */
private fun clock(iso: String): String = runCatching { CLOCK.format(Instant.parse(iso).atZone(ZoneId.systemDefault())) }.getOrDefault(iso)
