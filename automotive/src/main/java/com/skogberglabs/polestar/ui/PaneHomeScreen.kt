package com.skogberglabs.polestar.ui

import android.graphics.Bitmap
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.CarIcon
import androidx.car.app.model.Template
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.skogberglabs.polestar.AppService
import com.skogberglabs.polestar.R
import com.skogberglabs.polestar.installAction
import com.skogberglabs.polestar.installHeader
import com.skogberglabs.polestar.installRow
import com.skogberglabs.polestar.location.CarLocationService
import com.skogberglabs.polestar.location.isAllPermissionsGranted
import com.skogberglabs.polestar.location.notGrantedPermissions
import com.skogberglabs.polestar.pane
import com.skogberglabs.polestar.paneTemplate
import com.skogberglabs.polestar.titledAction
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber

class PaneHomeScreen(
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
        Timber.i("Event $event")
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
                job?.let { Timber.i("Canceled job.") }
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
                        sm.pushLogged(PaneHomeScreen(carContext, service))
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
                val messages = user.activeCar?.let { car ->
                    listOf("${plang.driving} ${car.name}.", plang.cloudInstructions)
                } ?: listOf("${plang.signedInAs} ${user.email}.")
                return paneTemplate(
                    pane {
                        messages.forEach { message ->
                            installRow(message)
                        }
                        installAction(lang.profile.goToMap, isPrimary = true) {
                            screenManager.pushLogged(PlacesScreen(carContext, service, lang))
                        }
                        if (user.activeCar == null) {
                            installAction(lang.settings.selectCar, isPrimary = false) {
                                screenManager.pushLogged(SelectCarScreen(carContext, lang, service))
                            }
                        }
                        user.localCarImage?.let { icon ->
                            setImage(CarIcon.Builder(icon).build())
                        }
                    }
                ) {
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
                }
            }
            is AppState.Loading -> {
                return paneTemplate(pane {
                    setLoading(true)
                }) {
                }
            }
            is AppState.Anon -> {
                val appName = state.lang.appName
                return paneTemplate(pane { installRow(appName) }) {}
            }
        }
    }
}
