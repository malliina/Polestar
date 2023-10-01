package com.skogberglabs.polestar.ui

import android.content.Intent
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ParkedOnlyOnClickListener
import androidx.car.app.model.Template
import androidx.car.app.model.signin.ProviderSignInMethod
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.skogberglabs.polestar.CarLang
import com.skogberglabs.polestar.UserState
import com.skogberglabs.polestar.action
import com.skogberglabs.polestar.signInTemplate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

class GoogleSignInScreen(carContext: CarContext,
                         private val lang: CarLang,
                         private val userState: UserState,
                         mainScope: CoroutineScope): Screen(carContext), LifecycleEventObserver {
    init {
        mainScope.launch {
            userState.userResult.collect { user ->
                if (user.isSuccess()) {
                    screenManager.pop()
                }
            }
        }
        lifecycle.addObserver(this)
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        if (event == Lifecycle.Event.ON_START) {
            Timber.i("Google started")
        }
    }

    override fun onGetTemplate(): Template {
        val signInAction = action {
            setTitle("${lang.profile.signInWith} Google")
            setOnClickListener(ParkedOnlyOnClickListener.create {
                val intent = Intent(carContext, GoogleSignInActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                carContext.startActivity(intent)
            })
        }
        val method = ProviderSignInMethod(signInAction)
        return signInTemplate(method) {
            setTitle("${lang.profile.signInWith} Google")
//            setInstructions("Sign in to store your rides in the cloud.")
//            setAdditionalText("Your information will not be shared with third parties.")
            setHeaderAction(Action.BACK)
        }
    }
}
