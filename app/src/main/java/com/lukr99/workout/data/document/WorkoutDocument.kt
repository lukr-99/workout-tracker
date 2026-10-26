package com.lukr99.workout.data.document

import com.lukr99.workout.data.export.ExportBundle
import kotlinx.serialization.Serializable

/**
 * Everything the web edition stores, as one document: the workout data in the [ExportBundle]
 * shape (without photos), so the browser copy is in the format docs/data-contract.md describes,
 * plus this device's own preferences and the dismissed "what's new" version.
 *
 * Runs and routes from an imported Android backup stay in [data] as they arrived. The web edition
 * does not show them, but a backup made on the web carries them back.
 *
 * The revision that guards against lost updates lives beside the document in the store
 * ([DocumentStore]), not inside it. Bump [SCHEMA] whenever this class gains a field, so an older
 * build in another tab stops saving instead of dropping what it does not know.
 */
@Serializable
data class WorkoutDocument(
    val schema: Int = SCHEMA,
    val data: ExportBundle = ExportBundle(exportedAtUtc = ""),
    val devicePrefs: DevicePrefsSnapshot = DevicePrefsSnapshot(),
    val seenVersion: Int? = null,
) {
    /** True when a newer build wrote this document: this build may not know all of its fields. */
    val isNewerThanThisBuild: Boolean
        get() = schema > SCHEMA || data.exportFormatVersion !in ExportBundle.SUPPORTED_VERSIONS

    companion object {
        const val SCHEMA = 1
    }
}
