package com.skogberglabs.polestar

import kotlinx.serialization.Serializable

@Serializable
data class CarInfo(val id: Int, val name: String, val token: String, val addedMillis: Long) {
    val idStr: String get() = "$id"
}

@Serializable
data class ApiUserInfo(val email: Email, val boats: List<CarInfo>)

@Serializable
data class UserContainer(val user: ApiUserInfo)
