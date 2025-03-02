package com.skogberglabs.polestar

import androidx.car.app.model.CarLocation
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AuthLang(val ctaGoogle: String, val instructions: String, val additionalText: String)

@JsonClass(generateAdapter = true)
data class CarProfileLang(
    val signedInAs: String,
    val driving: String,
    val cloudInstructions: String,
    val chooseLanguage: String,
    val auth: AuthLang,
    val signInWith: String,
    val signOut: String,
    val failedToLoadProfile: String,
    val failedToSignIn: String,
    val goToMap: String,
    val version: String,
    val nothingHere: String,
)

@JsonClass(generateAdapter = true)
data class CarStatsLang(
    val speed: String,
    val altitude: String,
    val nightMode: String,
    val dayMode: String,
    val bearing: String,
    val accuracy: String,
    val degrees: String,
    val meters: String,
    val batteryLevel: String,
    val capacity: String,
    val range: String,
    val outsideTemperature: String,
)

@JsonClass(generateAdapter = true)
data class PermissionContentLang(val title: String, val message: String)

@JsonClass(generateAdapter = true)
data class PermissionsLang(
    val grantCta: String,
    val grantAccess: String,
    val explanation: String,
    val tryAgain: String,
    val openSettingsText: String,
    val car: PermissionContentLang,
    val location: PermissionContentLang,
    val background: PermissionContentLang,
    val foreground: PermissionContentLang,
    val all: PermissionContentLang,
)

@JsonClass(generateAdapter = true)
data class CarSettingsLang(
    val title: String,
    val openSettings: String,
    val selectCar: String,
    val noCars: String,
    val tracks: String,
    val noTracks: String,
    val parking: String,
    val availableSpots: String,
    val noParkingAvailable: String,
    val navigate: String,
    val searchParkings: String,
    val searchParkingsHint: String,
    val failedToLoadParkings: String,
)

@JsonClass(generateAdapter = true)
data class CarLanguage(val code: String, val name: String)

@JsonClass(generateAdapter = true)
data class NotificationLang(
    val appRunning: String,
    val enjoy: String,
    val grantPermissions: String,
    val autoStart: String,
    val startTracking: String,
)

@JsonClass(generateAdapter = true)
data class CarLang(
    val appName: String,
    val language: CarLanguage,
    val profile: CarProfileLang,
    val settings: CarSettingsLang,
    val permissions: PermissionsLang,
    val stats: CarStatsLang,
    val notifications: NotificationLang,
)

@JsonClass(generateAdapter = true)
data class CarConf(val languages: List<CarLang>)

@JsonClass(generateAdapter = true)
data class Coord(val lat: Double, val lng: Double) {
    companion object {
        fun format(d: Double) {
            val trunc = (d * 100000).toInt().toDouble() / 100000
            "%1.5f".format(trunc).replace(',', '.')
        }

        fun location(carLocation: CarLocation) = Coord(carLocation.latitude, carLocation.longitude)
    }

    val approx get(): String = "${format(lat)},${format(lng)}"
}
