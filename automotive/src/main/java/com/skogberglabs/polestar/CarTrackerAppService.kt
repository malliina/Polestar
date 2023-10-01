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
import com.skogberglabs.polestar.ui.AllGoodScreen
import com.skogberglabs.polestar.ui.RequestPermissionScreen

class CarTrackerAppService : CarAppService() {
    override fun createHostValidator(): HostValidator = HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    override fun onCreateSession(): Session {
        val app = application as CarApp
        return CarSession(app)
    }
}

@androidx.annotation.OptIn(ExperimentalCarApi::class)
class CarSession(
    app: CarApp,
) : Session() {
    private val service = app.appService
    override fun onCreateScreen(intent: Intent): Screen {
        return if (carContext.isAllPermissionsGranted()) {
            AllGoodScreen(carContext, service)
        } else {
            val content = RequestPermissionScreen.permissionContent(carContext.notGrantedPermissions())
            RequestPermissionScreen(carContext, content) { sm ->
                carContext.startForegroundService(Intent(carContext, CarLocationService::class.java))
                sm.push(AllGoodScreen(carContext, service))
            }
        }
    }
}
