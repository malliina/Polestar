package com.skogberglabs.polestar.ui

import android.content.Intent
import android.net.Uri
import android.text.SpannableString
import android.text.Spanned
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.CarColor
import androidx.car.app.model.CarIcon
import androidx.car.app.model.CarLocation
import androidx.car.app.model.Distance
import androidx.car.app.model.DistanceSpan
import androidx.car.app.model.PlaceMarker
import androidx.car.app.model.Template
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.skogberglabs.polestar.AppService
import com.skogberglabs.polestar.CarLang
import com.skogberglabs.polestar.Coord
import com.skogberglabs.polestar.Outcome
import com.skogberglabs.polestar.action
import com.skogberglabs.polestar.actionStrip
import com.skogberglabs.polestar.carLocation
import com.skogberglabs.polestar.itemList
import com.skogberglabs.polestar.metadata
import com.skogberglabs.polestar.place
import com.skogberglabs.polestar.placeListTemplate
import com.skogberglabs.polestar.placeMarker
import com.skogberglabs.polestar.row
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.time.Duration.Companion.seconds

class PlacesScreen(
    carContext: CarContext,
    private val service: AppService,
    private val lang: CarLang,
) : Screen(carContext), LifecycleEventObserver {
    private val locationSource = service.locationSource
    private val latestCoord = locationSource.locationLatest() ?: Coord(60.155, 24.877)
    private var currentLocation: CarLocation = CarLocation.create(latestCoord.lat, latestCoord.lng)

    private var job: Job? = null
    private var locationJob: Job? = null
    private var isFirstRender = true

    init {
        lifecycle.addObserver(this)
    }

    @OptIn(FlowPreview::class)
    override fun onStateChanged(
        source: LifecycleOwner,
        event: Lifecycle.Event,
    ) {
        when (event) {
            Lifecycle.Event.ON_START -> {
                job =
                    service.mainScope.launch {
                        service.parkings.collect { ps ->
                            when (ps) {
                                Outcome.Idle -> {
                                    Timber.i("Parkings state is idle.")
                                    service.searchParkings(currentLocation)
                                }
                                Outcome.Loading -> {
                                    Timber.i("Loading, refreshing...")
                                    invalidateIfUpdate()
                                }
                                is Outcome.Success -> {
                                    val ds = ps.result.directions
                                    Timber.i("Got ${ds.size} directions, refreshing places list...")
                                    invalidateIfUpdate()
                                }

                                is Outcome.Error -> {
                                    Timber.i(ps.e, "Errored out.")
                                    invalidateIfUpdate()
                                }
                            }
                        }
                    }
                locationJob =
                    service.mainScope.launch {
                        locationSource.currentLocation
                            .filterNotNull()
                            .distinctUntilChanged()
                            .sample(15.seconds)
                            .collect { coord ->
                                updateLocation(CarLocation.create(coord.latitude, coord.longitude))
                            }
                    }
            }
            Lifecycle.Event.ON_STOP -> {
                job?.cancel()
                job?.let { Timber.i("Canceled parkings listener.") }
                job = null
                locationJob?.cancel()
                locationJob = null
            }
            else -> {}
        }
    }

    private fun invalidateIfUpdate() {
        if (!isFirstRender) {
            Timber.i("Invalidating map template...")
            invalidate()
        }
        isFirstRender = false
    }

    private fun updateLocation(loc: CarLocation) {
        currentLocation = loc
        Timber.i("Invalidating due to location update")
        invalidateIfUpdate()
    }

    override fun onGetTemplate(): Template {
        Timber.i("Getting template...")
        val interpunct = "\u00b7"
        val myPlace =
            place(currentLocation) {
                setMarker(
                    placeMarker {
                        setIcon(CarIcon.APP_ICON, PlaceMarker.TYPE_ICON)
                        setColor(CarColor.BLUE)
                    },
                )
            }
        return placeListTemplate {
            setTitle(lang.settings.parking)
            setHeaderAction(Action.BACK)
            when (val s = service.parkings.value) {
                Outcome.Idle -> {
//                    setItemList(
//                        itemList {
//                            setNoItemsMessage(lang.settings.searchParkingsHint)
//                        },
//                    )
                    setLoading(true)
                }
                Outcome.Loading -> {
                    setLoading(true)
                }
                is Outcome.Success -> {
                    val list =
                        itemList {
                            setNoItemsMessage(lang.settings.noParkingAvailable)
                            s.result.directions.forEach { result ->
                                addItem(
                                    row {
                                        val parkingCoord = result.nearest.coord
                                        val span =
                                            DistanceSpan.create(
                                                Distance.create(result.nearest.distance.meters, Distance.UNIT_METERS),
                                            )
                                        val str =
                                            SpannableString(
                                                "  $interpunct ${result.capacity} ${lang.settings.availableSpots}",
                                            )
                                        str.setSpan(span, 0, 1, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
                                        setTitle(str)
                                        result.nearest.address?.let { address ->
                                            addText(address)
                                        }
                                        setMetadata(
                                            metadata {
                                                setPlace(
                                                    place(parkingCoord.carLocation()) {
                                                        setMarker(
                                                            placeMarker {
                                                            },
                                                        )
                                                    },
                                                )
                                            },
                                        )
                                        setBrowsable(false)
                                        setOnClickListener {
                                            // https://developer.android.com/training/cars/apps#handle-user-input
                                            val navigationIntent =
                                                Intent(
                                                    CarContext.ACTION_NAVIGATE,
                                                    Uri.parse("geo:${parkingCoord.lat},${parkingCoord.lng}"),
                                                )
                                            carContext.startCarApp(navigationIntent)
                                        }
                                    },
                                )
                            }
                        }
                    setItemList(list)
                    setLoading(false)
                }
                is Outcome.Error -> {
                    setItemList(
                        itemList {
                            setNoItemsMessage(lang.settings.failedToLoadParkings)
                        },
                    )
                    setLoading(false)
                }
            }
            setCurrentLocationEnabled(true)
            setAnchor(myPlace)
            setActionStrip(
                actionStrip {
                    addAction(
                        action {
                            setTitle(lang.settings.title)
                            setOnClickListener {
                                screenManager.push(SettingsScreen(carContext, lang, service))
                            }
                        },
                    )
                },
            )

            setOnContentRefreshListener {
                Timber.i("Refreshing...")
                service.searchParkings(currentLocation)
            }
        }
    }
}
