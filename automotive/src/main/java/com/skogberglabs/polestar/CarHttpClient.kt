package com.skogberglabs.polestar

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
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
        val empty =
            object : TokenSource {
                override suspend fun fetchToken(): IdToken? = null
            }
    }
}

class GoogleTokenSource(private val google: Google) : TokenSource {
    override suspend fun fetchToken(): IdToken? =
        try {
            google.signInSilently("token")?.idToken
        } catch (e: Exception) {
            Timber.w(e, "Failed to fetch token")
            null
        }
}

class GoogleCredTokenSource(private val google: GoogleCredManager) : TokenSource {
    override suspend fun fetchToken(): IdToken? =
        try {
            Timber.w("Fetching a token without an activity is not supported.")
            null
        } catch (e: Exception) {
            Timber.w(e, "Failed to fetch token")
            null
        }
}

class CarHttpClient(private val tokenSource: TokenSource, private val env: EnvConf = EnvConf.current) {
    companion object {
        private const val Accept = "Accept"
        private const val Authorization = "Authorization"
        private const val CsrfToken = "Csrf-Token"
        private const val UserAgent = "User-Agent"
        private val MediaTypeJson = "application/vnd.car.v1+json".toMediaType()
//        private val MediaTypeJson = "application/vnd.boat.v2+json".toMediaType()

        val client: OkHttpClient =
            OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .callTimeout(60, TimeUnit.SECONDS)
                .build()

        fun headers(
            token: IdToken?,
            carToken: String?,
        ): Map<String, String> {
            val alwaysIncluded =
                mapOf(
                    Accept to MediaTypeJson.toString(),
                    CsrfToken to "nocheck",
                    UserAgent to "Car-Map/${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                )
            val idTokenAuth = if (token != null) mapOf(Authorization to "Bearer $token") else emptyMap()
            val carTokenAuth = if (carToken != null) mapOf("X-Token" to carToken) else emptyMap()
            return idTokenAuth + carTokenAuth + alwaysIncluded
        }
    }

    private var token: IdToken? = null

    private suspend fun fetchToken(): IdToken? =
        token ?: run {
            val t = tokenSource.fetchToken()
            token = t
            t
        }

    suspend inline fun <reified T> get(
        path: String,
        carToken: String?,
    ): T = get(path, carToken, serializer())

    suspend fun <T> get(
        path: String,
        carToken: String?,
        adapter: KSerializer<T>,
    ): T {
        val request = authRequest(env.baseUrl.append(path), carToken).get().build()
        Timber.i("Fetching '${request.url}'...")
        return execute(request, adapter)
    }

    suspend inline fun <reified Req, reified Res> post(
        path: String,
        body: Req,
        carToken: String?,
    ): Res = post(path, body, carToken, serializer(), serializer())

    suspend fun <Req, Res> post(
        path: String,
        body: Req,
        carToken: String?,
        writer: KSerializer<Req>,
        reader: KSerializer<Res>,
    ): Res = body(path, body, carToken, writer, reader) { req, rb -> req.post(rb) }

    private suspend fun <Req, Res> body(
        path: String,
        body: Req,
        carToken: String?,
        writer: KSerializer<Req>,
        reader: KSerializer<Res>,
        install: (Request.Builder, RequestBody) -> Request.Builder,
    ): Res =
        withContext(Dispatchers.IO) {
            val url = env.baseUrl.append(path)
            val requestBody = JsonConf.encode(body, writer).toRequestBody(MediaTypeJson)
            execute(install(authRequest(url, carToken), requestBody).build(), reader)
        }

    private suspend fun <T> execute(
        request: Request,
        reader: KSerializer<T>,
    ): T =
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
                    Timber.w(
                        "Token expired and unable to renew token. Failing request ${request.method} ${request.url}.",
                    )
                    throw e
                }
            } else {
                Timber.e(e, "Request to ${request.method} ${request.url} failed.")
                throw e
            }
        }

    private suspend fun <T> executeOnce(
        request: Request,
        reader: KSerializer<T>,
    ): T =
        withContext(Dispatchers.IO) {
            make(client.newCall(request)).use { response ->
                val body = response.body
                if (response.isSuccessful) {
                    JsonConf.decode(body.string(), reader)
                } else {
                    val errors = try {
                        val str = body.string()
                        Timber.w("Request ${request.method} ${request.url} errored with body $str")
                        JsonConf.decode(str, Errors.serializer())
                    } catch (e: Exception) {
                        null
                    }
                    if (errors != null) {
                        Timber.w("Throwing error.")
                        throw ErrorsException(errors, response.code, request)
                    }
                    throw StatusException(response.code, request)
                }
            }
        }

    private suspend fun make(call: Call): Response = call.await()

    private suspend fun authRequest(
        url: FullUrl,
        carToken: String?,
    ) = newRequest(url, headers(fetchToken(), carToken))

    private fun newRequest(
        url: FullUrl,
        headers: Map<String, String>,
    ): Request.Builder {
        val builder = Request.Builder().url(url.url)
        return installHeaders(headers, builder)
    }

    private fun installHeaders(
        headers: Map<String, String>,
        builder: Request.Builder,
    ): Request.Builder {
        for ((k, v) in headers) {
            builder.header(k, v)
        }
        return builder
    }
}

suspend fun Call.await(): Response =
    suspendCancellableCoroutine { cont ->
        val callback =
            object : Callback {
                override fun onResponse(
                    call: Call,
                    response: Response,
                ) {
                    cont.resume(response) { t, v, ctx ->
                        // If we have a response but we're cancelled while resuming, we need to
                        // close() the unused response
                        response.closeQuietly()
                    }
                }

                override fun onFailure(
                    call: Call,
                    e: IOException,
                ) {
                    cont.resumeWithException(e)
                }
            }
        enqueue(callback)
        cont.invokeOnCancellation {
            try {
                cancel()
            } catch (t: Throwable) {
                // Ignore cancel exception
            }
        }
    }