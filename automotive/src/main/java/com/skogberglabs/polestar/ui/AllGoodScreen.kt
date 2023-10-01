package com.skogberglabs.polestar.ui

import android.content.Intent
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.CarIcon
import androidx.car.app.model.Template
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.skogberglabs.polestar.ConfState
import com.skogberglabs.polestar.Google
import com.skogberglabs.polestar.UserState
import com.skogberglabs.polestar.action
import com.skogberglabs.polestar.actionStrip
import com.skogberglabs.polestar.location.CarLocationService
import com.skogberglabs.polestar.location.isAllPermissionsGranted
import com.skogberglabs.polestar.location.notGrantedPermissions
import com.skogberglabs.polestar.messageTemplate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

class AllGoodScreen(carContext: CarContext,
                    private val service: AppService
): Screen(carContext), LifecycleEventObserver {
    init {
        service.mainScope.launch {
            service.userState.userResult.collect { _ ->
                invalidate()
            }
        }
        lifecycle.addObserver(this)
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        // Checks permissions on every start
        if (event == Lifecycle.Event.ON_START) {
            Timber.i("Event $event")
            if (!carContext.isAllPermissionsGranted()) {
                val content = RequestPermissionScreen.permissionContent(carContext.notGrantedPermissions())
                val permissionsScreen = RequestPermissionScreen(carContext, content) { sm ->
                    carContext.startForegroundService(Intent(carContext, CarLocationService::class.java))
                    sm.push(AllGoodScreen(carContext, service))
                }
                screenManager.push(permissionsScreen)
            }
        }
    }

    override fun onGetTemplate(): Template {
        val userState = service.userState
        val settingsAction = action {
            setTitle("Settings")
            setOnClickListener {
                Timber.i("Open settings...")
                screenManager.push(SettingsScreen(carContext, service))
            }
        }
        return messageTemplate(if (userState.latest() != null) "Drive safely!" else "Welcome") {
            setIcon(CarIcon.APP_ICON)
            setTitle("Car-Tracker")
            userState.latest()?.let { user ->
                Timber.i("Got ${user.email}.")
                setActionStrip(actionStrip { addAction(settingsAction) })
            } ?: run {
                addAction(action {
                    setTitle("Sign in to continue")
                    setOnClickListener {
                        screenManager.push(GoogleSignInScreen(carContext, userState, service.mainScope))
                    }
                })
            }
        }
    }
}
