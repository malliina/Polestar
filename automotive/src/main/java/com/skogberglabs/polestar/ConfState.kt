package com.skogberglabs.polestar

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber

class ConfState {
    private val confState: MutableStateFlow<CarConf?> = MutableStateFlow(null)
    val conf: StateFlow<CarConf?> = confState

    suspend fun update(conf: CarConf) {
        Timber.i("Updating conf.")
        confState.emit(conf)
    }

    companion object {
        val instance = ConfState()
    }
}
