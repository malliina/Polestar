package com.skogberglabs.polestar.ui

import android.content.Intent
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.CarIcon
import androidx.car.app.model.Template
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.skogberglabs.polestar.action
import com.skogberglabs.polestar.actionStrip
import com.skogberglabs.polestar.location.CarLocationService
import com.skogberglabs.polestar.location.isAllPermissionsGranted
import com.skogberglabs.polestar.location.notGrantedPermissions
import com.skogberglabs.polestar.messageTemplate
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import timber.log.Timber

class AllGoodScreen(carContext: CarContext,
                    private val service: AppService
): Screen(carContext), LifecycleEventObserver {
    private var isLoading = true
    init {
        service.mainScope.launch {
            service.userState.userResult.combine(service.currentLang) { a, b -> Pair(a, b) }
//                .filter { p -> p.first.isSuccess() && p.second.isSuccess() }
                .collect {
                    isLoading = false
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
        // Message cannot be empty
        val msg = service.langLatest()?.appName ?: "Car-Tracker"
        return messageTemplate(msg) {
            if (isLoading) {
                setLoading(true)
            } else {
                setIcon(CarIcon.APP_ICON)
                service.langLatest()?.let { lang ->
                    userState.latest()?.let { user ->
                        Timber.i("Got ${user.email}.")
                            setTitle(lang.appName)
                            setActionStrip(actionStrip { addAction(action {
                                setTitle(lang.settings.title)
                                setOnClickListener {
                                    Timber.i("Open settings...")
                                    screenManager.push(SettingsScreen(carContext, lang, service))
                                }
                            }) })
                        }?: run {
                        addAction(action {
                            setTitle("${lang.profile.signInWith} Google")
                            setOnClickListener {
                                screenManager.push(GoogleSignInScreen(carContext, lang, userState, service.mainScope))
                            }
                        })
                    }
                }
            }
        }
    }
}
