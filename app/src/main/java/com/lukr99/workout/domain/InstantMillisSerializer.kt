package com.lukr99.workout.domain

import kotlinx.datetime.Instant
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
        encoder.encodeString(Instant.fromEpochMilliseconds(value).toString())
    }

    override fun deserialize(decoder: Decoder): Long {
        val raw = decoder.decodeString().trim()
        if (raw.isEmpty()) return 0L
        // An ISO-8601 instant with "Z" or an offset; the fallback covers bare millis.
        return runCatching { Instant.parse(raw).toEpochMilliseconds() }
            .getOrElse { raw.toLongOrNull() ?: 0L }
    }
}
