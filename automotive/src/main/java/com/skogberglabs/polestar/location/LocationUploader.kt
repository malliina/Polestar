package com.skogberglabs.polestar.location

import com.skogberglabs.polestar.CarHttpClient
import com.skogberglabs.polestar.CarListener
import com.skogberglabs.polestar.LocalDataSource
import com.skogberglabs.polestar.LocationUpdates
import com.skogberglabs.polestar.Outcome
import com.skogberglabs.polestar.SimpleMessage
import com.skogberglabs.polestar.UserState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import timber.log.Timber

@OptIn(ExperimentalCoroutinesApi::class)
class LocationUploader(
    private val http: CarHttpClient,
    userState: UserState,
    prefs: LocalDataSource,
    private val locations: LocationSource,
    private val carListener: CarListener,
    ioScope: CoroutineScope,
) {
    private val path = "/cars/locations"
    private val message: SharedFlow<Outcome<SimpleMessage>> =
        userState.userResult.filter { it != Outcome.Loading }.distinctUntilChanged().flatMapLatest { user ->
            when (user) {
                is Outcome.Success -> {
                    Timber.i("Logged in as ${user.result.email}, sending locations...")
                    sendLocations()
                }
                else -> {
                    flowOf(Outcome.Idle)
                }
            }
        }.shareIn(ioScope, SharingStarted.Eagerly, replay = 1)
    val status: StateFlow<Outcome<SimpleMessage>> = message.stateIn(ioScope, SharingStarted.Eagerly, Outcome.Loading)

    private val selectedCar = prefs.userPreferencesFlow().map { it.selectedCar }

    private fun sendLocations(): Flow<Outcome<SimpleMessage>> =
        locations.locationUpdates
            .filter { it.isNotEmpty() }
            .combine(selectedCar.filterNotNull()) { locs, car ->
                try {
                    val result =
                        http.post<LocationUpdates, SimpleMessage>(
                            path,
                            LocationUpdates(
                                locs.map {
                                    it.toPoint(carListener.carInfo.value)
                                },
                                car.id,
                            ),
                            car.token
                        )
                    Timber.d("Uploaded ${locs.size} locations to $path.")
                    Outcome.Success(result)
                } catch (e: Exception) {
                    Timber.w(e, "Failed to POST location updates to $path.")
                    Outcome.Error(e)
                }
            }
}
