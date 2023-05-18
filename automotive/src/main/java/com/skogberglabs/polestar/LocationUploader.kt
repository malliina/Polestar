package com.skogberglabs.polestar

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
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

@OptIn(ExperimentalCoroutinesApi::class)
class LocationUploader(
    private val http: CarHttpClient,
    private val userState: UserState,
    private val prefs: LocalDataSource,
    private val locations: LocationSource,
    private val carListener: CarListener
) {
    private val path = "/cars/locations"
    private val io = CoroutineScope(Dispatchers.IO)
    val message: SharedFlow<Outcome<SimpleMessage>> = userState.userResult.filter { it != Outcome.Loading }.distinctUntilChanged().flatMapLatest { user ->
        when (user) {
            is Outcome.Success -> {
                Timber.i("Logged in as ${user.result.email}, sending locations...")
                sendLocations()
            }
            else -> flowOf(Outcome.Idle)
        }
    }.shareIn(io, SharingStarted.Eagerly, replay = 1)

    private val carIds = prefs.userPreferencesFlow().map { it.carId }
    private suspend fun sendLocations(): Flow<Outcome<SimpleMessage>> =
        locations.locationUpdates
            .filter { it.isNotEmpty() }
            .combine(carIds.filterNotNull()) { locs, id ->
            try {
                val result = http.post(
                    path,
                    LocationUpdates(locs.map { it.toPoint(carListener.carInfo.value) }, id),
                    Adapters.locationUpdates,
                    Adapters.message
                )
                Timber.i("Uploaded ${locs.size} locations to $path.")
                Outcome.Success(result)
            } catch (e: Exception) {
                Timber.w(e, "Failed to POST location updates to $path.")
                Outcome.Error(e)
            }
        }
}
