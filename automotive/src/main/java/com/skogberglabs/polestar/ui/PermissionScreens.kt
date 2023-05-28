package com.skogberglabs.polestar.ui

import android.Manifest
import android.content.Intent
import android.provider.Settings
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ParkedOnlyOnClickListener
import androidx.car.app.model.Template
import com.skogberglabs.polestar.CarListener
import com.skogberglabs.polestar.action
import com.skogberglabs.polestar.location.isAllPermissionsGranted
import com.skogberglabs.polestar.location.notGrantedPermissions
import com.skogberglabs.polestar.messageTemplate
import timber.log.Timber

data class PermissionContent(val title: String, val message: String, val permissions: List<String>) {
    companion object {
        const val locationPermission = Manifest.permission.ACCESS_FINE_LOCATION
        const val backgroundPermission = Manifest.permission.ACCESS_BACKGROUND_LOCATION
        val allPermissions = CarListener.permissions + listOf(locationPermission, backgroundPermission)
        val allExceptBackgroundLocation = CarListener.permissions + listOf(locationPermission)
        val car = PermissionContent(
            "Grant access to car data",
            "This app needs access to car data (speed, battery level, and so on) in order to save it to your Car-Tracker account.",
            CarListener.permissions
        )
        val location = PermissionContent(
            "Grant access to location",
            "This app needs access to the car's location in order to save it to your Car-Tracker account.",
            listOf(locationPermission)
        )
        /**
         * Request separately https://developer.android.com/about/versions/11/privacy/location#request-background-location-separately
         */
        val background = PermissionContent(
            "Grant access to background location",
            "This app needs background location access.",
            listOf(backgroundPermission)
        )
        val allForeground = PermissionContent(
            "Grant app access to location and car",
            "This app needs access to location and car properties in order to save them to your Car-Tracker account.",
            allExceptBackgroundLocation
        )
    }
}

class RequestPermissionScreen(carContext: CarContext, val content: PermissionContent, private val onGranted: () -> Unit) :
    Screen(carContext) {
    companion object {
        fun permissionContent(notGranted: List<String>): PermissionContent =
            if (notGranted.any { p -> PermissionContent.car.permissions.contains(p) }) PermissionContent.car
            else if (notGranted.contains(PermissionContent.locationPermission)) PermissionContent.location
            else if (notGranted.contains(PermissionContent.backgroundPermission)) PermissionContent.background
            else PermissionContent.allForeground
    }

    override fun onGetTemplate(): Template {
        Screens.installProfileRootBackBehavior(this)
        val pocl = ParkedOnlyOnClickListener.create {
            carContext.requestPermissions(content.permissions) { grantedPermissions, rejectedPermissions ->
                if (grantedPermissions.isNotEmpty()) {
                    val str = grantedPermissions.joinToString(separator = ", ")
                    Timber.i("Granted permissions: $str.")
                    if (carContext.isAllPermissionsGranted()) onGranted()
                    else screenManager.push(RequestPermissionScreen(carContext, permissionContent(carContext.notGrantedPermissions()), onGranted))
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
