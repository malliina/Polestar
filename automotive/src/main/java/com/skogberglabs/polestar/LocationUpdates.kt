package com.skogberglabs.polestar

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationResult
import com.skogberglabs.polestar.Utils.appId
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import timber.log.Timber
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId

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
data class LocationUpdates(val updates: List<LocationUpdate>, val carId: String, val car: CarState)

interface LocationSourceInterface {
    val currentLocation: Flow<LocationUpdate?>
}

class LocationSource : LocationSourceInterface {
    companion object {
        val instance = LocationSource()
    }
    private val scope = CoroutineScope(Dispatchers.IO)
    private val locationServicesAvailability: MutableStateFlow<Boolean?> = MutableStateFlow(null)
    private val updatesState: MutableStateFlow<List<LocationUpdate>> = MutableStateFlow(emptyList())
    val locationUpdates: SharedFlow<List<LocationUpdate>> = updatesState.shareIn(scope, SharingStarted.Lazily, 1)
    override val currentLocation: Flow<LocationUpdate?> = locationUpdates.map { it.lastOrNull() }
    val locationServicesAvailable: Flow<Boolean?> = locationServicesAvailability.shareIn(scope, SharingStarted.Lazily, 1)

    fun save(updates: List<LocationUpdate>): Boolean = updatesState.tryEmit(updates)
    fun availability(isAvailable: Boolean) = locationServicesAvailability.tryEmit(isAvailable)
}

@OptIn(ExperimentalCoroutinesApi::class)
class LocationUploader(
    private val http: CarHttpClient,
    private val userState: UserState,
    private val prefs: LocalDataSource,
    private val locations: LocationSource,
    private val carListener: CarListener
) {
    private val io = CoroutineScope(Dispatchers.IO)
    val message = userState.userResult.filter { it != Outcome.Loading }.distinctUntilChanged().flatMapLatest { user ->
        when (user) {
            is Outcome.Success -> {
                Timber.i("Logged in as ${user.result.email}, sending locations...")
                sendLocations()
            }
            else -> flowOf(Outcome.Idle)
        }
    }.shareIn(io, SharingStarted.Eagerly, 1)

    private val carIds = prefs.userPreferencesFlow().map { it.carId }
    val a = carListener.carInfo.value
    private suspend fun sendLocations(): Flow<Outcome<SimpleMessage>> =
        locations.locationUpdates.combine(carIds.filterNotNull()) { locs, id ->
            val path = "/cars/locations"
            try {
                val result = http.post(
                    path,
                    LocationUpdates(locs, id, carListener.carInfo.value),
                    Adapters.locationUpdates,
                    Adapters.message
                )
                Outcome.Success(result)
            } catch (e: Exception) {
                Timber.w(e, "Failed to POST location updates to '$path'.")
                Outcome.Error(e)
            }
        }
}

class LocationUpdatesBroadcastReceiver : BroadcastReceiver() {
    companion object {
        val ACTION_LOCATIONS = appId("LOCATIONS")
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
                    locs.availability(locationAvailability.isLocationAvailable)
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
