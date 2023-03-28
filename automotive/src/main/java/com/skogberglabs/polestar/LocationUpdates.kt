package com.skogberglabs.polestar

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit

// Inspiration from https://github.com/android/location-samples/blob/main/LocationUpdatesBackgroundKotlin/app/src/main/java/com/google/android/gms/location/sample/locationupdatesbackgroundkotlin/data/MyLocationManager.kt
data class LocationUpdate(
    val longitude: Double,
    val latitude: Double?,
    val altitudeMeters: Double?,
    val accuracyMeters: Float?,
    val bearing: Float?,
    val bearingAccuracyDegrees: Float?,
    val date: Date
)

class CarLocationManager(private val context: Context) {
    private val client = LocationServices.getFusedLocationProviderClient(context)
    private val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000).setMinUpdateIntervalMillis(2000).build()
    private val pendingIntent: PendingIntent by lazy {
        val intent = Intent(context, LocationUpdatesBroadcastReceiver::class.java).apply {
            action = LocationUpdatesBroadcastReceiver.ACTION_LOCATIONS
        }
        PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
    }

    @SuppressLint("MissingPermission")
    fun start() {
        client.requestLocationUpdates(request, pendingIntent)
        Timber.i("Requested location updates.")
    }

    fun stop() {
        client.removeLocationUpdates(pendingIntent)
    }
}

class LocationUpdatesBroadcastReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_LOCATIONS = "com.skogberglabs.polestar.action.locations"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Timber.i("Received intent ${intent.action}.")
        if (intent.action == ACTION_LOCATIONS) {
            LocationAvailability.extractLocationAvailability(intent)?.let { locationAvailability ->
                if (!locationAvailability.isLocationAvailable) {
                    Timber.i("Location services are not available.")
                } else {
                    Timber.i("Location services are available.")
                }
            }
            Timber.i("Extras ${intent.extras}")
            LocationResult.extractResult(intent)?.let { result ->
                Timber.i("Received ${result.locations.size} locations.")
                val updates = result.locations.map { loc ->
                    Timber.i("Got ${loc.latitude}, ${loc.longitude}")
                    LocationUpdate(
                        loc.longitude,
                        loc.latitude,
                        if (loc.hasAltitude()) loc.altitude else null,
                        if (loc.hasAccuracy()) loc.accuracy else null,
                        if (loc.hasBearing()) loc.bearing else null,
                        if (loc.hasBearingAccuracy()) loc.bearingAccuracyDegrees else null,
                        Date(loc.time)
                    )
                }
                if (updates.isNotEmpty()) {

                }
            } ?: run {
                Timber.i("LocationResult.extractResult returned null.")
            }
        }
    }
}

