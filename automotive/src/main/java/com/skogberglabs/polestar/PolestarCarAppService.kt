package com.skogberglabs.polestar

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.Intent
import android.content.pm.PackageManager
import androidx.car.app.AppManager
import androidx.car.app.CarAppService
import androidx.car.app.Screen
import androidx.car.app.ScreenManager
import androidx.car.app.Session
import androidx.car.app.SurfaceCallback
import androidx.car.app.SurfaceContainer
import androidx.car.app.validation.HostValidator
import timber.log.Timber

class PolestarCarAppService : CarAppService() {
    override fun createHostValidator(): HostValidator = HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    override fun onCreateSession(): Session = PolestarSession()
}

class NoLogging : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
    }
}

@androidx.annotation.OptIn(androidx.car.app.annotations.ExperimentalCarApi::class)
class PolestarSession : Session() {
    override fun onCreateScreen(intent: Intent): Screen {
        val tree = if (BuildConfig.DEBUG) Timber.DebugTree() else NoLogging()
        Timber.plant(tree)
        Timber.i("Starting!")
        val isGranted =
            carContext.checkSelfPermission(ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return if (isGranted) {
//            carContext.getCarService(AppManager::class.java).setSurfaceCallback(object:
//                SurfaceCallback {
//                override fun onScroll(distanceX: Float, distanceY: Float) {
//                    Timber.i("onScroll")
//                }
//                override fun  onClick(x: Float, y: Float) {
//                    Timber.i("Click x: $x y: $y")
//                    super.onClick(x, y)
//                }
//                override fun onSurfaceAvailable(surfaceContainer: SurfaceContainer) {
//                    Timber.i("Surface available")
//                }
//            })
            PlacesScreen(carContext)
        } else {
            val sm = carContext.getCarService(ScreenManager::class.java)
            sm.push(PlacesScreen(carContext))
            RequestPermissionScreen(carContext, onGranted = { sm.pop() })
        }
    }
}
