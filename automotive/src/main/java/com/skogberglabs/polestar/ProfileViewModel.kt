package com.skogberglabs.polestar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
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

data class ProfileInfo(val user: ApiUserInfo, val carId: String?) {
    val activeCar = user.boats.firstOrNull { car -> car.id == carId }
    val hasCars = user.boats.isNotEmpty()
}

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModel(private val appl: Application) : AndroidViewModel(appl) {
    val app: PolestarApp = appl as PolestarApp
    val http = app.http
    val locationSource = app.locationSource
    val google = app.google
    val uploadMessage = app.uploader.message

    val user: StateFlow<Outcome<UserInfo>> = app.userState.userResult
    private val activeCar = app.preferences.userPreferencesFlow().map { it.carId }
    val profile: Flow<ProfileInfo?> = user.mapLatest { user ->
        user.toOption()?.let {
            me().user
        }
    }.combine(activeCar) { user, carId -> user?.let { ProfileInfo(it, carId) } }

    private suspend fun me() = http.get("/users/me", Adapters.userContainer)

    fun selectCar(id: String) {
        viewModelScope.launch {
            app.preferences.saveCarId(id)
        }
    }

    fun signOut() {
        viewModelScope.launch {
            google.signOut()
        }
    }
}
