package com.skogberglabs.polestar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
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
    val carState: StateFlow<CarState>
    fun selectCar(id: String)
    fun signOut()

    companion object {
        val preview = object : ProfileViewModelInterface {
            override val user: StateFlow<Outcome<UserInfo>> = MutableStateFlow(Outcome.Idle)
            override val profile: Flow<Outcome<ProfileInfo?>> = MutableStateFlow(Outcome.Idle)
            override val uploadMessage: SharedFlow<Outcome<SimpleMessage>> = MutableSharedFlow()
            override val locationSource: LocationSourceInterface = object : LocationSourceInterface {
                override val currentLocation: Flow<LocationUpdate?> = MutableStateFlow(null)
            }
            override val carState: StateFlow<CarState> = MutableStateFlow(CarState.empty)
            override fun selectCar(id: String) {}
            override fun signOut() {}
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModel(private val appl: Application) : AndroidViewModel(appl), ProfileViewModelInterface {
    val app: CarTrackerApp = appl as CarTrackerApp
    val http = app.http
    override val locationSource = app.locationSource
    val google = app.google
    override val uploadMessage = app.uploader.message
    override val carState = app.carInfo.carInfo

    override val user: StateFlow<Outcome<UserInfo>> = app.userState.userResult
    private val activeCar = app.preferences.userPreferencesFlow().map { it.carId }
    override val profile: Flow<Outcome<ProfileInfo?>> = user.filter { it != Outcome.Loading }.distinctUntilChanged().flatMapLatest { user ->
        when (user) {
            is Outcome.Success -> flow {
                emit(Outcome.Loading)
                emit(me().map { it.user })
            }
            else -> flow { Outcome.Idle }
        }
    }.combine(activeCar) { user, carId ->
        user.map { ProfileInfo(it, carId) }
    }
    private suspend fun me() =
        try {
            Outcome.Success(http.get("/users/me", Adapters.userContainer))
        } catch (e: Exception) {
            Timber.e(e, "Failed to load profile.")
            Outcome.Error(e)
        }

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
