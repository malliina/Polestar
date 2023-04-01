package com.skogberglabs.polestar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

class UserState {
    companion object {
        val instance = UserState()
    }

    private val current: MutableStateFlow<Outcome<UserInfo>> = MutableStateFlow(Outcome.Idle)
    val userResult: StateFlow<Outcome<UserInfo>> = current

    fun update(outcome: Outcome<UserInfo>) {
        current.value = outcome
        Timber.i("Updated user with $outcome")
    }
}

class ProfileViewModel(private val appl: Application) : AndroidViewModel(appl) {
    val app: PolestarApp = appl as PolestarApp
    val http = app.http
    val locations = app.locations
    val locationSource = app.locationSource
    val google = app.google
    val uploadMessage = app.uploader.message

    val user: StateFlow<Outcome<UserInfo>> = UserState.instance.userResult

    fun signOut() {
        viewModelScope.launch {
            google.signOut()
        }
    }
}
