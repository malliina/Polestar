package com.skogberglabs.polestar

import android.app.Application
import android.content.Intent
import timber.log.Timber

class CarTrackerApp : Application() {
    private lateinit var carListener: CarListener
    val carInfo: CarListener get() = carListener
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
        carListener = CarListener(applicationContext)
        prefs = LocalDataSource(applicationContext)
        googleClient = Google.build(applicationContext, userState)
        httpClient = CarHttpClient(GoogleTokenSource(googleClient))
        CarLocationService.createNotificationChannels(applicationContext)
        deviceLocationSource = LocationSource.instance
        locationUploader = LocationUploader(http, userState, preferences, deviceLocationSource, carListener)
        startForegroundService(Intent(applicationContext, CarLocationService::class.java))
        carListener.connect()
    }
}

class NoLogging : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
    }
}
