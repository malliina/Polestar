package com.skogberglabs.polestar

import android.app.Application
import android.content.Intent
import timber.log.Timber

class PolestarApp : Application() {
    private lateinit var prefs: LocalDataSource
    val preferences: LocalDataSource get() = prefs
    private lateinit var httpClient: CarHttpClient
    val http: CarHttpClient get() = httpClient
    private lateinit var googleClient: Google
    val google: Google get() = googleClient
    private lateinit var deviceLocationSource: LocationSource
    val locationSource: LocationSource get() = deviceLocationSource
    private lateinit var locationUploader: LocationUploader
    val uploader: LocationUploader get() = locationUploader
    val userState = UserState.instance

    override fun onCreate() {
        super.onCreate()
        val tree = if (BuildConfig.DEBUG) Timber.DebugTree() else NoLogging()
        Timber.plant(tree)
        Timber.i("Launching app.")
        prefs = LocalDataSource(applicationContext)
        googleClient = Google.build(applicationContext, userState)
        httpClient = CarHttpClient(GoogleTokenSource(googleClient))
        deviceLocationSource = LocationSource.instance
        locationUploader = LocationUploader(http, userState, preferences, deviceLocationSource)
        startForegroundService(Intent(applicationContext, CarLocationService::class.java))
    }
}

class NoLogging : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
    }
}
