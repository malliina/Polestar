package com.skogberglabs.polestar.ui

import android.content.Intent
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.ParkedOnlyOnClickListener
import androidx.car.app.model.Template
import androidx.car.app.model.signin.ProviderSignInMethod
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.skogberglabs.polestar.CarLang
import com.skogberglabs.polestar.ProfileInfo
import com.skogberglabs.polestar.action
import com.skogberglabs.polestar.actionStrip
import com.skogberglabs.polestar.location.CarLocationService
import com.skogberglabs.polestar.location.isAllPermissionsGranted
import com.skogberglabs.polestar.location.notGrantedPermissions
import com.skogberglabs.polestar.messageTemplate
import com.skogberglabs.polestar.signInTemplate
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber

sealed class AppState {
    data class LoggedIn(val user: ProfileInfo, val lang: CarLang): AppState()
    data class Anon(val lang: CarLang): AppState()
    data object Loading: AppState()
}

class AllGoodScreen(carContext: CarContext,
                    private val service: AppService
): Screen(carContext), LifecycleEventObserver {
    private var isLoading = true
    private var job: Job? = null
    init {
        lifecycle.addObserver(this)
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        Timber.i("Event $event")
        // Checks permissions on every start
        when (event) {
            Lifecycle.Event.ON_START -> {
                if (carContext.isAllPermissionsGranted()) {
                    job = service.mainScope.launch {
                        service.appState.collect { state ->
                            isLoading = state != AppState.Loading
                            val stateStr = when (state) {
                                is AppState.Anon -> "anon"
                                AppState.Loading -> "loading"
                                is AppState.LoggedIn -> state.user.email.value
                            }
                            Timber.i("Invalidating AllGoodScreen, state $stateStr")
                            invalidate()
                        }
                    }
                } else {
                    val content = RequestPermissionScreen.permissionContent(carContext.notGrantedPermissions())
                    val permissionsScreen = RequestPermissionScreen(carContext, content) { sm ->
                        carContext.startForegroundService(Intent(carContext, CarLocationService::class.java))
                        sm.push(AllGoodScreen(carContext, service))
                    }
                    screenManager.push(permissionsScreen)
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

    override fun onGetTemplate(): Template {
        return when (val state = service.state()) {
            is AppState.LoggedIn -> {
                val lang = state.lang
                return messageTemplate(state.lang.appName) {
                    val user = state.user
                    Timber.i("Got ${user.email}.")
                    user.activeCar?.let { car ->
                        setTitle(lang.appName)
                    } ?: run {
                        addAction(action {
                            setTitle(lang.settings.selectCar)
                            setOnClickListener {
                                screenManager.push(SelectCarScreen(carContext, lang, service))
                            }
                        })
                    }
                    setActionStrip(actionStrip {
                        addAction(action {
                            setTitle(lang.settings.title)
                            setOnClickListener {
                                Timber.i("Open settings...")
                                screenManager.push(SettingsScreen(carContext, lang, service))
                            }
                        })
                    })
                }
            }
            is AppState.Anon -> {
                val lang = state.lang
                val authLang = lang.profile.auth
                val signInAction = action {
                    setTitle(authLang.ctaGoogle)
                    setOnClickListener(ParkedOnlyOnClickListener.create {
                        val intent = Intent(carContext, GoogleSignInActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        carContext.startActivity(intent)
                    })
                }
                val method = ProviderSignInMethod(signInAction)
                return signInTemplate(method) {
                    setTitle(authLang.ctaGoogle)
                    setInstructions(authLang.instructions)
                    setAdditionalText(authLang.additionalText)
                }
            }

            AppState.Loading -> messageTemplate("Car-Tracker") {
                setLoading(true)
            }
        }
    }
}
