package com.skogberglabs.polestar

import android.app.Application
import androidx.compose.runtime.MutableState
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
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

interface ProfileViewModelInterface {
    val user: StateFlow<Outcome<UserInfo>>
    val profile: Flow<Outcome<ProfileInfo?>>
    val uploadMessage: SharedFlow<Outcome<SimpleMessage>>
    val locationSource: LocationSourceInterface

    fun selectCar(id: String)
    fun signOut()

    companion object {
        val preview = object: ProfileViewModelInterface {
            override val user: StateFlow<Outcome<UserInfo>> = MutableStateFlow(Outcome.Idle)
            override val profile: Flow<Outcome<ProfileInfo?>> = MutableStateFlow(Outcome.Idle)
            override val uploadMessage: SharedFlow<Outcome<SimpleMessage>> = MutableSharedFlow()
            override val locationSource: LocationSourceInterface = object : LocationSourceInterface {
                override val currentLocation: Flow<LocationUpdate?> = MutableStateFlow(null)
            }
            override fun selectCar(id: String) {}
            override fun signOut() {}
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModel(private val appl: Application) : AndroidViewModel(appl), ProfileViewModelInterface {
    val app: PolestarApp = appl as PolestarApp
    val http = app.http
    override val locationSource = app.locationSource
    val google = app.google
    override val uploadMessage = app.uploader.message

    override val user: StateFlow<Outcome<UserInfo>> = app.userState.userResult
    private val activeCar = app.preferences.userPreferencesFlow().map { it.carId }
    override val profile: Flow<Outcome<ProfileInfo?>> = user.flatMapLatest { user ->
        flow {
            emit(Outcome.Loading)
            val u = user.toOption()?.let {
                me().user
            }
            emit(Outcome.Success(u))
        }
    }.combine(activeCar) { user, carId ->
        when (val u = user) {
            is Outcome.Success -> Outcome.Success(u.result?.let { ProfileInfo(it, carId) })
            is Outcome.Error -> Outcome.Error(u.e)
            Outcome.Idle -> Outcome.Idle
            Outcome.Loading -> Outcome.Loading
        }
    }
    private suspend fun me() = http.get("/users/me", Adapters.userContainer)

    override fun selectCar(id: String) {
        viewModelScope.launch {
            app.preferences.saveCarId(id)
        }
    }

    override fun signOut() {
        viewModelScope.launch {
            google.signOut()
        }
    }
}
