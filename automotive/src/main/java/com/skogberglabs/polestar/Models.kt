package com.skogberglabs.polestar

import com.squareup.moshi.JsonClass
import java.time.OffsetDateTime

// Inspiration from https://github.com/android/location-samples/blob/main/LocationUpdatesBackgroundKotlin/app/src/main/java/com/google/android/gms/location/sample/locationupdatesbackgroundkotlin/data/MyLocationManager.kt

@JsonClass(generateAdapter = true)
data class LocationUpdate(
    val longitude: Double,
    val latitude: Double,
    val altitudeMeters: Double?,
    val accuracyMeters: Float?,
    val bearing: Float?,
    val bearingAccuracyDegrees: Float?,
    val date: OffsetDateTime
) {
    fun toPoint(car: CarState) = CarPoint(longitude, latitude, altitudeMeters, accuracyMeters, bearing, bearingAccuracyDegrees, car.speed, car.batteryLevel, car.batteryCapacity, car.rangeRemaining, car.outsideTemperature, car.nightMode, date)
}

@JsonClass(generateAdapter = true)
data class CarPoint(
    val longitude: Double,
    val latitude: Double,
    val altitudeMeters: Double?,
    val accuracyMeters: Float?,
    val bearing: Float?,
    val bearingAccuracyDegrees: Float?,
    val speed: Speed?,
    val batteryLevel: Energy?,
    val batteryCapacity: Energy?,
    val rangeRemaining: Distance?,
    val outsideTemperature: Temperature?,
    val nightMode: Boolean?,
    val date: OffsetDateTime
)

@JsonClass(generateAdapter = true)
data class LocationUpdates(val updates: List<CarPoint>, val carId: String)

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
    fun <U> map(f: (T) -> U): Outcome<U> = when (this) {
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
