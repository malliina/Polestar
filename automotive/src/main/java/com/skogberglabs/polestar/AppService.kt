package com.skogberglabs.polestar

import android.content.Context
import com.skogberglabs.polestar.location.CarLocationService
import com.skogberglabs.polestar.location.LocationSource
import com.skogberglabs.polestar.location.LocationUploader
import com.skogberglabs.polestar.location.isAllPermissionsGranted
import com.skogberglabs.polestar.ui.AppState
import com.skogberglabs.polestar.ui.Previews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.time.Duration.Companion.seconds

interface CarViewModelInterface {
    val languages: Flow<List<CarLanguage>>
    val profile: Flow<Outcome<ProfileInfo?>>
    fun selectCar(id: String)
    fun saveLanguage(code: String)
    fun signOut()

    companion object {
        fun preview(ctx: Context) = object : CarViewModelInterface {
            override val languages: StateFlow<List<CarLanguage>> = MutableStateFlow(
                Previews.conf(
                    ctx
                ).languages.map { it.language })
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
class AppService(
    private val applicationContext: Context,
    val mainScope: CoroutineScope,
    private val ioScope: CoroutineScope): CarViewModelInterface {
    private val userState = UserState.instance
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
    private val savedLanguage: StateFlow<String?> = preferences.userPreferencesFlow().map { it.language }.distinctUntilChanged()
        .stateIn(ioScope, SharingStarted.Eagerly, null)
    fun currentLanguage() = savedLanguage.value
    override val languages: StateFlow<List<CarLanguage>> =
        preferences.userPreferencesFlow()
            .map { c -> c.carConf?.let { it.languages.map { l -> l.language } } ?: emptyList() }
        .stateIn(ioScope, SharingStarted.Eagerly, emptyList())
    fun languagesLatest() = languages.value
    private val currentLang: StateFlow<Outcome<CarLang>> =
        preferences.userPreferencesFlow()
            .map { it.lang?.let { lang -> Outcome.Success(lang) } ?: Outcome.Loading }
            .stateIn(ioScope, SharingStarted.Eagerly, Outcome.Idle)
    val appState: StateFlow<AppState> = currentLang.combine(profile) { lang, user ->
        when (lang) {
            is Outcome.Error -> AppState.Loading
            Outcome.Idle -> AppState.Loading
            Outcome.Loading -> AppState.Loading
            is Outcome.Success -> when (user) {
                is Outcome.Error -> AppState.Anon(lang.result)
                Outcome.Idle -> AppState.Anon(lang.result)
                Outcome.Loading -> AppState.Loading
                is Outcome.Success ->
                    user.result?.let {
                        AppState.LoggedIn(
                            it,
                            lang.result
                        )
                    } ?: AppState.Anon(lang.result)
            }
        }
    }.stateIn(mainScope, SharingStarted.Eagerly, AppState.Loading)
    fun state(): AppState = appState.value

    fun onCreate() {
        carListener.connect()
        ioScope.launch { initialize() }
    }

    fun signInSilently() {
        ioScope.launch { google.signInSilently() }
    }

    private suspend fun initialize() {
        google.signInSilently()
        // If loading conf fails, retries every 30 seconds until it succeeds once
        confFlow().map { it.toOption() }.filterNotNull().take(1).collect { conf ->
            val updated = preferences.saveConf(conf)
            updated.lang?.let { lang ->
                val nlang = lang.notifications
                val text = if (applicationContext.isAllPermissionsGranted()) nlang.enjoy else nlang.grantPermissions
                val carIntent = CarLocationService.intent(applicationContext, nlang.appRunning, text)
                applicationContext.startForegroundService(carIntent)
            }
        }
    }

    // Emits loading/error states until loading conf succeeds
    private suspend fun confFlow(): Flow<Outcome<CarConf>> = flow {
        emit(Outcome.Loading)
        val outcome = try {
            val response = http.get("/cars/conf", Adapters.carConf)
            Outcome.Success(response)
        } catch (e: Exception) {
            // Emitting in a catch-clause fails
            Timber.e(e, "Failed to load config. Retrying soon...")
            Outcome.Error(e)
        }
        emit(outcome)
        if (!outcome.isSuccess()) {
            delay(30.seconds)
            emitAll(confFlow())
        }
    }

    private suspend fun meFlow(): Flow<Outcome<UserContainer>> = flow {
        emit(Outcome.Loading)
        val outcome = try {
            val response = http.get("/users/me", Adapters.userContainer)
            Outcome.Success(response)
        } catch (e: Exception) {
            Timber.e(e, "Failed to load profile. Retrying soon...")
            Outcome.Error(e)
        }
        emit(outcome)
        if (!outcome.isSuccess()) {
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
