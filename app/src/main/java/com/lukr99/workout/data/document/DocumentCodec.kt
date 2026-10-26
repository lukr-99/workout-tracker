package com.lukr99.workout.data.document

import com.lukr99.workout.data.export.ExportBundle
import kotlinx.serialization.json.Json

/**
 * [WorkoutDocument] to and from the stored text. Writing always stamps the current export format
 * and leaves photos out (they get their own store). Reading ignores unknown fields; the caller
 * checks [WorkoutDocument.isNewerThanThisBuild] before it saves anything back.
 */
object DocumentCodec {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = true
    }

    fun encode(document: WorkoutDocument): String = JsonNumberRepair.repair(
        json.encodeToString(
            WorkoutDocument.serializer(),
            document.copy(
                schema = WorkoutDocument.SCHEMA,
                data = document.data.copy(exportFormatVersion = ExportBundle.CURRENT_VERSION, photos = emptyList()),
            ),
        ),
    )

    /** Throws when [text] is not a readable document. */
    fun decode(text: String): WorkoutDocument = json.decodeFromString(WorkoutDocument.serializer(), text)
}
