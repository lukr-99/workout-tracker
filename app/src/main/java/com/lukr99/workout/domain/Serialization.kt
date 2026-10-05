package com.lukr99.workout.domain

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Serialization glue that keeps the JSON export bytes compatible with the MAUI `v1.0` export.
 *
 * Two shape decisions the MAUI `System.Text.Json` (Web defaults) writer made, reproduced here:
 * - **Enums are numbers** (their ordinal), not names.
 * - **Timestamps are ISO-8601 strings** (`DateTime` `"O"` round-trip format), while in-memory /
 *   Room we carry epoch-millis `Long`.
 *
 * These live in `domain/` (pure Kotlin, no Android imports) because the domain models are the
 * portable, `@Serializable` contract — the same seam a future desktop tool reads.
 */

/** `yyyy-MM-dd` (UTC) for display/notes — e.g. the "Created from workout on …" template note. */
fun formatIsoDate(millis: Long): String =
    DateTimeFormatter.ISO_LOCAL_DATE.format(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC))
