package com.skogberglabs.polestar.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.skogberglabs.polestar.AppTheme
import com.skogberglabs.polestar.CarApp
import com.skogberglabs.polestar.CarNavGraph
import com.skogberglabs.polestar.Google
import timber.log.Timber

class GoogleSignInActivity : ComponentActivity() {
    private val requestCodeSignIn = 100
    private lateinit var google: Google

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.i("Creating Google sign in activity...")
        val app = application as CarApp
        google = app.appService.google
        startActivityForResult(google.startSignIn(), requestCodeSignIn)
    }

    @Deprecated("Higher up")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Timber.i("Got activity result of request $requestCode. Result code $resultCode.")
        if (requestCode == requestCodeSignIn) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                Timber.i("Sign in complete, ${account.email ?: "no email"}")
                google.handleSignIn(account, silent = false)
                finish()
            } catch (e: ApiException) {
                Timber.w(e, "Failed to handle sign in.")
                google.fail(e)
            }
        }
    }
}