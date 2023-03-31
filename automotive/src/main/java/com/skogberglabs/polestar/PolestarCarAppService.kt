package com.skogberglabs.polestar

import android.content.Intent
import androidx.car.app.CarAppService
import androidx.car.app.Screen
import androidx.car.app.ScreenManager
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator

class PolestarCarAppService : CarAppService() {
    override fun createHostValidator(): HostValidator = HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    override fun onCreateSession(): Session = PolestarSession()
}

@androidx.annotation.OptIn(androidx.car.app.annotations.ExperimentalCarApi::class)
class PolestarSession : Session() {
    private lateinit var locations: CarLocationManager
    override fun onCreateScreen(intent: Intent): Screen {
        locations = CarLocationManager(carContext)
        return if (locations.isGranted()) {
            locations.startIfGranted()
            PlacesScreen(carContext)
//            MapScreen(carContext)
        } else {
            val sm = carContext.getCarService(ScreenManager::class.java)
            sm.push(PlacesScreen(carContext))
            RequestPermissionScreen(carContext, onGranted = { sm.pop() })
        }
    }
}
