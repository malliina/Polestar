package com.skogberglabs.polestar

import android.app.Service
import android.content.Intent
import android.os.IBinder

class LocationService: Service() {
    override fun onCreate() {
        super.onCreate()
        // start location request here
    }
    override fun onBind(intent: Intent?): IBinder? = null
}