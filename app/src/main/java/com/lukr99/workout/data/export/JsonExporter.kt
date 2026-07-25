package com.lukr99.workout.data.export

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

    fun toJson(bundle: ExportBundle): String = json.encodeToString(ExportBundle.serializer(), bundle)

    fun fromJson(text: String): ExportBundle = json.decodeFromString(ExportBundle.serializer(), text)
}
