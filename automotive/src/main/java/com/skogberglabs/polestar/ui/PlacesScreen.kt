package com.skogberglabs.polestar.ui

import android.text.SpannableString
import android.text.Spanned
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.annotations.ExperimentalCarApi
import androidx.car.app.model.Action
import androidx.car.app.model.CarColor
import androidx.car.app.model.CarIcon
import androidx.car.app.model.CarLocation
import androidx.car.app.model.Distance
import androidx.car.app.model.DistanceSpan
import androidx.car.app.model.PlaceMarker
import androidx.car.app.model.Template
import com.skogberglabs.polestar.CarLang
import com.skogberglabs.polestar.Coord
import com.skogberglabs.polestar.Track
import com.skogberglabs.polestar.actionStrip
import com.skogberglabs.polestar.itemList
import com.skogberglabs.polestar.location.LocationSourceInterface
import com.skogberglabs.polestar.place
import com.skogberglabs.polestar.placeListTemplate
import com.skogberglabs.polestar.placeMarker
import com.skogberglabs.polestar.row
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.time.Duration.Companion.seconds

@androidx.annotation.OptIn(ExperimentalCarApi::class)
class PlacesScreen(carContext: CarContext, locationSource: LocationSourceInterface, private val tracks: List<Track>, private val lang: CarLang) : Screen(carContext) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val latestCoord = locationSource.locationLatest() ?: tracks.firstOrNull()?.topPoint?.coord ?: Coord(60.155, 24.877)
    private var currentLocation: CarLocation = CarLocation.create(latestCoord.lat, latestCoord.lng)

    @OptIn(FlowPreview::class)
    val locationJob = scope.launch {
        locationSource.locationUpdates.debounce(10.seconds).collect { updates ->
            updates.lastOrNull()?.let { last ->
                Timber.i("Updating current location...")
//                updateLocation(CarLocation.create(last.latitude, last.longitude))
            }
        }
    }

    private fun updateLocation(loc: CarLocation) {
        currentLocation = loc
        invalidate()
    }

    override fun onGetTemplate(): Template {
        val interpunct = "\u00b7"
        val myItems = itemList {
            setNoItemsMessage(lang.settings.noTracks)
            tracks.forEach { track ->
                addItem(
                    row {
                        val span = DistanceSpan.create(Distance.create(track.distanceMeters.kilometers, Distance.UNIT_KILOMETERS))
                        val str = SpannableString("  $interpunct ${track.trackName}")
                        str.setSpan(span, 0, 1, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
                        setTitle(str)
                        setBrowsable(false)
                        setOnClickListener {
                            val top = track.topPoint.coord
                            updateLocation(CarLocation.create(top.lat, top.lng))
                        }
                    }
                )
            }
        }
        val myPlace = place(currentLocation) {
            setMarker(
                placeMarker {
                    setIcon(CarIcon.APP_ICON, PlaceMarker.TYPE_ICON)
                    setColor(CarColor.BLUE)
                }
            )
        }
        return placeListTemplate {
            setTitle(lang.settings.tracks)
            setHeaderAction(Action.BACK)
            setItemList(myItems)
            setCurrentLocationEnabled(true)
            setAnchor(myPlace)
            setActionStrip(actionStrip { addAction(Action.PAN) })
//            setOnContentRefreshListener {
//                Timber.i("Refresh!")
//                invalidate()
//            }
        }
    }
}
