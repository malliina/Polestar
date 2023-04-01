package com.skogberglabs.polestar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class UserState {
    companion object {
        val instance = UserState()
    }

    private val userState: MutableStateFlow<UserInfo?> = MutableStateFlow(null)
    val user: StateFlow<UserInfo?> = userState

    fun update(user: UserInfo?) {
        userState.value = user
    }
}

class ProfileViewModel(private val appl: Application): AndroidViewModel(appl) {
    val app: PolestarApp = appl as PolestarApp
    val http = app.http
    val locations = app.locations
    val locationSource = app.locationSource
    val google = app.google

    val user: StateFlow<UserInfo?> = UserState.instance.user
}
