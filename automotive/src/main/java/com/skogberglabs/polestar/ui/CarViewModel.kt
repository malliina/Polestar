package com.skogberglabs.polestar.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.skogberglabs.polestar.Adapters
import com.skogberglabs.polestar.ApiUserInfo
import com.skogberglabs.polestar.CarApp
import com.skogberglabs.polestar.CarConf
import com.skogberglabs.polestar.CarInfo
import com.skogberglabs.polestar.CarLang
import com.skogberglabs.polestar.CarLanguage
import com.skogberglabs.polestar.CarState
import com.skogberglabs.polestar.Email
import com.skogberglabs.polestar.LocationUpdate
import com.skogberglabs.polestar.Outcome
import com.skogberglabs.polestar.ProfileInfo
import com.skogberglabs.polestar.SimpleMessage
import com.skogberglabs.polestar.UserContainer
import com.skogberglabs.polestar.UserInfo
import com.skogberglabs.polestar.location.LocationSourceInterface
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.time.Duration.Companion.seconds

interface CarViewModelInterface {
    val conf: Flow<Outcome<CarLang>>
    val languages: Flow<List<CarLanguage>>
    val savedLanguage: Flow<String?>
    val user: StateFlow<Outcome<UserInfo>>
    val profile: Flow<Outcome<ProfileInfo?>>
    val uploadMessage: SharedFlow<Outcome<SimpleMessage>>
    val locationSource: LocationSourceInterface
    val carState: StateFlow<CarState>
    fun selectCar(id: String)
    fun saveLanguage(code: String)
    fun signOut()

    companion object {
        fun preview(ctx: Context) = object : CarViewModelInterface {
            override val conf: Flow<Outcome<CarLang>> = MutableStateFlow(Outcome.Idle)
            override val languages: Flow<List<CarLanguage>> = MutableStateFlow(Previews.conf(ctx).languages.map { it.language })
            override val savedLanguage: Flow<String?> = MutableStateFlow(null)
            override val user: StateFlow<Outcome<UserInfo>> = MutableStateFlow(Outcome.Idle)
            val cars = ProfileInfo(ApiUserInfo(Email("a@b.com"), listOf(CarInfo("a", "Mos", 1L), CarInfo("b", "Tesla", 1L), CarInfo("a", "Toyota", 1L), CarInfo("a", "Rivian", 1L), CarInfo("a", "Cybertruck", 1L))), null)
            override val profile: Flow<Outcome<ProfileInfo?>> = MutableStateFlow(
                Outcome.Success(
                    cars
                )
            )
            override val uploadMessage: SharedFlow<Outcome<SimpleMessage>> = MutableSharedFlow()
            override val locationSource: LocationSourceInterface = object :
                LocationSourceInterface {
                override val currentLocation: Flow<LocationUpdate?> = MutableStateFlow(null)
                override val locationUpdates: SharedFlow<List<LocationUpdate>> = MutableSharedFlow()
            }
            override val carState: StateFlow<CarState> = MutableStateFlow(CarState.empty)
            override fun selectCar(id: String) {}
            override fun saveLanguage(code: String) {}
            override fun signOut() {}
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class CarViewModel(private val appl: Application) : AndroidViewModel(appl),
    CarViewModelInterface {
    val app: CarApp = appl as CarApp
    val http = app.http
    override val locationSource = app.locationSource
    val google = app.google
    override val uploadMessage = app.uploader.message
    override val carState = app.carInfo.carInfo

    override val user: StateFlow<Outcome<UserInfo>> = app.userState.userResult
    private val activeCar = app.preferences.userPreferencesFlow().map { it.carId }
    override val profile: Flow<Outcome<ProfileInfo?>> = user.filter { it != Outcome.Loading }.distinctUntilChanged().flatMapLatest { user ->
        when (user) {
            is Outcome.Success -> meFlow().map { it.map { u -> u.user } }
            else -> flow { Outcome.Idle }
        }
    }.combine(activeCar) { user, carId ->
        user.map { ProfileInfo(it, carId) }
    }
    private val confs: StateFlow<CarConf?> = app.confState.conf
    override val savedLanguage: Flow<String?> = app.preferences.userPreferencesFlow().map { it.language }.distinctUntilChanged()
    override val languages: Flow<List<CarLanguage>> = confs.map { c -> c?.let { it.languages.map { l -> l.language } } ?: emptyList() }
    override val conf: Flow<Outcome<CarLang>> = confs.combine(savedLanguage) { confs, saved ->
        val cs = confs ?: CarConf(emptyList())
        val attempt = cs.languages.firstOrNull { it.language.code == saved } ?: cs.languages.firstOrNull()
        attempt?.let { Outcome.Success(it) } ?: Outcome.Loading
    }

    private suspend fun meFlow(): Flow<Outcome<UserContainer>> = flow {
        try {
            emit(Outcome.Loading)
            val response = http.get("/users/me", Adapters.userContainer)
            emit(Outcome.Success(response))
        } catch (e: Exception) {
            Timber.e(e, "Failed to load profile. Retrying soon...")
            emit(Outcome.Error(e))
            delay(30.seconds)
            emitAll(meFlow())
        }
    }

    override fun selectCar(id: String) {
        viewModelScope.launch {
            app.preferences.saveCarId(id)
        }
    }

    override fun saveLanguage(code: String) {
        viewModelScope.launch {
            app.preferences.saveLanguage(code)
        }
    }

    override fun signOut() {
        viewModelScope.launch {
            google.signOut()
        }
    }
}
