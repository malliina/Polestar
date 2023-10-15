package com.skogberglabs.polestar.ui

import android.content.Context
import com.skogberglabs.polestar.CarState
import com.skogberglabs.polestar.LocationUpdate
import com.skogberglabs.polestar.Outcome
import com.skogberglabs.polestar.SimpleMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface StatsViewModelInterface {
    val currentLocation: Flow<LocationUpdate?>
    val carState: StateFlow<CarState>
    val uploadMessage: SharedFlow<Outcome<SimpleMessage>>

    companion object {
        fun preview(ctx: Context) = object : StatsViewModelInterface {
            override val currentLocation: Flow<LocationUpdate?> = MutableStateFlow(null)
            override val uploadMessage: SharedFlow<Outcome<SimpleMessage>> = MutableSharedFlow()
            override val carState: StateFlow<CarState> = MutableStateFlow(CarState.empty)
        }
    }
}
