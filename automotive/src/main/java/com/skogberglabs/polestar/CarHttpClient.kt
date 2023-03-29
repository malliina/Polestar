package com.skogberglabs.polestar

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.internal.closeQuietly
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resumeWithException

class CarHttpClient {
    companion object {
        private const val Accept = "Accept"
        private const val Authorization = "Authorization"
        private val MediaTypeJson = "application/vnd.car.v1+json".toMediaType()

        fun headers(token: IdToken?): Map<String, String> {
            val acceptPair = Accept to MediaTypeJson.toString()
            return if (token != null) mapOf(Authorization to "Bearer $token", acceptPair) else mapOf(acceptPair)
        }
    }
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .callTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun <T> get(path: String, adapter: JsonAdapter<T>): T {
        val request = authRequest(Env.baseUrl.append(path)).get().build()
        return execute(request, adapter)
    }

    private suspend fun <T> execute(request: Request, reader: JsonAdapter<T>): T =
        withContext(Dispatchers.IO) {
            make(client.newCall(request)).use { response ->
                val body = response.body
                if (response.isSuccessful) {
                    body?.let { b ->
                        reader.fromJson(b.source()) ?: throw JsonDataException("Moshi returned null for response body from '${request.url}'.")
                    } ?: run {
                        throw BodyException(request)
                    }
                } else {
                    val errors = body?.let { b ->
                        try { Adapters.errors.read(b.string()) } catch (e: Exception) { null }
                    }
                    errors?.let { throw ErrorsException(it, response.code, request) } ?: run {
                        throw StatusException(response.code, request)
                    }
                }
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun make(call: Call): Response = suspendCancellableCoroutine { cont ->
        val callback = object : Callback {
            override fun onResponse(call: Call, response: Response) {
                cont.resume(response) {
                    // If we have a response but we're cancelled while resuming, we need to
                    // close() the unused response
                    if (response.body != null) {
                        response.closeQuietly()
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                cont.resumeWithException(e)
            }
        }
        call.enqueue(callback)
        cont.invokeOnCancellation {
            try {
                call.cancel()
            } catch (t: Throwable) {
                // Ignore cancel exception
            }
        }
    }

    private suspend fun authRequest(url: FullUrl) =
        newRequest(url, headers(null))

    private fun newRequest(url: FullUrl, headers: Map<String, String>): Request.Builder {
        val builder = Request.Builder().url(url.url)
        for ((k, v) in headers) {
            builder.header(k, v)
        }
        return builder
    }
}
