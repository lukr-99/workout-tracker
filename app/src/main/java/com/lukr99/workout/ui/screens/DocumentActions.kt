package com.lukr99.workout.ui.screens

import com.lukr99.workout.data.transfer.ExportArtifact

/**
 * What the Data screen can ask the platform to do with files: pick a file to import, save an
 * export where the person chooses, or share it. Android opens the system file picker and share
 * sheet; the web edition uses `<input type=file>`, a download and the Web Share API.
 */
class DocumentActions(
    /** Opens the file picker. The pick comes back through the callbacks of `rememberDocumentActions`. */
    val pickFile: () -> Unit,
    /** Asks where to save [ExportArtifact] and writes it there. */
    val saveFile: (ExportArtifact) -> Unit,
    /** Opens the share sheet for the export, with the given title. */
    val shareFile: (ExportArtifact, String) -> Unit,
)
