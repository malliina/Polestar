package com.skogberglabs.polestar

import android.content.Intent
import androidx.car.app.CarAppService
import androidx.car.app.Screen
import androidx.car.app.ScreenManager
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator

class PolestarCarAppService : CarAppService() {
    override fun createHostValidator(): HostValidator = HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    override fun onCreateSession(): Session {
        val app = application as PolestarApp
        return PolestarSession(app.locations, app.locationSource)
    }
}

@androidx.annotation.OptIn(androidx.car.app.annotations.ExperimentalCarApi::class)
class PolestarSession(
    private val locations: CarLocationManager,
    private val locationSource: LocationSource
) : Session() {
    override fun onCreateScreen(intent: Intent): Screen {
        return if (locations.isGranted()) {
            locations.startIfGranted()
            PlacesScreen(carContext, locationSource)
//            MapScreen(carContext)
        } else {
            val sm = carContext.getCarService(ScreenManager::class.java)
            sm.push(PlacesScreen(carContext, locationSource))
            RequestPermissionScreen(carContext, onGranted = {
                locations.startIfGranted()
                sm.pop()
            })
        }
    }
}
