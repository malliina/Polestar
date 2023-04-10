package com.skogberglabs.polestar

import android.content.Intent
import androidx.car.app.CarAppService
import androidx.car.app.Screen
import androidx.car.app.ScreenManager
import androidx.car.app.Session
import androidx.car.app.annotations.ExperimentalCarApi
import androidx.car.app.validation.HostValidator

class CarTrackerAppService : CarAppService() {
    override fun createHostValidator(): HostValidator = HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    override fun onCreateSession(): Session {
        val app = application as CarTrackerApp
        return CarTrackerSession(app.locationSource)
    }
}

@androidx.annotation.OptIn(ExperimentalCarApi::class)
class CarTrackerSession(
    private val locationSource: LocationSource
) : Session() {
    override fun onCreateScreen(intent: Intent): Screen {
        return if (!carContext.isLocationGranted()) {
            RequestPermissionScreen(carContext, PermissionContent.location, onGranted = {
                carContext.startForegroundService(Intent(carContext, CarLocationService::class.java))
                goToProfile()
            })
        } else if (!carContext.isCarPermissionGranted()) {
            RequestPermissionScreen(carContext, PermissionContent.car, onGranted = {
                goToProfile()
            })
        } else {
            PlacesScreen(carContext, locationSource)
        }
    }

    private fun goToProfile() {
        val i = Intent(carContext, ProfileActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        carContext.startActivity(i)
    }
}
