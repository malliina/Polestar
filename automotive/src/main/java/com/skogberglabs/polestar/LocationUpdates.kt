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
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.Date

// Inspiration from https://github.com/android/location-samples/blob/main/LocationUpdatesBackgroundKotlin/app/src/main/java/com/google/android/gms/location/sample/locationupdatesbackgroundkotlin/data/MyLocationManager.kt
@JsonClass(generateAdapter = true)
data class LocationUpdate(
    val longitude: Double,
    val latitude: Double,
    val altitudeMeters: Double?,
    val accuracyMeters: Float?,
    val bearing: Float?,
    val bearingAccuracyDegrees: Float?,
    val date: OffsetDateTime
)

@JsonClass(generateAdapter = true)
data class LocationUpdates(val updates: List<LocationUpdate>)

class LocationSource {
    companion object {
        val instance = LocationSource()
    }
    private val scope = CoroutineScope(Dispatchers.IO)
    private val updatesState: MutableStateFlow<List<LocationUpdate>> = MutableStateFlow(emptyList())
    val locationUpdates = updatesState.shareIn(scope, SharingStarted.Lazily, 1)
    val currentLocation: Flow<LocationUpdate?> = locationUpdates.map { it.lastOrNull() }
    fun save(updates: List<LocationUpdate>): Boolean = updatesState.tryEmit(updates)
}

class LocationUploader(private val http: CarHttpClient) {
    private val io = CoroutineScope(Dispatchers.IO)
    val job = io.launch {
        LocationSource.instance.locationUpdates.collect { locs ->
            val json = Adapters.locationUpdates.toJson(LocationUpdates(locs))
            Timber.i("Post $json to /cars/locations")
            try {
                http.post("/cars/locations", LocationUpdates(locs), Adapters.locationUpdates, Adapters.message)
            } catch (e: Exception) {
                Timber.i(e, "POST location updates failed.")
            }
        }
    }
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
        val locs = LocationSource.instance
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
                        OffsetDateTime.ofInstant(Instant.ofEpochMilli(loc.time), ZoneId.systemDefault())
                    )
                }
                if (updates.isNotEmpty()) {
                    locs.save(updates)
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
