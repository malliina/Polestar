package com.skogberglabs.polestar

import android.Manifest
import android.content.Intent
import android.provider.Settings
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ParkedOnlyOnClickListener
import androidx.car.app.model.Template
import timber.log.Timber

class RequestPermissionScreen(carContext: CarContext, private val onGranted: () -> Unit) :
    Screen(carContext) {
    override fun onGetTemplate(): Template {
        val pocl = ParkedOnlyOnClickListener.create {
            carContext.requestPermissions(
                listOf(Manifest.permission.ACCESS_FINE_LOCATION)
            ) { grantedPermissions, rejectedPermissions ->
                if (grantedPermissions.isNotEmpty()) {
                    onGranted()
                } else {
                    screenManager.push(NoPermissionScreen(carContext))
                }
            }
        }

        val myAction = action {
            setTitle("Grant location access")
            setOnClickListener(pocl)
        }
        return messageTemplate("This app needs access to location in order to track the car.") {
            addAction(myAction)
            setHeaderAction(Action.BACK)
        }
    }
}

class NoPermissionScreen(carContext: CarContext) : Screen(carContext) {
    override fun onGetTemplate(): Template {
        val openSettingsAction = action {
            setTitle("Open Settings")
            setOnClickListener {
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                carContext.startActivity(intent)
            }
        }
        val tryAgainAction = action {
            setTitle("Try again")
            setOnClickListener {
                Timber.i("Trying again...")
                screenManager.push(RequestPermissionScreen(carContext, onGranted = { screenManager.popToRoot() }))
            }
        }
        return messageTemplate("Please open Settings and grant app-level permissions for this app to use location.") {
            addAction(openSettingsAction)
            addAction(tryAgainAction)
            setHeaderAction(Action.BACK)
        }
    }
}
