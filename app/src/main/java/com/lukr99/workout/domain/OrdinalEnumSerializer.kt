package com.lukr99.workout.domain

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

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
