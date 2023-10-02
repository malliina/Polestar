package com.skogberglabs.polestar

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AuthLang(val ctaGoogle: String, val instructions: String, val additionalText: String)

@JsonClass(generateAdapter = true)
data class CarProfileLang(
    val signedInAs: String,
    val driving: String,
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
data class PermissionsLang(
    val grantCta: String,
    val grantAccess: String,
    val explanation: String,
    val tryAgain: String,
    val openSettingsText: String
)
@JsonClass(generateAdapter = true)
data class CarSettingsLang(val title: String, val openSettings: String, val selectCar: String, val noCars: String)
@JsonClass(generateAdapter = true)
data class CarLanguage(val code: String, val name: String)
@JsonClass(generateAdapter = true)
data class CarLang(
    val appName: String,
    val language: CarLanguage,
    val profile: CarProfileLang,
    val settings: CarSettingsLang,
    val permissions: PermissionsLang,
    val stats: CarStatsLang
)
@JsonClass(generateAdapter = true)
data class CarConf(val languages: List<CarLang>)
