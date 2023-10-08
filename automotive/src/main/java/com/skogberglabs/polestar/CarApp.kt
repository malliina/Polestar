package com.skogberglabs.polestar

import android.app.Application
import com.skogberglabs.polestar.location.CarLocationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import timber.log.Timber

class CarApp : Application() {
    private val ioScope = CoroutineScope(Dispatchers.IO)
    private val mainScope = CoroutineScope(Dispatchers.Main)

    lateinit var appService: AppService

    override fun onCreate() {
        super.onCreate()
        val tree = if (BuildConfig.DEBUG) Timber.DebugTree() else NoLogging()
        Timber.plant(tree)
        Timber.i("Launching app.")
        CarLocationService.createNotificationChannels(applicationContext)
        appService = AppService(applicationContext, mainScope, ioScope)
        appService.onCreate()
    }
}

class NoLogging : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
    }
}
