package com.skogberglabs.polestar.location

import com.skogberglabs.polestar.Coord
import com.skogberglabs.polestar.LocationUpdate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn

interface LocationSourceInterface {
    val locationUpdates: SharedFlow<List<LocationUpdate>>
    val currentLocation: StateFlow<LocationUpdate?>

    fun locationLatest(): Coord?
}

class LocationSource : LocationSourceInterface {
    companion object {
        val instance = LocationSource()
    }

    private val scope = CoroutineScope(Dispatchers.IO)
    private val locationServicesAvailability: MutableStateFlow<Boolean?> =
        MutableStateFlow(null)
    private val updatesState: MutableStateFlow<List<LocationUpdate>> =
        MutableStateFlow(emptyList())
    override val locationUpdates: SharedFlow<List<LocationUpdate>> =
        updatesState.shareIn(scope, SharingStarted.Eagerly, replay = 1)
    override val currentLocation: StateFlow<LocationUpdate?> =
        locationUpdates.map { it.lastOrNull() }.stateIn(scope, SharingStarted.Eagerly, null)

    override fun locationLatest(): Coord? = currentLocation.value?.let { loc -> Coord(loc.latitude, loc.longitude) }

    val locationServicesAvailable: StateFlow<Boolean?> =
        locationServicesAvailability.stateIn(scope, SharingStarted.Eagerly, null)

    fun save(updates: List<LocationUpdate>): Boolean = updatesState.tryEmit(updates)

    fun availability(isAvailable: Boolean) = locationServicesAvailability.tryEmit(isAvailable)
}
