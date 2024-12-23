package com.skogberglabs.polestar.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.ScreenManager
import androidx.car.app.model.Action
import androidx.car.app.model.ParkedOnlyOnClickListener
import androidx.car.app.model.Template
import com.skogberglabs.polestar.CarListener
import com.skogberglabs.polestar.PermissionContentLang
import com.skogberglabs.polestar.PermissionsLang
import com.skogberglabs.polestar.action
import com.skogberglabs.polestar.location.isAllPermissionsGranted
import com.skogberglabs.polestar.location.notGrantedPermissions
import com.skogberglabs.polestar.messageTemplate
import timber.log.Timber

data class PermissionContent(val lang: PermissionContentLang, val permissions: List<String>) {
    val title = lang.title
    val message = lang.message

    companion object {
        const val locationPermission = Manifest.permission.ACCESS_FINE_LOCATION
        const val backgroundPermission = Manifest.permission.ACCESS_BACKGROUND_LOCATION
        private val allLegacyPermissions = CarListener.permissions + listOf(locationPermission, backgroundPermission)
        val allPermissions =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) allLegacyPermissions + listOf(Manifest.permission.FOREGROUND_SERVICE_LOCATION)
            else allLegacyPermissions
        private val allExceptBackgroundLocation = CarListener.permissions + listOf(locationPermission)

        fun car(lang: PermissionContentLang) = PermissionContent(lang, CarListener.permissions)

        fun location(lang: PermissionContentLang) = PermissionContent(lang, listOf(locationPermission))

        /**
         * Request separately https://developer.android.com/about/versions/11/privacy/location#request-background-location-separately
         */
        fun background(lang: PermissionContentLang) = PermissionContent(lang, listOf(backgroundPermission))

        fun allForeground(lang: PermissionContentLang) = PermissionContent(lang, allExceptBackgroundLocation)
    }
}

class RequestPermissionScreen(
    carContext: CarContext,
    val content: PermissionContent,
    val lang: PermissionsLang,
    private val onGranted: (ScreenManager) -> Unit,
) : Screen(carContext) {
    companion object {
        fun permissionContent(
            notGranted: List<String>,
            lang: PermissionsLang,
        ): PermissionContent =
            if (notGranted.any { p -> CarListener.permissions.contains(p) }) {
                PermissionContent.car(lang.car)
            } else if (notGranted.contains(PermissionContent.locationPermission)) {
                PermissionContent.location(lang.location)
            } else if (notGranted.contains(PermissionContent.backgroundPermission)) {
                PermissionContent.background(lang.background)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && notGranted.contains(Manifest.permission.FOREGROUND_SERVICE_LOCATION)) {
                PermissionContent(PermissionContentLang("Foreground service", "This app runs as a foreground service."), listOf(Manifest.permission.FOREGROUND_SERVICE_LOCATION))
            } else {
                PermissionContent.allForeground(lang.all)
            }
    }

    override fun onGetTemplate(): Template {
        Screens.installProfileRootBackBehavior(this)
        val pocl =
            ParkedOnlyOnClickListener.create {
                carContext.requestPermissions(content.permissions) { grantedPermissions, rejectedPermissions ->
                    if (grantedPermissions.isNotEmpty()) {
                        val str = grantedPermissions.joinToString(separator = ", ")
                        Timber.i("Granted permissions: $str.")
                        if (carContext.isAllPermissionsGranted()) {
                            onGranted(screenManager)
                        } else {
                            screenManager.push(
                                RequestPermissionScreen(
                                    carContext,
                                    permissionContent(carContext.notGrantedPermissions(), lang),
                                    lang,
                                    onGranted,
                                ),
                            )
                        }
                    } else {
                        val str = rejectedPermissions.joinToString(separator = ", ")
                        Timber.i("Rejected permissions: $str.")
                        screenManager.push(NoPermissionScreen(carContext, content, lang))
                    }
                }
            }

        val myAction =
            action {
                setTitle(content.title)
                setOnClickListener(pocl)
            }
        return messageTemplate(content.message) {
            addAction(myAction)
            setHeaderAction(Action.BACK)
        }
    }
}

class NoPermissionScreen(carContext: CarContext, val content: PermissionContent, val lang: PermissionsLang) : Screen(carContext) {
    override fun onGetTemplate(): Template {
        val openSettingsAction =
            action {
                setTitle("Open Settings")
                setOnClickListener {
                    val intent =
                        Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                    carContext.startActivity(intent)
                }
            }
        val tryAgainAction =
            action {
                setTitle("Try again")
                setOnClickListener {
                    Timber.i("Trying again...")
                    screenManager.push(RequestPermissionScreen(carContext, content, lang, onGranted = { screenManager.popToRoot() }))
                }
            }
        return messageTemplate("Please open Settings and grant app-level permissions for this app.") {
            addAction(openSettingsAction)
            addAction(tryAgainAction)
            setHeaderAction(Action.BACK)
        }
    }
}
