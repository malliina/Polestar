package com.skogberglabs.polestar

import android.content.Context
import android.graphics.BitmapFactory
import androidx.car.app.model.CarLocation
import androidx.core.graphics.drawable.IconCompat
import com.skogberglabs.polestar.location.CarLocationService
import com.skogberglabs.polestar.location.LocationSource
import com.skogberglabs.polestar.location.LocationUploader
import com.skogberglabs.polestar.location.isAllPermissionsGranted
import com.skogberglabs.polestar.location.isForegroundServiceGranted
import com.skogberglabs.polestar.location.isLocationGranted
import com.skogberglabs.polestar.ui.AppState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import okhttp3.Request
import okio.buffer
import okio.sink
import timber.log.Timber
import java.io.File
import java.time.Instant
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

interface CarViewModelInterface {
    val profile: Flow<Outcome<ProfileInfo?>>

    fun selectCar(ref: CarRef)

    fun saveLanguage(code: String)

    fun signOut()

    companion object {
        fun preview(ctx: Context) =
            object : CarViewModelInterface {
                val cars =
                    ProfileInfo(
                        ApiUserInfo(
                            Email("a@b.com"),
                            listOf(
                                CarInfo(1, "Mos", "t", 1L),
                                CarInfo(1, "Tesla", "t", 1L),
                                CarInfo(1, "Toyota", "t", 1L),
                                CarInfo(1, "Rivian", "t", 1L),
                                CarInfo(1, "Cybertruck", "t", 1L),
                            ),
                            emptyList()
                        ),
                        null,
                        null,
                    )
                override val profile: StateFlow<Outcome<ProfileInfo?>> =
                    MutableStateFlow(Outcome.Success(cars))

                override fun selectCar(ref: CarRef) {}

                override fun saveLanguage(code: String) {}

                override fun signOut() {}
            }
    }
}

data class ParkingsSearch(val near: Coord, val at: Instant)

@OptIn(ExperimentalCoroutinesApi::class)
class AppService(
    private val applicationContext: Context,
    val mainScope: CoroutineScope,
    private val ioScope: CoroutineScope,
) : CarViewModelInterface {
    private val userState = UserState.instance
    val google = Google.build(applicationContext, userState)

//    val google = GoogleCredManager.build(applicationContext, userState)
    private val http = CarHttpClient(GoogleTokenSource(google))
    private val preferences = LocalDataSource(applicationContext)
    val locationSource = LocationSource.instance
    private val carListener = CarListener(applicationContext)
    val locationUploader = LocationUploader(http, userState, preferences, locationSource, carListener, ioScope)
    private val activeCar = preferences.userPreferencesFlow().map { it.carId }
    private val parkingSearch: MutableStateFlow<ParkingsSearch?> = MutableStateFlow(null)

    @OptIn(FlowPreview::class)
    val parkings: StateFlow<Outcome<ParkingResponse>> =
        parkingSearch.filterNotNull().flatMapLatest { query ->
            parkingsFlow(query)
        }.debounce(200.milliseconds)
            .flowOn(Dispatchers.IO)
            .stateIn(mainScope, SharingStarted.Eagerly, Outcome.Idle)

    override val profile: StateFlow<Outcome<ProfileInfo>> =
        userState.userResult.flatMapLatest { user ->
            Timber.i("Now got $user")
            when (user) {
                is Outcome.Success -> meFlow().map { it.map { u -> u } }
                Outcome.Idle -> flowOf(Outcome.Idle)
                Outcome.Loading -> flowOf(Outcome.Loading)
                is Outcome.Error -> flowOf(Outcome.Error(user.e))
            }
        }.combine(activeCar) { user, carId ->
            user.map { ProfileInfo(it.user, carId, it.localCarImage) }
        }.flowOn(Dispatchers.IO)
            .stateIn(mainScope, SharingStarted.Eagerly, Outcome.Idle)

    fun profileLatest(): ProfileInfo? = profile.value.toOption()

    val prefs =
        preferences.userPreferencesFlow()
            .stateIn(ioScope, SharingStarted.Eagerly, UserPreferences.empty)
    private val currentLang: StateFlow<Outcome<CarLang>> =
        preferences.userPreferencesFlow()
            .map { it.lang?.let { lang -> Outcome.Success(lang) } ?: Outcome.Loading }
            .stateIn(ioScope, SharingStarted.Eagerly, Outcome.Idle)

    @OptIn(FlowPreview::class)
    val appState: StateFlow<AppState> =
        currentLang.combine(profile) { lang, user ->
            when (lang) {
                is Outcome.Error -> AppState.Loading(null)
                Outcome.Idle -> AppState.Loading(null)
                Outcome.Loading -> AppState.Loading(null)
                is Outcome.Success ->
                    when (user) {
                        is Outcome.Error -> AppState.Anon(lang.result)
                        Outcome.Idle -> AppState.Anon(lang.result)
                        Outcome.Loading -> AppState.Loading(lang.result)
                        is Outcome.Success -> AppState.LoggedIn(user.result, lang.result)
                    }
            }
        }.debounce(500.milliseconds).stateIn(mainScope, SharingStarted.Eagerly, AppState.Loading(null))

    private val navigateToPlacesState: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val navigateToPlaces get() = navigateToPlacesState.value

    fun initialNavigation() {
        navigateToPlacesState.value = false
    }

    fun state(): AppState = appState.value

    fun onCreate() {
        carListener.connect()
        ioScope.launch { initialize() }
    }

    fun signInSilently(tag: String) {
        ioScope.launch { google.signInSilently(tag) }
    }

    private suspend fun initialize() {
        google.signInSilently("initialize")
        // If loading conf fails, retries every 30 seconds until it succeeds once
        confFlow().map { it.toOption() }.filterNotNull().take(1).collect { conf ->
            val updated = preferences.saveConf(conf)
            updated.lang?.let { lang ->
                val nlang = lang.notifications
                val text = if (applicationContext.isAllPermissionsGranted()) nlang.enjoy else nlang.grantPermissions
                val carIntent = CarLocationService.intent(applicationContext, nlang.appRunning, text)
                if (applicationContext.isForegroundServiceGranted() && applicationContext.isLocationGranted()) {
                    applicationContext.startForegroundService(carIntent)
                }
            }
        }
    }

    // Emits loading/error states until loading conf succeeds
    private fun confFlow(): Flow<Outcome<CarConf>> =
        flow {
            emit(Outcome.Loading)
            val outcome =
                try {
                    val response = http.get<CarConf>("/cars/conf", null)
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

    private fun parkingsFlow(query: ParkingsSearch): Flow<Outcome<ParkingResponse>> =
        flow {
            emit(Outcome.Loading)
            val near = query.near
            val outcome =
                try {
                    val response =
                        http.get<ParkingResponse>(
                            "/cars/parkings/search?lat=${near.lat}&lng=${near.lng}&limit=20",
                            null,
                        )
                    Timber.i("Loaded ${response.directions.size} parkings near ${near.lat},${near.lng}.")
                    Outcome.Success(response)
                } catch (e: Exception) {
                    // Emitting in a catch-clause fails
                    Timber.e(e, "Failed to load available parking spots.")
                    Outcome.Error(e)
                }
            emit(outcome)
        }

    private fun meFlow(): Flow<Outcome<UserData>> =
        flow {
            emit(Outcome.Loading)
            val outcome =
                try {
                    val response = http.get<UserContainer>("/users/me?includeCars=true", null)
                    val localImage = response.user.cars.map { v -> v.studioImage }.firstOrNull()?.let { url ->
                        val file = download(url, "car.png")
                        val bmp = BitmapFactory.decodeFile(file.absolutePath)
                        IconCompat.createWithBitmap(bmp)
                    }
                    Outcome.Success(UserData(response.user, localImage))
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

    private suspend fun download(url: FullUrl, to: String): File {
        val req = Request.Builder().url(url.url).build()
        val response = CarHttpClient.client.newCall(req).await()
        val downloadedFile = File(applicationContext.cacheDir, to)
        val bytes = downloadedFile.sink().buffer().use { sink ->
            sink.writeAll(response.body.source())
        }
        Timber.i("Downloaded $bytes bytes to $downloadedFile.")
        return downloadedFile
    }

    fun searchParkings(loc: CarLocation) {
        searchParkings(Coord(loc.latitude, loc.longitude))
    }

    private fun searchParkings(near: Coord) {
        parkingSearch.value = ParkingsSearch(near, Instant.now())
    }

    override fun selectCar(ref: CarRef) {
        ioScope.launch {
            preferences.saveCar(ref)
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
