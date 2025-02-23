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
import com.skogberglabs.polestar.AppService
import com.skogberglabs.polestar.R
import com.skogberglabs.polestar.action
import com.skogberglabs.polestar.signInTemplate
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber

class SignInScreen(
    carContext: CarContext,
    private val service: AppService,
) : Screen(carContext), LifecycleEventObserver {
    private var job: Job? = null

    init {
        lifecycle.addObserver(this)
    }

    override fun onStateChanged(
        source: LifecycleOwner,
        event: Lifecycle.Event,
    ) {
        when (event) {
            Lifecycle.Event.ON_START -> {
                job =
                    service.mainScope.launch {
                        service.appState.collect { state ->
                            when (state) {
                                is AppState.LoggedIn -> {
                                    Timber.i("Logged in, pushing home screen...")
                                    screenManager.push(HomeScreen(carContext, service))
                                }
                                else -> {
                                }
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

    override fun onGetTemplate(): Template {
        val state = service.state()
        val appName = carContext.getString(R.string.app_name)
        val title = state.carLang()?.profile?.auth?.ctaGoogle ?: appName
        val signInAction =
            action {
                setTitle(title)
                setOnClickListener(
                    ParkedOnlyOnClickListener.create {
                        val intent =
                            Intent(carContext, GoogleSignInActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                        carContext.startActivity(intent)
                    },
                )
            }
        val method = ProviderSignInMethod(signInAction)
        return signInTemplate(method) {
            setLoading(state is AppState.Loading)
            setTitle(title)
            state.carLang()?.let { lang ->
                val authLang = lang.profile.auth
                setInstructions(authLang.instructions)
                setAdditionalText(authLang.additionalText)
            }
        }
    }
}
