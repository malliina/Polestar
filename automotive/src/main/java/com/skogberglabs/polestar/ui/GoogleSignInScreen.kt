package com.skogberglabs.polestar.ui

import android.content.Intent
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.ParkedOnlyOnClickListener
import androidx.car.app.model.Template
import androidx.car.app.model.signin.ProviderSignInMethod
import com.skogberglabs.polestar.Google
import com.skogberglabs.polestar.action
import com.skogberglabs.polestar.signInTemplate
import timber.log.Timber

class GoogleSignInScreen(carContext: CarContext, val google: Google): Screen(carContext) {
    override fun onGetTemplate(): Template {
        val signInAction = action {
            setTitle("Sign in with Google")
            setOnClickListener(ParkedOnlyOnClickListener.create {
                Timber.i("Hej")
                val intent = google.startSignIn().apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                carContext.startActivity(intent)
            })
        }
        val method = ProviderSignInMethod(signInAction)
        return signInTemplate(method) {
            setTitle("Sign in")
            setInstructions("Log in with your Google account.")
            setAdditionalText("More info here.")
        }
    }
}
