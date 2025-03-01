package com.skogberglabs.polestar

import android.content.Intent
import androidx.car.app.CarAppService
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator
import com.skogberglabs.polestar.ui.HomeScreen
import timber.log.Timber

class CarTrackerAppService : CarAppService() {
    override fun createHostValidator(): HostValidator = HostValidator.ALLOW_ALL_HOSTS_VALIDATOR

    override fun onCreateSession(): Session {
        val app = application as CarApp
        return CarSession(app)
    }
}

class CarSession(val app: CarApp) : Session() {
    override fun onCreateScreen(intent: Intent): Screen {
        intent.data?.let { uri ->
            Timber.i("Creating screen with $uri...")
        }
        return HomeScreen(carContext, app.appService)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.data?.let { uri ->
            Timber.i("Received intent with $uri...")
        }
    }
}
