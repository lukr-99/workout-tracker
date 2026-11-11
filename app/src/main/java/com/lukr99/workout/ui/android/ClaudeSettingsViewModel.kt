package com.lukr99.workout.ui.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lukr99.workout.data.claude.ClaudeConnector
import com.lukr99.workout.data.claude.Proposal
import com.lukr99.workout.data.claude.ShareMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Settings, Claude (docs/claude-connector.md): connect this phone to the Claude server, choose how
 * Claude's requests are answered, share now or delete the shared copy, get a code for Claude's
 * sign-in, and accept or reject Claude's proposals. Android only.
 */
class ClaudeSettingsViewModel(private val claude: ClaudeConnector) : ViewModel() {
    private val state = MutableStateFlow(ClaudeSettingsState())
    val ui: StateFlow<ClaudeSettingsState> = state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(claude.store.link, claude.store.shareMode) { link, mode -> link to mode }.collect { (link, mode) ->
                state.update { it.copy(link = link, shareMode = mode) }
            }
        }
    }

    /** Reloads the shared copy and the waiting proposals from the server. */
    fun refresh() = launchAction("refresh") {
        if (state.value.link == null) return@launchAction
        val share = claude.api.shareStatus()
        val requests = claude.sharing.catchUp()
        val proposals = claude.inbox.pending()
        state.update { it.copy(share = share, requests = requests, proposals = proposals) }
    }

    /**
     * Reloads only the waiting proposals, for Home each time Ember comes to the front. Quiet: it
     * changes nothing on the server and shows no error, since Home works without the server.
     */
    fun refreshProposals() {
        if (state.value.link == null || state.value.busy != null) return
        viewModelScope.launch {
            runCatching { claude.inbox.pending() }.onSuccess { proposals -> state.update { it.copy(proposals = proposals) } }
        }
    }

    fun openConnect() = state.update { it.copy(connectOpen = true, error = null) }

    fun dismissConnect() = state.update { it.copy(connectOpen = false) }

    fun connect(serverUrl: String, setupSecret: String, deviceName: String) = launchAction("connect") {
        val push = claude.connect(serverUrl, setupSecret, deviceName)
        state.update { it.copy(connectOpen = false, pushOn = push, message = "Connected. Now connect Claude.") }
    }

    /** After the notification permission answer: sends the push token, if there is one. */
    fun registerPush() = launchAction("push") {
        val push = claude.registerPush()
        state.update { it.copy(pushOn = push) }
    }

    fun disconnect() = launchAction("disconnect") {
        claude.disconnect()
        state.update { it.copy(share = null, proposals = emptyList(), code = null, message = "Ember is no longer connected on this phone.") }
    }

    fun setShareMode(mode: ShareMode) {
        viewModelScope.launch { claude.store.setShareMode(mode) }
    }

    fun shareNow() = launchAction("share") {
        val share = claude.sharing.shareNow()
        state.update { it.copy(share = share, message = "Shared for 30 minutes.") }
    }

    /** The owner answers an open request here instead of in the notification. */
    fun answer(requestId: String, allow: Boolean) = launchAction("request") {
        if (allow) claude.sharing.allow(requestId) else claude.sharing.deny(requestId)
        val share = if (allow) claude.api.shareStatus() else state.value.share
        state.update { current ->
            current.copy(
                requests = current.requests.filterNot { it.id == requestId },
                share = share,
                message = if (allow) "Shared with Claude for 30 minutes." else "Claude's request was declined.",
            )
        }
    }

    fun stopSharing() = launchAction("stop") {
        claude.api.forgetShare()
        state.update { it.copy(share = null, message = "The shared copy is deleted.") }
    }

    fun getCode() = launchAction("code") {
        val code = claude.api.connectorCode()
        state.update { it.copy(code = code) }
    }

    fun dismissCode() = state.update { it.copy(code = null) }

    fun accept(proposal: Proposal) = launchAction("proposal") {
        claude.inbox.accept(proposal)
        state.update { current -> current.copy(proposals = current.proposals - proposal, message = "Added \"${proposal.title}\".") }
    }

    fun reject(proposal: Proposal) = launchAction("proposal") {
        claude.inbox.reject(proposal)
        state.update { current -> current.copy(proposals = current.proposals - proposal, message = "Rejected \"${proposal.title}\".") }
    }

    fun removeClaude() = launchAction("remove") {
        val removed = claude.removeClaude()
        state.update {
            it.copy(message = if (removed == 0) "Claude was not connected." else "Claude can no longer read Ember. Remove the connector in Claude too.")
        }
    }

    fun consumeMessage() = state.update { it.copy(message = null) }

    private fun launchAction(action: String, block: suspend () -> Unit) {
        if (state.value.busy != null) return
        state.update { it.copy(busy = action, error = null) }
        viewModelScope.launch {
            try {
                block()
            } catch (e: Exception) {
                state.update { it.copy(error = e.message ?: "Something went wrong.") }
            } finally {
                state.update { it.copy(busy = null) }
            }
        }
    }

    companion object {
        fun factory(claude: ClaudeConnector): ViewModelProvider.Factory = viewModelFactory {
            initializer { ClaudeSettingsViewModel(claude) }
        }
    }
}
