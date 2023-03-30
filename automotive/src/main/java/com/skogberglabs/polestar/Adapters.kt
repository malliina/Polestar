package com.skogberglabs.polestar

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.adapter

@OptIn(ExperimentalStdlibApi::class)
object Adapters {
    private val moshi: Moshi = Moshi.Builder()
        .add(PrimitiveAdapter())
        .build()

    val errors: JsonAdapter<Errors> = moshi.adapter()
}

class PrimitiveAdapter {
    @FromJson
    fun id(s: String): IdToken = IdToken(s)
    @ToJson
    fun writeId(s: IdToken): String = s.token
}

fun <T> JsonAdapter<T>.read(json: String): T {
    return this.fromJson(json)
        ?: throw JsonDataException("Moshi returned null when reading '$json'.")
}
