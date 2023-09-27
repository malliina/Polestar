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
import com.skogberglabs.polestar.ui.GoogleSignInScreen
import timber.log.Timber

class CarTrackerAppService : CarAppService() {
    override fun createHostValidator(): HostValidator = HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    override fun onCreateSession(): Session {
        val app = application as CarApp
        return CarSession(app, app.locationSource, app.confState)
    }
}

@androidx.annotation.OptIn(ExperimentalCarApi::class)
class CarSession(
    private val app: CarApp,
    private val locationSource: LocationSource,
    private val confState: ConfState
) : Session() {
    override fun onCreateScreen(intent: Intent): Screen {
        Timber.i("Create screen")
        return if (carContext.isAllPermissionsGranted()) {
            GoogleSignInScreen(carContext, app.google)
//            AllGoodScreen(carContext, confState, app.mainScope)
        } else {
            val content = RequestPermissionScreen.permissionContent(carContext.notGrantedPermissions())
            RequestPermissionScreen(carContext, content) { sm ->
                carContext.startForegroundService(Intent(carContext, CarLocationService::class.java))
                sm.push(AllGoodScreen(carContext, confState, app.mainScope))
            }
        }
    }
}
