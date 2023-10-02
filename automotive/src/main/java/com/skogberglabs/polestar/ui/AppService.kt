package com.skogberglabs.polestar.ui

import android.content.Context
import com.skogberglabs.polestar.Adapters
import com.skogberglabs.polestar.ApiUserInfo
import com.skogberglabs.polestar.CarConf
import com.skogberglabs.polestar.CarHttpClient
import com.skogberglabs.polestar.CarInfo
import com.skogberglabs.polestar.CarLang
import com.skogberglabs.polestar.CarLanguage
import com.skogberglabs.polestar.CarListener
import com.skogberglabs.polestar.CarState
import com.skogberglabs.polestar.ConfState
import com.skogberglabs.polestar.Email
import com.skogberglabs.polestar.Google
import com.skogberglabs.polestar.GoogleTokenSource
import com.skogberglabs.polestar.LocalDataSource
import com.skogberglabs.polestar.LocationUpdate
import com.skogberglabs.polestar.Outcome
import com.skogberglabs.polestar.ProfileInfo
import com.skogberglabs.polestar.SimpleMessage
import com.skogberglabs.polestar.UserContainer
import com.skogberglabs.polestar.UserInfo
import com.skogberglabs.polestar.UserState
import com.skogberglabs.polestar.location.LocationSource
import com.skogberglabs.polestar.location.LocationSourceInterface
import com.skogberglabs.polestar.location.LocationUploader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.time.Duration.Companion.seconds

interface CarViewModelInterface {
    val currentLang: Flow<Outcome<CarLang>>
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
            override val currentLang: Flow<Outcome<CarLang>> = MutableStateFlow(Outcome.Idle)
            override val languages: StateFlow<List<CarLanguage>> = MutableStateFlow(Previews.conf(ctx).languages.map { it.language })
            override val savedLanguage: Flow<String?> = MutableStateFlow(null)
            override val user: StateFlow<Outcome<UserInfo>> = MutableStateFlow(Outcome.Idle)
            val cars = ProfileInfo(ApiUserInfo(Email("a@b.com"), listOf(CarInfo("a", "Mos", 1L), CarInfo("b", "Tesla", 1L), CarInfo("a", "Toyota", 1L), CarInfo("a", "Rivian", 1L), CarInfo("a", "Cybertruck", 1L))), null)
            override val profile: StateFlow<Outcome<ProfileInfo?>> = MutableStateFlow(
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
class AppService(applicationContext: Context, val mainScope: CoroutineScope, private val ioScope: CoroutineScope): CarViewModelInterface {
    private val userState = UserState.instance
    private val confState = ConfState.instance
    val google = Google.build(applicationContext, userState)
    val http = CarHttpClient(GoogleTokenSource(google))
    private val preferences = LocalDataSource(applicationContext)
    override val locationSource = LocationSource.instance
    val carListener = CarListener(applicationContext)
    private val locationUploader = LocationUploader(http, userState, preferences, locationSource, carListener)
    override val uploadMessage = locationUploader.message
    override val carState = carListener.carInfo

    override val user: StateFlow<Outcome<UserInfo>> = userState.userResult
    private val activeCar = preferences.userPreferencesFlow().map { it.carId }
    override val profile: StateFlow<Outcome<ProfileInfo?>> = user.filter { it != Outcome.Loading }.distinctUntilChanged().flatMapLatest { user ->
        when (user) {
            is Outcome.Success -> meFlow().map { it.map { u -> u.user } }
            else -> flowOf(Outcome.Idle)
        }
    }.combine(activeCar) { user, carId ->
        user.map { ProfileInfo(it, carId) }
    }.distinctUntilChanged().stateIn(mainScope, SharingStarted.Eagerly, Outcome.Idle)
    fun profileLatest(): ProfileInfo? = profile.value.toOption()
    private val confs: StateFlow<CarConf?> = confState.conf
    override val savedLanguage: StateFlow<String?> = preferences.userPreferencesFlow().map { it.language }.distinctUntilChanged()
        .stateIn(ioScope, SharingStarted.Eagerly, null)
    fun currentLanguage() = savedLanguage.value
    override val languages: StateFlow<List<CarLanguage>> = confs.map { c -> c?.let { it.languages.map { l -> l.language } } ?: emptyList() }
        .distinctUntilChanged()
        .stateIn(ioScope, SharingStarted.Eagerly, emptyList())
    fun languagesLatest() = languages.value
    override val currentLang: StateFlow<Outcome<CarLang>> = confs.combine(savedLanguage) { confs, saved ->
        val cs = confs ?: CarConf(emptyList())
        val attempt = cs.languages.firstOrNull { it.language.code == saved } ?: cs.languages.firstOrNull()
        attempt?.let { Outcome.Success(it) } ?: Outcome.Loading
    }.stateIn(ioScope, SharingStarted.Eagerly, Outcome.Idle)
    val appState: StateFlow<AppState> = currentLang.map { it.toOption() }.combine(profile.map { it.toOption() }) { lang, user ->
        if (lang != null)
            if (user != null) AppState.LoggedIn(user, lang)
            else AppState.Anon(lang)
        else AppState.Loading
    }.distinctUntilChanged().stateIn(mainScope, SharingStarted.Eagerly, AppState.Loading)
    fun state() = appState.value

    fun onCreate() = ioScope.launch { initialize() }

    private suspend fun initialize() {
        google.signInSilently()
        val response = http.get("/cars/conf", Adapters.carConf)
        confState.update(response)
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
        ioScope.launch {
            preferences.saveCarId(id)
        }
    }

    override fun saveLanguage(code: String) {
        ioScope.launch {
            preferences.saveLanguage(code)
        }
    }

    override fun signOut() {
        ioScope.launch {
            google.signOut()
        }
    }
}
