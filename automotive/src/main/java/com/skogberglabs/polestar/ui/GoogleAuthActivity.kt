package com.skogberglabs.polestar.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import com.skogberglabs.polestar.AppService
import com.skogberglabs.polestar.CarApp
import kotlinx.coroutines.launch

class GoogleAuthActivity : ComponentActivity() {
    private lateinit var service: AppService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as CarApp
        service = app.appService
    }

    override fun onStart() {
        super.onStart()
        val ctx = this
        service.mainScope.launch {
//            service.googleAuth.startSignIn(ctx)
            finish()
        }
    }
}
