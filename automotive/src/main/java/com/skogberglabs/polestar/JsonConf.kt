package com.skogberglabs.polestar

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.ClassDiscriminatorMode
import kotlinx.serialization.json.Json
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

object JsonConf {
    @OptIn(ExperimentalSerializationApi::class)
    val instance =
        Json {
            explicitNulls = false
            ignoreUnknownKeys = true
            encodeDefaults = true
            classDiscriminatorMode = ClassDiscriminatorMode.NONE
        }

    fun <T> decode(
        string: String,
        reader: KSerializer<T>,
    ): T =
        try {
            instance.decodeFromString(reader, string)
        } catch (e: Exception) {
            throw JsonException("Failed to decode JSON.", e)
        }

    fun <T> encode(
        t: T,
        writer: KSerializer<T>,
    ): String = instance.encodeToString(writer, t)

    inline fun <reified T> encode(t: T): String = instance.encodeToString(t)
}

typealias StringDateTime =
    @Serializable(OffsetDateTimeSerializer::class)
    OffsetDateTime

class OffsetDateTimeSerializer : KSerializer<OffsetDateTime> {
    private val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor(OffsetDateTime::class.simpleName!!, PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): OffsetDateTime = OffsetDateTime.parse(decoder.decodeString(), formatter)

    override fun serialize(
        encoder: Encoder,
        value: OffsetDateTime,
    ) {
        encoder.encodeString(formatter.format(value))
    }
}

class JsonException(message: String, cause: Exception) : Exception(message, cause)
