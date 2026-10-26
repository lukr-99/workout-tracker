package com.lukr99.workout.data.export

import com.lukr99.workout.data.document.JsonNumberRepair
import kotlinx.serialization.json.Json

/**
 * `ExportBundle` ⇄ JSON. The single place JSON settings for the bundle live.
 *
 * `ignoreUnknownKeys` lets a reader tolerate forward-added fields and the MAUI computed properties
 * (`bodyPartsSummary`, `isStrength`) present in `v1.0` files; `isLenient` is forgiving of minor
 * formatting. Writing is pretty-printed to match the human-readable MAUI export.
 */
object JsonExporter {

    val json: Json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = true
    }

    /**
     * The bundle as JSON. [JsonNumberRepair] fixes numbers Kotlin/Wasm 2.0 writes wrongly (`1e-7` as
     * `.e-7`), so a backup made in the web edition always reads back; on Android it changes nothing.
     */
    fun toJson(bundle: ExportBundle): String =
        JsonNumberRepair.repair(json.encodeToString(ExportBundle.serializer(), bundle))

    fun fromJson(text: String): ExportBundle = json.decodeFromString(ExportBundle.serializer(), text)
}
