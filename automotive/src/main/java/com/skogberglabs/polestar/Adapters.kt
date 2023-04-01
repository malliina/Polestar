package com.skogberglabs.polestar

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.adapter
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalStdlibApi::class)
object Adapters {
    private val moshi: Moshi = Moshi.Builder()
        .add(PrimitiveAdapter())
        .build()

    val errors: JsonAdapter<Errors> = moshi.adapter()
//    val locationUpdate: JsonAdapter<LocationUpdate> = moshi.adapter()
    val locationUpdates: JsonAdapter<LocationUpdates> = moshi.adapter()
    val message: JsonAdapter<SimpleMessage> = moshi.adapter()
}

class PrimitiveAdapter {
    @FromJson
    fun id(s: String): IdToken = IdToken(s)
    @ToJson
    fun writeId(s: IdToken): String = s.token
    @FromJson
    fun readDateTime(s: String): OffsetDateTime = OffsetDateTime.parse(s)
    @ToJson
    fun writeDateTime(s: OffsetDateTime): String = s.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
}

fun <T> JsonAdapter<T>.read(json: String): T {
    return this.fromJson(json)
        ?: throw JsonDataException("Moshi returned null when reading '$json'.")
}
