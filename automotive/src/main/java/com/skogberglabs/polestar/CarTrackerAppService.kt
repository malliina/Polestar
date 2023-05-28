package com.skogberglabs.polestar

import android.content.Intent
import androidx.car.app.CarAppService
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.annotations.ExperimentalCarApi
import androidx.car.app.validation.HostValidator
import com.skogberglabs.polestar.location.CarLocationService
import com.skogberglabs.polestar.location.LocationSource
import com.skogberglabs.polestar.location.isAllPermissionsGranted
import com.skogberglabs.polestar.location.notGrantedPermissions
import com.skogberglabs.polestar.ui.CarActivity
import com.skogberglabs.polestar.ui.PlacesScreen
import com.skogberglabs.polestar.ui.RequestPermissionScreen

class CarTrackerAppService : CarAppService() {
    override fun createHostValidator(): HostValidator = HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    override fun onCreateSession(): Session {
        val app = application as CarApp
        return CarSession(app.locationSource)
    }
}

@androidx.annotation.OptIn(ExperimentalCarApi::class)
class CarSession(
    private val locationSource: LocationSource
) : Session() {
    override fun onCreateScreen(intent: Intent): Screen {
        return if (carContext.isAllPermissionsGranted()) {
            PlacesScreen(carContext, locationSource)
        } else {
            val content = RequestPermissionScreen.permissionContent(carContext.notGrantedPermissions())
            RequestPermissionScreen(carContext, content) {
                carContext.startForegroundService(Intent(carContext, CarLocationService::class.java))
                goToProfile()
            }
        }
    }

    private fun goToProfile() {
        val i = Intent(carContext, CarActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        carContext.startActivity(i)
    }
}
