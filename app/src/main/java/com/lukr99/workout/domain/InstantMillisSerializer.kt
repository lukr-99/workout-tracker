package com.lukr99.workout.domain

import java.time.Instant
import java.time.OffsetDateTime
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

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
