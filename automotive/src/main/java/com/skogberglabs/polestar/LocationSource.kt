package com.skogberglabs.polestar

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn

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
