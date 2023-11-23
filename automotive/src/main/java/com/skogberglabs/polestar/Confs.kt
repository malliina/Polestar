package com.skogberglabs.polestar

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
    val nothingHere: String
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
    val outsideTemperature: String
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
    val all: PermissionContentLang
)

@JsonClass(generateAdapter = true)
data class CarSettingsLang(val title: String, val openSettings: String, val selectCar: String, val noCars: String, val tracks: String, val noTracks: String)

@JsonClass(generateAdapter = true)
data class CarLanguage(val code: String, val name: String)

@JsonClass(generateAdapter = true)
data class NotificationLang(val appRunning: String, val enjoy: String, val grantPermissions: String, val autoStart: String, val startTracking: String)

@JsonClass(generateAdapter = true)
data class CarLang(
    val appName: String,
    val language: CarLanguage,
    val profile: CarProfileLang,
    val settings: CarSettingsLang,
    val permissions: PermissionsLang,
    val stats: CarStatsLang,
    val notifications: NotificationLang
)

@JsonClass(generateAdapter = true)
data class CarConf(val languages: List<CarLang>)

@JsonClass(generateAdapter = true)
data class TrackTime(val dateTime: String)

@JsonClass(generateAdapter = true)
data class Times(val start: TrackTime, val end: TrackTime)

@JsonClass(generateAdapter = true)
data class Coord(val lat: Double, val lng: Double)

@JsonClass(generateAdapter = true)
data class TopPoint(val coord: Coord)

@JsonClass(generateAdapter = true)
data class Track(val trackName: String, val boatName: String, val distanceMeters: Distance, val topPoint: TopPoint, val times: Times)

@JsonClass(generateAdapter = true)
data class Tracks(val tracks: List<Track>)