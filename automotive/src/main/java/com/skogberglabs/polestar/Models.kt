package com.skogberglabs.polestar

import kotlinx.serialization.Serializable

@JvmInline
@Serializable
value class SpeedKnots(val knots: Double) {
    fun toSpeed() = Speed(knots.toFloat() * knotInKmh / meterPerSecondInKmh)

    companion object {
        const val knotInKmh: Float = 1.852f
        const val meterPerSecondInKmh: Float = 3.6f
    }
}

@Serializable
data class TrackTime(val dateTime: String)

@Serializable
data class Times(val start: TrackTime, val end: TrackTime)

@Serializable
data class TopPoint(val coord: Coord, val time: TrackTime, val speed: SpeedKnots) {
    val carSpeed: Speed get() = speed.toSpeed()
}

@Serializable
data class Track(
    val trackName: String,
    val boatName: String,
    val distanceMeters: Distance,
    val topPoint: TopPoint,
    val times: Times,
)

@Serializable
data class Tracks(val tracks: List<Track>)

@Serializable
data class NearestCoord(val coord: Coord, val distance: Distance, val address: String?)

@Serializable
data class ParkingDirections(val from: Coord, val to: List<Coord>, val nearest: NearestCoord, val capacity: Int)

@Serializable
data class ParkingResponse(val directions: List<ParkingDirections>)

// Inspiration from https://github.com/android/location-samples/blob/main/LocationUpdatesBackgroundKotlin/app/src/main/java/com/google/android/gms/location/sample/locationupdatesbackgroundkotlin/data/MyLocationManager.kt

@Serializable
data class LocationUpdate(
    val longitude: Double,
    val latitude: Double,
    val altitudeMeters: Double?,
    val accuracyMeters: Float?,
    val bearing: Float?,
    val bearingAccuracyDegrees: Float?,
    val date: StringDateTime,
) {
    val coord get(): Coord = Coord(latitude, longitude)

    val approx get(): String = coord.approx

    fun toPoint(car: CarState) =
        CarPoint(
            longitude, latitude, altitudeMeters,
            accuracyMeters, bearing, bearingAccuracyDegrees,
            car.speed, car.batteryLevel, car.batteryCapacity,
            car.rangeRemaining, car.outsideTemperature, car.nightMode, date,
        )
}

@Serializable
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
    val rangeRemaining: DistanceF?,
    val outsideTemperature: Temperature?,
    val nightMode: Boolean?,
    val date: StringDateTime,
)

@Serializable
data class LocationUpdates(val updates: List<CarPoint>, val carId: String)

interface Primitive {
    val value: String
}

@JvmInline
@Serializable
value class Email(val email: String) : Primitive {
    override val value: String get() = email

    override fun toString(): String = email
}

@JvmInline
@Serializable
value class IdToken(val token: String) : Primitive {
    override val value: String get() = token

    override fun toString(): String = token
}

data class UserInfo(val email: Email, val idToken: IdToken)

sealed class Outcome<out T> {
    data class Success<T>(val result: T) : Outcome<T>()

    data class Error(val e: Exception) : Outcome<Nothing>()

    data object Loading : Outcome<Nothing>()

    data object Idle : Outcome<Nothing>()

    fun <U> map(f: (T) -> U): Outcome<U> = flatMap { Success(f(it)) }

    fun <U> flatMap(f: (T) -> Outcome<U>): Outcome<U> =
        when (this) {
            is Success -> f(result)
            is Error -> Error(e)
            Idle -> Idle
            Loading -> Loading
        }

    fun toOption(): T? =
        when (this) {
            is Success -> this.result
            is Error -> null
            Idle -> null
            Loading -> null
        }

    fun isSuccess(): Boolean =
        when (this) {
            is Success -> true
            else -> false
        }
}
