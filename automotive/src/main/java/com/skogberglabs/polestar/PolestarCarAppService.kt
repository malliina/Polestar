package com.skogberglabs.polestar

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.car.app.CarAppService
import androidx.car.app.Screen
import androidx.car.app.ScreenManager
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator

class PolestarCarAppService : CarAppService() {
    override fun createHostValidator(): HostValidator = HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    override fun onCreateSession(): Session {
        val app = application as PolestarApp
        return PolestarSession(app.locationSource)
    }
}

@androidx.annotation.OptIn(androidx.car.app.annotations.ExperimentalCarApi::class)
class PolestarSession(
    private val locationSource: LocationSource
) : Session() {
    override fun onCreateScreen(intent: Intent): Screen {
        return if (isGranted()) {
            PlacesScreen(carContext, locationSource)
        } else {
            val sm = carContext.getCarService(ScreenManager::class.java)
            sm.push(PlacesScreen(carContext, locationSource))
            RequestPermissionScreen(carContext, onGranted = {
                carContext.startForegroundService(Intent(carContext, CarLocationService::class.java))
                sm.pop()
            })
        }
    }
    private fun isGranted(): Boolean =
        carContext.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
}
