package com.skogberglabs.polestar

import android.app.Application
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import timber.log.Timber

class PolestarApp : Application() {
    private lateinit var httpClient: CarHttpClient
    val http: CarHttpClient get() = httpClient
    private lateinit var locationManager: CarLocationManager
    val locations: CarLocationManager get() = locationManager
    private lateinit var client: GoogleSignInClient
    val google: GoogleSignInClient get() = client
    private lateinit var deviceLocationSource: LocationSource
    val locationSource: LocationSource get() = deviceLocationSource
    private lateinit var uploader: LocationUploader

    override fun onCreate() {
        super.onCreate()
        val tree = if (BuildConfig.DEBUG) Timber.DebugTree() else NoLogging()
        Timber.plant(tree)
        Timber.i("Launching app.")
        locationManager = CarLocationManager(applicationContext)
        httpClient = CarHttpClient(GoogleTokenSource(applicationContext))
        client = Google.instance.client(applicationContext)
        deviceLocationSource = LocationSource.instance
        uploader = LocationUploader(http)
    }
}

class NoLogging : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
    }
}
