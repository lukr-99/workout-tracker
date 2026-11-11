package com.lukr99.workout.ui.android

import com.lukr99.workout.data.claude.ClaudeLink
import com.lukr99.workout.data.claude.ConnectorCode
import com.lukr99.workout.data.claude.Proposal
import com.lukr99.workout.data.claude.ShareMode
import com.lukr99.workout.data.claude.ShareRequest
import com.lukr99.workout.data.claude.ShareStatus

/** What Settings, Claude shows. [busy] names the action that is running. */
data class ClaudeSettingsState(
    val link: ClaudeLink? = null,
    val shareMode: ShareMode = ShareMode.Ask,
    val share: ShareStatus? = null,
    /** Claude's requests still open, for a phone that shows no notifications. */
    val requests: List<ShareRequest> = emptyList(),
    val proposals: List<Proposal> = emptyList(),
    /** False when this build or phone has no push, so Ember asks only when it is opened. */
    val pushOn: Boolean = true,
    val busy: String? = null,
    val error: String? = null,
    /** A one-line result to show as a toast, then cleared. */
    val message: String? = null,
    val connectOpen: Boolean = false,
    val code: ConnectorCode? = null,
)
