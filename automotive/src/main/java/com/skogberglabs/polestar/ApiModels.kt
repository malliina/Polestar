package com.skogberglabs.polestar

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CarInfo(val id: String, val name: String, val addedMillis: Long)

@JsonClass(generateAdapter = true)
data class ApiUserInfo(val email: Email, val boats: List<CarInfo>)

@JsonClass(generateAdapter = true)
data class UserContainer(val user: ApiUserInfo)
