package com.skogberglabs.polestar

import androidx.core.graphics.drawable.IconCompat
import kotlinx.serialization.Serializable

@Serializable
data class CarInfo(val id: Int, val name: String, val token: String, val addedMillis: Long) {
    val idStr: String get() = "$id"
}

@Serializable
data class ApiUserInfo(val email: Email, val boats: List<CarInfo>, val cars: List<Vehicle>)

@Serializable
data class UserContainer(val user: ApiUserInfo)

@Serializable
data class VehicleBattery(val chargeLevelPercentage: Int, val chargingStatus: String)
@Serializable
data class Vehicle(val vin: String, val registrationNumber: String, val studioImage: FullUrlJson)

data class UserData(val user: ApiUserInfo, val localCarImage: IconCompat?) {
    val cars = user.cars
}
