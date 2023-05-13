package com.skogberglabs.polestar

data class CarProfileLang(
    val signedInAs: String,
    val driving: String,
    val chooseLanguage: String,
    val signInWith: String,
    val signOut: String,
    val failedToLoadProfile: String,
    val failedToSignIn: String,
    val goToMap: String,
    val version: String,
    val nothingHere: String
)
data class CarStatsLang(
    val speed: String,
    val height: String,
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
data class PermissionsLang(
    val grantCta: String,
    val grantAccess: String,
    val explanation: String,
    val tryAgain: String,
    val openSettingsText: String
)
data class CarSettingsLang(val title: String, val openSettings: String, val selectCar: String, val noCars: String)
data class CarLang(
    val appName: String,
    val language: String,
    val profile: CarProfileLang,
    val settings: CarSettingsLang,
    val permissions: PermissionsLang,
    val stats: CarStatsLang
)
data class CarLanguages(val finnish: CarLang, val swedish: CarLang, val english: CarLang)
