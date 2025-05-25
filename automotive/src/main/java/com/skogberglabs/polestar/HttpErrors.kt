package com.skogberglabs.polestar

import kotlinx.serialization.Serializable
import okhttp3.Request

open class HttpException(message: String, val request: Request) : Exception(message) {
    val httpUrl = request.url
    val url = FullUrl.build(request.url.toString())
}

class BodyException(request: Request) : HttpException("Invalid HTTP response body.", request)

open class StatusException(val code: Int, request: Request) :
    HttpException("Invalid status code $code from '${request.url}'.", request)

class ErrorsException(val errors: Errors, code: Int, request: Request) :
    StatusException(code, request) {
    val isTokenExpired: Boolean get() = errors.errors.any { e -> e.key == "token_expired" }
}

@Serializable
data class SimpleMessage(val message: String)

@Serializable
data class SingleError(val key: String, val message: String) {
    companion object {
        fun backend(message: String) = SingleError("backend", message)
    }
}

@Serializable
data class Errors(val errors: List<SingleError>) {
    companion object {
        fun input(message: String) = single("input", message)

        fun single(
            key: String,
            message: String,
        ): Errors = Errors(listOf(SingleError(key, message)))
    }
}
