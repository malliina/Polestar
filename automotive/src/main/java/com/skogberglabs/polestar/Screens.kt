package com.skogberglabs.polestar

import android.Manifest
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.CarColor
import androidx.car.app.model.CarIcon
import androidx.car.app.model.CarLocation
import androidx.car.app.model.ParkedOnlyOnClickListener
import androidx.car.app.model.PlaceMarker
import androidx.car.app.model.Template
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import timber.log.Timber

@androidx.annotation.OptIn(androidx.car.app.annotations.ExperimentalCarApi::class)
class PlacesScreen(carContext: CarContext) : Screen(carContext) {
    val scope = CoroutineScope(Dispatchers.IO)
    var currentLocation: CarLocation = CarLocation.create(60.155, 24.877)
    val locationJob = scope.launch {
        LocationSource.instance.locationUpdates.collect { updates ->
            updates.lastOrNull()?.let { last ->
                Timber.i("Updating current location...")
                currentLocation = CarLocation.create(last.latitude, last.longitude)
                invalidate()
            }
        }
    }
    override fun onGetTemplate(): Template {
        val myItems = itemList {
            addItem(
                row {
                    setTitle("Places")
                    setBrowsable(true)
                    setOnClickListener {
                        screenManager.push(NoPermissionScreen(carContext))
                    }
                }
            )
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
            setHeaderAction(Action.APP_ICON)
            setItemList(myItems)
            setCurrentLocationEnabled(true)
            setAnchor(myPlace)
            setActionStrip(actionStrip { addAction(Action.PAN) })
            setOnContentRefreshListener {
                Timber.i("Refresh!")
                invalidate()
            }
        }
    }
}

class PlaceNavScreen(carContext: CarContext) : Screen(carContext) {
    override fun onGetTemplate(): Template {
        val myItems = itemList {
            addItem(
                row {
                    setTitle("My places")
                    setBrowsable(true)
                    setOnClickListener {
                        screenManager.push(NoPermissionScreen(carContext))
                    }
                }
            )
        }
        return placeListNavigationTemplate {
            setHeaderAction(Action.APP_ICON)
            setItemList(myItems)
        }
    }
}

class MapScreen(carContext: CarContext) : Screen(carContext) {
    override fun onGetTemplate(): Template = navigationTemplate {
        setBackgroundColor(CarColor.GREEN)
        setActionStrip(
            actionStrip {
                addAction(Action.PAN)
                addAction(
                    action {
                        setTitle("Action here")
                        setOnClickListener {
                            Timber.i("Clicked action.")
                        }
                    }
                )
            }
        )
        setPanModeListener { isPan ->
            Timber.i("Pan $isPan")
        }
    }
}

class RequestPermissionScreen(carContext: CarContext, private val onGranted: () -> Unit) :
    Screen(carContext) {
    override fun onGetTemplate(): Template {
        val pocl = ParkedOnlyOnClickListener.create {
            carContext.requestPermissions(
                listOf(Manifest.permission.ACCESS_FINE_LOCATION)
            ) { grantedPermissions, rejectedPermissions ->
                if (grantedPermissions.isNotEmpty()) {
                    onGranted()
                }
            }
        }

        val myAction = action {
            setTitle("Grant access.")
            setOnClickListener(pocl)
        }
        return messageTemplate("Location usage explanation.") {
            addAction(myAction)
            setHeaderAction(Action.APP_ICON)
        }
    }
}

class NoPermissionScreen(carContext: CarContext) : Screen(carContext) {
    override fun onGetTemplate(): Template {
        val row = row {
            setTitle("Please grant permission to use location.")
        }
        val pane = pane {
            addRow(row)
            addAction(
                action {
                    setTitle("Try again")
                    setOnClickListener {
                        Timber.i("Trying again...")
                        screenManager.push(RequestPermissionScreen(carContext, onGranted = { screenManager.popToRoot() }))
                    }
                }
            )
        }
        return paneTemplate(pane) {
            setHeaderAction(Action.APP_ICON)
        }
    }
}
