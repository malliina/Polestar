package com.skogberglabs.polestar

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
    val conf: Flow<Outcome<CarLang>>
    val languages: Flow<List<CarLanguage>>
    val savedLanguage: Flow<String?>
    val user: StateFlow<Outcome<UserInfo>>
    val profile: Flow<Outcome<ProfileInfo?>>
    val uploadMessage: SharedFlow<Outcome<SimpleMessage>>
    val locationSource: LocationSourceInterface
    val carState: StateFlow<CarState>
    suspend fun prepare()
    fun selectCar(id: String)
    fun saveLanguage(code: String)
    fun signOut()

    companion object {
        fun preview(ctx: Context) = object : ProfileViewModelInterface {
            override val conf: Flow<Outcome<CarLang>> = MutableStateFlow(Outcome.Idle)
            override val languages: Flow<List<CarLanguage>> = MutableStateFlow(Previews.conf(ctx).languages.map { it.language })
            override val savedLanguage: Flow<String?> = MutableStateFlow(null)
            override val user: StateFlow<Outcome<UserInfo>> = MutableStateFlow(Outcome.Idle)
            val cars = ProfileInfo(ApiUserInfo(Email("a@b.com"), listOf(CarInfo("a", "Mos", 1L), CarInfo("b", "Tesla", 1L), CarInfo("a", "Toyota", 1L), CarInfo("a", "Rivian", 1L), CarInfo("a", "Cybertruck", 1L))), null)
            override val profile: Flow<Outcome<ProfileInfo?>> = MutableStateFlow(Outcome.Success(cars))
            override val uploadMessage: SharedFlow<Outcome<SimpleMessage>> = MutableSharedFlow()
            override val locationSource: LocationSourceInterface = object : LocationSourceInterface {
                override val currentLocation: Flow<LocationUpdate?> = MutableStateFlow(null)
            }
            override val carState: StateFlow<CarState> = MutableStateFlow(CarState.empty)
            override suspend fun prepare() {}
            override fun selectCar(id: String) {}
            override fun saveLanguage(code: String) {}
            override fun signOut() {}
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModel(private val appl: Application) : AndroidViewModel(appl), ProfileViewModelInterface {
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
    private val confs: MutableStateFlow<CarConf> = MutableStateFlow(CarConf(emptyList()))
    override val savedLanguage: Flow<String?> = app.preferences.userPreferencesFlow().map { it.language }.distinctUntilChanged()
    override val languages: Flow<List<CarLanguage>> = confs.map { c -> c.languages.map { l -> l.language } }
    override val conf: Flow<Outcome<CarLang>> = confs.combine(savedLanguage) { confs, saved ->
        val attempt = confs.languages.firstOrNull { it.language.code == saved } ?: confs.languages.firstOrNull()
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

    override suspend fun prepare() {
        val response = http.get("/cars/conf", Adapters.carConf)
        confs.emit(response)
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
