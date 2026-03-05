package com.skogberglabs.polestar.logs

import android.util.Log
import com.skogberglabs.polestar.IdToken
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Instant
import java.time.format.DateTimeFormatter

@Serializable
data class TokenRequest(val app: String)

@Serializable
data class TokenResponse(val token: IdToken)

@Serializable
enum class LogLevel {
    @SerialName("debug")
    Debug,
    @SerialName("info")
    Info,
    @SerialName("warn")
    Warning,
    @SerialName("error")
    Error;

    companion object {
        fun fromTimber(p: Int): LogLevel = when (p) {
            Log.DEBUG -> Debug
            Log.INFO -> Info
            Log.WARN -> Warning
            Log.ERROR -> Error
            else -> Debug
        }
    }
}

object InstantSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("java.time.Instant", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: Instant) = encoder.encodeString(DateTimeFormatter.ISO_INSTANT.format(value))
    override fun deserialize(decoder: Decoder): Instant = Instant.parse(decoder.decodeString())
}

typealias KInstant = @Serializable(InstantSerializer::class) Instant

@Serializable
data class LogEvent(
    val timestamp: KInstant,
    val message: String,
    val loggerName: String,
    val threadName: String,
    val level: LogLevel,
    val stackTrace: String?
)

@Serializable
data class LogEvents(val events: List<LogEvent>)
@Serializable
data class Published(val eventCount: Int)

