package com.skogberglabs.polestar

import android.Manifest
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
