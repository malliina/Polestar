package com.skogberglabs.polestar

interface Primitive {
    val value: String
}

data class Email(val email: String) : Primitive {
    override val value: String get() = email
    override fun toString(): String = email
}

data class IdToken(val token: String) : Primitive {
    override val value: String get() = token
    override fun toString(): String = token
}

data class UserInfo(val email: Email, val idToken: IdToken)

sealed class Outcome<out T> {
    data class Success<T>(val result: T) : Outcome<T>()
    data class Error(val e: Exception) : Outcome<Nothing>()
    object Loading : Outcome<Nothing>()
    object Idle : Outcome<Nothing>()
    fun <U>map(f: (T) -> U): Outcome<U> = when(this) {
        is Success -> Success(f(result))
        is Error -> Error(e)
        Idle -> Idle
        Loading -> Loading
    }
    fun toOption(): T? = when (this) {
        is Success -> this.result
        is Error -> null
        Idle -> null
        Loading -> null
    }
}
