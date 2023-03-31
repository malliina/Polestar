package com.skogberglabs.polestar

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.util.Date

// Inspiration from https://github.com/android/location-samples/blob/main/LocationUpdatesBackgroundKotlin/app/src/main/java/com/google/android/gms/location/sample/locationupdatesbackgroundkotlin/data/MyLocationManager.kt
data class LocationUpdate(
    val longitude: Double,
    val latitude: Double,
    val altitudeMeters: Double?,
    val accuracyMeters: Float?,
    val bearing: Float?,
    val bearingAccuracyDegrees: Float?,
    val date: Date
)

class LocationSource {
    companion object {
        val instance = LocationSource()
    }
    private val updatesState: MutableStateFlow<List<LocationUpdate>> = MutableStateFlow(emptyList())
    val locationUpdates: Flow<List<LocationUpdate>> = updatesState
    val currentLocation: Flow<LocationUpdate?> = locationUpdates.map { it.lastOrNull() }
    fun save(updates: List<LocationUpdate>): Boolean = updatesState.tryEmit(updates)
}

class CarLocationManager(private val context: Context) {
    private val client = LocationServices.getFusedLocationProviderClient(context)
    private val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
        .setMinUpdateIntervalMillis(1000).build()
    private val pendingIntent: PendingIntent by lazy {
        val intent = Intent(context, LocationUpdatesBroadcastReceiver::class.java).apply {
            action = LocationUpdatesBroadcastReceiver.ACTION_LOCATIONS
        }
        PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
    }

    fun isGranted(): Boolean =
        context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    fun startIfGranted() {
        if (isGranted()) {
            start()
        }
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
        if (intent.action == ACTION_LOCATIONS) {
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
                    LocationSource.instance.save(updates)
                }
            } ?: run {
                LocationAvailability.extractLocationAvailability(intent)?.let { locationAvailability ->
                    if (!locationAvailability.isLocationAvailable) {
                        Timber.i("Location services are not available.")
                    } else {
                        Timber.i("Location services are available.")
                    }
                }
            } ?: run {
                Timber.w("Got intent ${intent.action}, but unable to extract location data.")
            }
        }
    }
}
