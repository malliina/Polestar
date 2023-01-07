package com.skogberglabs.polestar

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.Intent
import android.content.pm.PackageManager
import androidx.car.app.CarAppService
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.ScreenManager
import androidx.car.app.Session
import androidx.car.app.model.Action
import androidx.car.app.model.CarLocation
import androidx.car.app.model.ParkedOnlyOnClickListener
import androidx.car.app.model.Template
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

class PolestarSession : Session() {
    override fun onCreateScreen(intent: Intent): Screen {
        val tree = if (BuildConfig.DEBUG) Timber.DebugTree() else NoLogging()
        Timber.plant(tree)
        Timber.i("Starting!")
        val isGranted =
            carContext.checkSelfPermission(ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return if (isGranted) {
            PlacesScreen(carContext)
        } else {
            val sm = carContext.getCarService(ScreenManager::class.java)
            sm.push(PlacesScreen(carContext))
            RequestPermissionScreen(carContext, onGranted = { sm.pop() })
        }
    }
}

class MapScreen(carContext: CarContext): Screen(carContext) {
    override fun onGetTemplate(): Template = navigationTemplate {

    }
}

class PlacesScreen(carContext: CarContext) : Screen(carContext) {
    override fun onGetTemplate(): Template {
        val myItems = itemList {
            addItem(
                row {
                    setTitle("Places")
                    setBrowsable(true)
                    setOnClickListener {
                        screenManager.push(NoPermissionScreen(carContext))
                    }
                }
            )
        }
        val myPlace = place(CarLocation.create(60.155, 24.877))
        return placeListMap {
            setHeaderAction(Action.APP_ICON)
            setItemList(myItems)
            setCurrentLocationEnabled(true)
            setAnchor(myPlace)
        }
    }
}

class RequestPermissionScreen(carContext: CarContext, private val onGranted: () -> Unit) :
    Screen(carContext) {
    override fun onGetTemplate(): Template {
        val pocl = ParkedOnlyOnClickListener.create {
            carContext.requestPermissions(
                listOf(ACCESS_FINE_LOCATION)
            ) { grantedPermissions, rejectedPermissions ->
                if (grantedPermissions.isNotEmpty()) {
                    onGranted()
                }
            }
        }

        val myAction = action {
            setTitle("Grant access.")
            setOnClickListener(pocl)
        }
        return messageTemplate("Location usage explanation.") {
            addAction(myAction)
            setHeaderAction(Action.APP_ICON)
        }
    }
}

class NoPermissionScreen(carContext: CarContext) : Screen(carContext) {
    override fun onGetTemplate(): Template {
        val row = row {
            setTitle("Please grant permission to use location.")
        }
        val pane = pane {
            addRow(row)
            addAction(
                action {
                    setTitle("Try again")
                    setOnClickListener {
                        Timber.i("Trying again...")
                        screenManager.push(RequestPermissionScreen(carContext, onGranted = { screenManager.popToRoot() }))
                    }
                }
            )
        }
        return paneTemplate(pane) {
            setHeaderAction(Action.APP_ICON)
        }
    }
}
