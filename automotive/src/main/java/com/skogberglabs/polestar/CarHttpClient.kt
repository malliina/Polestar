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
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.internal.closeQuietly
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resumeWithException

interface TokenSource {
    suspend fun fetchToken(): IdToken?

    companion object {
        val empty = object : TokenSource {
            override suspend fun fetchToken(): IdToken? = null
        }
    }
}

class GoogleTokenSource(private val google: Google) : TokenSource {
    override suspend fun fetchToken(): IdToken? = try {
        google.signInSilently()?.idToken
    } catch (e: Exception) {
        Timber.w(e, "Failed to fetch token")
        null
    }
}

class CarHttpClient(private val tokenSource: TokenSource, private val env: EnvConf = EnvConf.current) {
    companion object {
        private const val Accept = "Accept"
        private const val Authorization = "Authorization"
        private val MediaTypeJson = "application/vnd.car.v1+json".toMediaType()
//        private val MediaTypeJson = "application/vnd.boat.v2+json".toMediaType()

        // OkHttp automatically advertises gzip compression, but Azure returns HTTP 502 for failed
        // POST requests where gzip support is advertised. This disables it as a workaround.
        val postPutHeaders = mapOf("Accept-Encoding" to "identity")

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

    private var token: IdToken? = null
    private suspend fun fetchToken(): IdToken? =
        token ?: run {
            val t = tokenSource.fetchToken()
            token = t
            t
        }

    suspend fun <T> get(path: String, adapter: JsonAdapter<T>): T {
        val request = authRequest(env.baseUrl.append(path)).get().build()
        return execute(request, adapter)
    }

    suspend fun <Req, Res> post(
        path: String,
        body: Req,
        writer: JsonAdapter<Req>,
        reader: JsonAdapter<Res>
    ): Res = body(path, body, writer, reader) { req, rb -> req.post(rb) }

    suspend fun <Req, Res> body(
        path: String,
        body: Req,
        writer: JsonAdapter<Req>,
        reader: JsonAdapter<Res>,
        install: (Request.Builder, RequestBody) -> Request.Builder
    ): Res = withContext(Dispatchers.IO) {
        val url = env.baseUrl.append(path)
        val requestBody = writer.toJson(body).toRequestBody(MediaTypeJson)
        val builder = installHeaders(postPutHeaders, authRequest(url))
        execute(install(builder, requestBody).build(), reader)
    }

    private suspend fun <T> execute(request: Request, reader: JsonAdapter<T>): T =
        try {
            executeOnce(request, reader)
        } catch (e: ErrorsException) {
            if (e.isTokenExpired) {
                Timber.i("JWT is expired. Obtaining a new token and retrying...")
                val newToken = tokenSource.fetchToken()
                if (newToken != null) {
                    token = newToken
                    val newAttempt =
                        request.newBuilder().header(Authorization, "Bearer $newToken").build()
                    executeOnce(newAttempt, reader)
                } else {
                    Timber.w("Token expired and unable to renew token. Failing request ${request.method} ${request.url}.")
                    throw e
                }
            } else {
                throw e
            }
        }

    private suspend fun <T> executeOnce(request: Request, reader: JsonAdapter<T>): T =
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
                        try {
                            val str = b.string()
                            Timber.w("Request ${request.method} ${request.url} errored with body $str")
                            Adapters.errors.read(str)
                        } catch (e: Exception) { null }
                    }
                    errors?.let {
                        Timber.w("Throwing error.")
                        throw ErrorsException(it, response.code, request)
                    } ?: run {
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
        newRequest(url, headers(fetchToken()))
    private fun newRequest(url: FullUrl, headers: Map<String, String>): Request.Builder {
        val builder = Request.Builder().url(url.url)
        return installHeaders(headers, builder)
    }
    private fun installHeaders(headers: Map<String, String>, builder: Request.Builder): Request.Builder {
        for ((k, v) in headers) {
            builder.header(k, v)
        }
        return builder
    }
}
