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

interface StatsViewModelInterface {
    val currentLocation: Flow<LocationUpdate?>
    val carState: StateFlow<CarState>
    val uploadMessage: SharedFlow<Outcome<SimpleMessage>>

    companion object {
        fun preview(ctx: Context) = object: StatsViewModelInterface {
            override val currentLocation: Flow<LocationUpdate?> = MutableStateFlow(null)
            override val uploadMessage: SharedFlow<Outcome<SimpleMessage>> = MutableSharedFlow()
            override val carState: StateFlow<CarState> = MutableStateFlow(CarState.empty)
        }
    }
}

interface CarViewModelInterface {
    val languages: Flow<List<CarLanguage>>
    val savedLanguage: Flow<String?>
    val profile: Flow<Outcome<ProfileInfo?>>
    fun selectCar(id: String)
    fun saveLanguage(code: String)
    fun signOut()

    companion object {
        fun preview(ctx: Context) = object : CarViewModelInterface {
            override val languages: StateFlow<List<CarLanguage>> = MutableStateFlow(Previews.conf(ctx).languages.map { it.language })
            override val savedLanguage: Flow<String?> = MutableStateFlow(null)
            val cars = ProfileInfo(ApiUserInfo(Email("a@b.com"), listOf(CarInfo("a", "Mos", 1L), CarInfo("b", "Tesla", 1L), CarInfo("a", "Toyota", 1L), CarInfo("a", "Rivian", 1L), CarInfo("a", "Cybertruck", 1L))), null)
            override val profile: StateFlow<Outcome<ProfileInfo?>> =
                MutableStateFlow(Outcome.Success(cars))
            override fun selectCar(id: String) {}
            override fun saveLanguage(code: String) {}
            override fun signOut() {}
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class AppService(applicationContext: Context,
                 val mainScope: CoroutineScope,
                 private val ioScope: CoroutineScope): CarViewModelInterface {
    private val userState = UserState.instance
    private val confState = ConfState.instance
    val google = Google.build(applicationContext, userState)
    private val http = CarHttpClient(GoogleTokenSource(google))
    private val preferences = LocalDataSource(applicationContext)
    private val locationSource = LocationSource.instance
    private val carListener = CarListener(applicationContext)
    private val locationUploader = LocationUploader(http, userState, preferences, locationSource, carListener, ioScope)
    private val activeCar = preferences.userPreferencesFlow().map { it.carId }
    override val profile: StateFlow<Outcome<ProfileInfo?>> = userState.userResult.flatMapLatest { user ->
        when (user) {
            is Outcome.Success -> meFlow().map { it.map { u -> u.user } }
            Outcome.Idle -> flowOf(Outcome.Idle)
            Outcome.Loading -> flowOf(Outcome.Loading)
            is Outcome.Error -> flowOf(Outcome.Error(user.e))
        }
    }.combine(activeCar) { user, carId ->
        user.map { ProfileInfo(it, carId) }
    }.stateIn(mainScope, SharingStarted.Eagerly, Outcome.Idle)
    fun profileLatest(): ProfileInfo? = profile.value.toOption()
    private val confs: StateFlow<CarConf?> = confState.conf
    override val savedLanguage: StateFlow<String?> = preferences.userPreferencesFlow().map { it.language }.distinctUntilChanged()
        .stateIn(ioScope, SharingStarted.Eagerly, null)
    fun currentLanguage() = savedLanguage.value
    override val languages: StateFlow<List<CarLanguage>> =
        confs.map { c -> c?.let { it.languages.map { l -> l.language } } ?: emptyList() }
        .stateIn(ioScope, SharingStarted.Eagerly, emptyList())
    fun languagesLatest() = languages.value
    private val currentLang: StateFlow<Outcome<CarLang>> = confs.combine(savedLanguage) { confs, saved ->
        val cs = confs ?: CarConf(emptyList())
        val attempt = cs.languages.firstOrNull { it.language.code == saved } ?: cs.languages.firstOrNull()
        attempt?.let { Outcome.Success(it) } ?: Outcome.Loading
    }.stateIn(ioScope, SharingStarted.Eagerly, Outcome.Idle)
    val appState: StateFlow<AppState> = currentLang.combine(profile) { lang, user ->
        Timber.i("Lang $lang")
        Timber.i("User $user")
        when (lang) {
            is Outcome.Error -> AppState.Loading
            Outcome.Idle -> AppState.Loading
            Outcome.Loading -> AppState.Loading
            is Outcome.Success -> when (user) {
                is Outcome.Error -> AppState.Anon(lang.result)
                Outcome.Idle -> AppState.Anon(lang.result)
                Outcome.Loading -> AppState.Loading
                is Outcome.Success ->
                    user.result?.let { AppState.LoggedIn(it, lang.result) } ?: AppState.Anon(lang.result)
            }
        }
    }.stateIn(mainScope, SharingStarted.Eagerly, AppState.Loading)
    fun state() = appState.value

    fun onCreate() {
        carListener.connect()
        ioScope.launch { initialize() }
    }

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
