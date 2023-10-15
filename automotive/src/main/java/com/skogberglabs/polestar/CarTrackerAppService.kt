package com.skogberglabs.polestar

import android.content.Intent
import androidx.car.app.CarAppService
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.annotations.ExperimentalCarApi
import androidx.car.app.validation.HostValidator
import com.skogberglabs.polestar.ui.HomeScreen

class CarTrackerAppService : CarAppService() {
    override fun createHostValidator(): HostValidator = HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    override fun onCreateSession(): Session {
        val app = application as CarApp
        return CarSession(app)
    }
}

@androidx.annotation.OptIn(ExperimentalCarApi::class)
class CarSession(
    val app: CarApp
) : Session() {
    override fun onCreateScreen(intent: Intent): Screen =
        HomeScreen(carContext, app.appService)
}
