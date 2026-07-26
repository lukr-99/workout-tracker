package com.lukr99.workout.domain

import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

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

/** Encodes an epoch-millis `Long` as an ISO-8601 UTC string; tolerant on read (offset or `Z`). */
object InstantMillisSerializer : KSerializer<Long> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("InstantMillis", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Long) {
        encoder.encodeString(Instant.ofEpochMilli(value).toString())
    }

    override fun deserialize(decoder: Decoder): Long {
        val raw = decoder.decodeString().trim()
        if (raw.isEmpty()) return 0L
        // Fast path: plain UTC instant ("...Z"). Fallbacks cover offsets and bare millis.
        return runCatching { Instant.parse(raw).toEpochMilli() }
            .recoverCatching { OffsetDateTime.parse(raw).toInstant().toEpochMilli() }
            .getOrElse { raw.toLongOrNull() ?: 0L }
    }
}

/** Base for enums that must serialize as their Int ordinal (the MAUI wire value). */
abstract class OrdinalEnumSerializer<T : Enum<T>>(
    name: String,
    private val values: Array<T>,
) : KSerializer<T> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(name, PrimitiveKind.INT)
    override fun serialize(encoder: Encoder, value: T) = encoder.encodeInt(value.ordinal)
    override fun deserialize(decoder: Decoder): T {
        val ordinal = decoder.decodeInt()
        return values.getOrNull(ordinal)
            ?: throw IllegalArgumentException("Unknown ${descriptor.serialName} ordinal $ordinal")
    }
}

/** `yyyy-MM-dd` (UTC) for display/notes — e.g. the "Created from workout on …" template note. */
fun formatIsoDate(millis: Long): String =
    DateTimeFormatter.ISO_LOCAL_DATE.format(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC))

object ExerciseCategorySerializer :
    OrdinalEnumSerializer<ExerciseCategory>("ExerciseCategory", ExerciseCategory.entries.toTypedArray())

object ExerciseSourceSerializer :
    OrdinalEnumSerializer<ExerciseSource>("ExerciseSource", ExerciseSource.entries.toTypedArray())

object WorkoutSessionStatusSerializer :
    OrdinalEnumSerializer<WorkoutSessionStatus>("WorkoutSessionStatus", WorkoutSessionStatus.entries.toTypedArray())

object WorkoutSessionSourceSerializer :
    OrdinalEnumSerializer<WorkoutSessionSource>("WorkoutSessionSource", WorkoutSessionSource.entries.toTypedArray())

object SetTypeSerializer :
    OrdinalEnumSerializer<SetType>("SetType", SetType.entries.toTypedArray())
