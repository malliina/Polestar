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
import com.skogberglabs.polestar.CarNavGraph
import com.skogberglabs.polestar.Google
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class CarActivity : ComponentActivity() {
    private val requestCodeSignIn = 100
    private val profile: CarViewModel by viewModels()
    private val scope = CoroutineScope(Dispatchers.IO)

    private val google: Google get() = profile.google

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scope.launch {
            google.signInSilently()
            profile.prepare()
        }
        Timber.i("Creating activity...")
        setContent {
            AppTheme {
                val navController = rememberNavController()
                CarNavGraph(profile, onSignIn = { signIn() }, navController)
            }
        }
    }

    private fun signIn() {
        Timber.i("Signing in...")
        startActivityForResult(google.startSignIn(), requestCodeSignIn)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Timber.i("Got activity result of request $requestCode. Result code $resultCode.")
        if (requestCode == requestCodeSignIn) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                google.handleSignIn(account, silent = false)
            } catch (e: ApiException) {
                Timber.w(e, "Failed to handle sign in.")
                google.fail(e)
            }
        }
    }
}