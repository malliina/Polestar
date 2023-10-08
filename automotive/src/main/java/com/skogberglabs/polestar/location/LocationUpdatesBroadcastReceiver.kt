package com.skogberglabs.polestar.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationResult
import com.skogberglabs.polestar.LocalDataSource
import com.skogberglabs.polestar.LocationUpdate
import com.skogberglabs.polestar.Utils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId

class LocationUpdatesBroadcastReceiver : BroadcastReceiver() {
    private val locs = LocationSource.instance

    companion object {
        val ACTION_LOCATIONS = Utils.appAction("LOCATIONS")
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_LOCATIONS) {
            val prefs = runBlocking { LocalDataSource(context).userPreferencesFlow().first() }
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
                    locs.availability(locationAvailability.isLocationAvailable)
                    if (locationAvailability.isLocationAvailable) {
                        Timber.i("Location services are available.")
                    } else {
                        Timber.i("Location services are not available.")
                    }
                }
            } ?: run {
                Timber.w("Got intent ${intent.action}, but unable to extract location data.")
            }
        }
    }
}
