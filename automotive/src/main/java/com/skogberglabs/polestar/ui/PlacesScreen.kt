package com.skogberglabs.polestar.ui

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
import com.skogberglabs.polestar.itemList
import com.skogberglabs.polestar.place
import com.skogberglabs.polestar.placeListTemplate
import com.skogberglabs.polestar.placeMarker
import com.skogberglabs.polestar.row
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import timber.log.Timber

class PlacesScreen(
    carContext: CarContext,
    private val service: AppService,
    private val lang: CarLang,
) : Screen(carContext), LifecycleEventObserver {
    private val locationSource = service.locationSource
    private val latestCoord = locationSource.locationLatest() ?: Coord(60.155, 24.877)
    private var currentLocation: CarLocation = CarLocation.create(latestCoord.lat, latestCoord.lng)

    private var job: Job? = null

    init {
        lifecycle.addObserver(this)
    }

    override fun onStateChanged(
        source: LifecycleOwner,
        event: Lifecycle.Event,
    ) {
        when (event) {
            Lifecycle.Event.ON_START -> {
                job =
                    service.mainScope.launch {
//                        service.tracks.drop(1).collect { ts ->
//                            invalidate()
//                        }
                        service.parkings.filter { it != Outcome.Idle }.collect { ps ->
                            when (ps) {
                                is Outcome.Error -> {}
                                Outcome.Idle -> {}
                                Outcome.Loading -> {}
                                is Outcome.Success -> {
                                    val ds = ps.result.directions
                                    Timber.i("Got ${ds.size} directions, refreshing places list...")
                                    invalidate()
                                }
                            }
                        }
                    }
            }
            Lifecycle.Event.ON_STOP -> {
                job?.cancel()
                job?.let { Timber.i("Canceled tracks job.") }
                job = null
            }
            else -> {}
        }
    }

    private fun updateLocation(loc: CarLocation) {
        currentLocation = loc
        Timber.i("Invalidating due to location update")
        invalidate()
    }

    override fun onGetTemplate(): Template {
        Timber.i("Getting template...")
        val interpunct = "\u00b7"
        val ts = service.tracks.value
        val latestTracks = ts.toOption()?.tracks ?: emptyList()
        val trackItems =
            itemList {
                setNoItemsMessage(lang.settings.noTracks)
                latestTracks.forEach { track ->
                    addItem(
                        row {
                            val span = DistanceSpan.create(Distance.create(track.distanceMeters.kilometers, Distance.UNIT_KILOMETERS))
                            val str = SpannableString("  $interpunct ${track.topPoint.carSpeed.describeKmh}")
                            str.setSpan(span, 0, 1, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
                            setTitle(str)
                            setBrowsable(false)
                            setOnClickListener {
                                val top = track.topPoint.coord
                                updateLocation(CarLocation.create(top.lat, top.lng))
                            }
                        },
                    )
                }
            }
        val parkingItems =
            itemList {
                setNoItemsMessage("No parkings available.")
                (service.parkings.value.toOption()?.directions ?: emptyList()).forEach { result ->
                    addItem(
                        row {
                            val span = DistanceSpan.create(Distance.create(result.nearest.distance.meters, Distance.UNIT_METERS))
                            val str = SpannableString("  $interpunct ${result.capacity} available spots")
                            str.setSpan(span, 0, 1, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
                            setTitle(str)
                            setBrowsable(false)
                            setOnClickListener {
                                val top = result.nearest.coord
                                updateLocation(CarLocation.create(top.lat, top.lng))
                            }
                        },
                    )
                }
            }
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
            setTitle(lang.settings.tracks)
            setHeaderAction(Action.BACK)
//            setItemList(trackItems)
            setItemList(parkingItems)
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
                    addAction(
                        action {
                            setTitle("Parking")
                            setOnClickListener {
                                service.searchParkings(currentLocation)
                            }
                        },
                    )
                },
            )

//            setOnContentRefreshListener {
//                Timber.i("Refresh!")
//                invalidate()
//            }
        }
    }
}
