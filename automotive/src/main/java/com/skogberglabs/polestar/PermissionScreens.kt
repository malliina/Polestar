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

data class PermissionContent(val title: String, val message: String, val permissions: List<String>) {
    companion object {
        val car = PermissionContent(
            "Grant app access to car",
            "This app needs access to car properties in order to store them to your Car-Tracker account.",
            CarListener.permissions
        )
        val location = PermissionContent(
            "Grant location access",
            "This app needs access to location in order to track the car.",
            listOf(Manifest.permission.ACCESS_FINE_LOCATION)
        )
    }
}

class RequestPermissionScreen(carContext: CarContext, val content: PermissionContent, private val onGranted: () -> Unit) :
    Screen(carContext) {
    override fun onGetTemplate(): Template {
        Screens.installProfileRootBackBehavior(this)
        val pocl = ParkedOnlyOnClickListener.create {
            carContext.requestPermissions(content.permissions) { grantedPermissions, rejectedPermissions ->
                if (grantedPermissions.isNotEmpty()) {
                    val str = grantedPermissions.joinToString(separator = ", ")
                    Timber.i("Granted permissions: $str.")
                    onGranted()
                } else {
                    val str = rejectedPermissions.joinToString(separator = ", ")
                    Timber.i("Rejected permissions: $str.")
                    screenManager.push(NoPermissionScreen(carContext, content))
                }
            }
        }

        val myAction = action {
            setTitle(content.title)
            setOnClickListener(pocl)
        }
        return messageTemplate(content.message) {
            addAction(myAction)
            setHeaderAction(Action.BACK)
        }
    }
}

class NoPermissionScreen(carContext: CarContext, val content: PermissionContent) : Screen(carContext) {
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
                screenManager.push(RequestPermissionScreen(carContext, content, onGranted = { screenManager.popToRoot() }))
            }
        }
        return messageTemplate("Please open Settings and grant app-level permissions for this app.") {
            addAction(openSettingsAction)
            addAction(tryAgainAction)
            setHeaderAction(Action.BACK)
        }
    }
}
