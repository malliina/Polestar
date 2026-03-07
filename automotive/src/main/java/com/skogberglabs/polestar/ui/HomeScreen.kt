package com.skogberglabs.polestar.ui

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.CarIcon
import androidx.car.app.model.Template
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.skogberglabs.polestar.AppService
import com.skogberglabs.polestar.R
import com.skogberglabs.polestar.appendAction
import com.skogberglabs.polestar.installHeader
import com.skogberglabs.polestar.location.CarLocationService
import com.skogberglabs.polestar.location.isAllPermissionsGranted
import com.skogberglabs.polestar.location.notGrantedPermissions
import com.skogberglabs.polestar.messageTemplate
import com.skogberglabs.polestar.titledAction
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber

class HomeScreen(
    carContext: CarContext,
    private val service: AppService,
) : Screen(carContext), LifecycleEventObserver {
    private var job: Job? = null
    private var isFirstRender = true

    init {
        lifecycle.addObserver(this)
    }

    override fun onStateChanged(
        source: LifecycleOwner,
        event: Lifecycle.Event,
    ) {
        Timber.i("Event $event of ${javaClass.simpleName}")
        // Checks permissions on every start
        when (event) {
            Lifecycle.Event.ON_START -> {
                job =
                    service.mainScope.launch {
                        service.appState.collect { state ->
                            val stateStr =
                                when (state) {
                                    is AppState.Anon -> "anon"
                                    is AppState.Loading -> "loading"
                                    is AppState.LoggedIn -> state.user.email.value
                                }
                            val destination = checkDestination()
                            destination?.let { screen ->
                                screenManager.pushLogged(screen)
                            } ?: run {
                                if (!isFirstRender) {
                                    Timber.i("State updated to $stateStr, invalidating screen")
                                    invalidate()
                                }
                                isFirstRender = false
                            }
                        }
                    }
            }
            Lifecycle.Event.ON_STOP -> {
                job?.cancel()
                job = null
            }
            else -> {}
        }
    }

    // Screen preference: 1) permissions, 2) sign in, 3) map 4) home
    private fun checkDestination(): Screen? {
        if (carContext.isAllPermissionsGranted()) {
            when (val state = service.state()) {
                is AppState.LoggedIn -> {
                    if (service.navigateToPlaces) {
                        if (state.user.activeCar != null) {
                            service.initialNavigation()
                            Timber.i("Navigating to map...")
                            return PlacesScreen(carContext, service, state.lang)
                        }
                    }
                }
                is AppState.Anon -> {
                    Timber.i("Navigating to sign in...")
                    return SignInScreen(carContext, service)
                }
                else -> {}
            }
        } else {
            service.state().carLang()?.let { lang ->
                val content =
                    RequestPermissionScreen.permissionContent(
                        carContext.notGrantedPermissions(),
                        lang.permissions,
                    )
                val permissionsScreen =
                    RequestPermissionScreen(carContext, content, lang.permissions) { sm ->
                        val nlang = lang.notifications
                        val serviceIntent = CarLocationService.intent(carContext, nlang.appRunning, nlang.enjoy)
                        carContext.startForegroundService(serviceIntent)
                        sm.pushLogged(HomeScreen(carContext, service))
                    }
                return permissionsScreen
            }
        }
        return null
    }

    override fun onGetTemplate(): Template {
        when (val state = service.state()) {
            is AppState.LoggedIn -> {
                val lang = state.lang
                val user = state.user
                val plang = lang.profile
                val message =
                    user.activeCar?.let { car ->
                        "${plang.driving} ${car.name}. ${plang.cloudInstructions}"
                    } ?: "${plang.signedInAs} ${user.email}."
                return messageTemplate(message) {
                    installHeader {
                        setTitle(lang.appName)
                        addEndHeaderAction(
                            titledAction(lang.settings.title) {
                                setOnClickListener {
                                    screenManager.pushLogged(SettingsScreen(carContext, lang, service))
                                }
                            }
                        )
                    }
                    user.activeCar?.let {
                        setIcon(CarIcon.APP_ICON)
                    } ?: run {
                        appendAction(lang.settings.selectCar) {
                            setOnClickListener {
                                screenManager.pushLogged(SelectCarScreen(carContext, lang, service))
                            }
                        }
                    }
                    appendAction(lang.profile.goToMap) {
                        setOnClickListener {
                            screenManager.pushLogged(PlacesScreen(carContext, service, lang))
                        }
                    }
                }
            }
            is AppState.Loading -> {
                val appName = state.lang?.appName ?: carContext.getString(R.string.app_name)
                return messageTemplate(appName) {
                    setLoading(true)
                }
            }
            is AppState.Anon -> {
                val appName = state.lang.appName
                return messageTemplate(appName) {
                    setIcon(CarIcon.APP_ICON)
                }
            }
        }
    }
}
