package com.skogberglabs.polestar

import android.app.Application
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import timber.log.Timber

class PolestarApp : Application() {
    private lateinit var httpClient: CarHttpClient
    val http: CarHttpClient get() = httpClient
    private lateinit var locationManager: CarLocationManager
    val locations: CarLocationManager get() = locationManager
    private lateinit var googleClient: Google
    val google: Google get() = googleClient
    private lateinit var deviceLocationSource: LocationSource
    val locationSource: LocationSource get() = deviceLocationSource
    private lateinit var locationUploader: LocationUploader
    val uploader: LocationUploader get() = locationUploader

    override fun onCreate() {
        super.onCreate()
        val tree = if (BuildConfig.DEBUG) Timber.DebugTree() else NoLogging()
        Timber.plant(tree)
        Timber.i("Launching app.")
        locationManager = CarLocationManager(applicationContext)
        googleClient = Google.build(applicationContext)
        httpClient = CarHttpClient(GoogleTokenSource(googleClient))
        deviceLocationSource = LocationSource.instance
        locationUploader = LocationUploader(http)
    }
}

class NoLogging : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
    }
}
