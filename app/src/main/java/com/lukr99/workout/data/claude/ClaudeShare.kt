package com.lukr99.workout.data.claude

import com.lukr99.workout.data.export.ExportBundle
import kotlinx.serialization.Serializable

/**
 * What the phone sends when it shares with Claude (docs/claude-connector.md): the backup's
 * [ExportBundle] without photos, settings or runs, and the runs apart, without their GPS trace but
 * with their splits. The Worker reads it in cloud/src/workouts/SharedBundle.ts; a change here bumps
 * [FORMAT] and needs the Worker to read the new format first.
 */
@Serializable
data class ClaudeShare(
    val shareFormat: Int = FORMAT,
    /** The phone's IANA time zone, so the Worker counts the owner's own days. */
    val timeZone: String,
    /** "metric" or "imperial", the owner's weight unit setting, so Claude answers in it. */
    val units: String,
    val appVersion: String?,
    val bundle: ExportBundle,
    val runs: List<SharedRun>,
) {
    companion object {
        const val FORMAT = 1
    }
}
