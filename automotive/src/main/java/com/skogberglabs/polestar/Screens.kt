package com.skogberglabs.polestar

import android.content.Intent
import androidx.activity.OnBackPressedCallback
import androidx.car.app.CarContext
import androidx.car.app.CarToast
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.CarColor
import androidx.car.app.model.CarIcon
import androidx.car.app.model.CarLocation
import androidx.car.app.model.PlaceMarker
import androidx.car.app.model.Template
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.time.Duration.Companion.seconds

@androidx.annotation.OptIn(androidx.car.app.annotations.ExperimentalCarApi::class)
class PlacesScreen(carContext: CarContext, locationSource: LocationSource) : Screen(carContext) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var currentLocation: CarLocation = CarLocation.create(60.155, 24.877)
    @OptIn(FlowPreview::class)
    val locationJob = scope.launch {
        locationSource.locationUpdates.debounce(10.seconds).collect { updates ->
            updates.lastOrNull()?.let { last ->
                Timber.i("Updating current location...")
                currentLocation = CarLocation.create(last.latitude, last.longitude)
                invalidate()
            }
        }
    }
    override fun onGetTemplate(): Template {
        Screens.installProfileRootBackBehavior(this)
        val myItems = itemList {
            addItem(
                row {
                    setTitle("Navigation only")
                    setBrowsable(true)
                    setOnClickListener {
                        screenManager.push(NavigationScreen(carContext))
                    }
                }
            )
            addItem(
                row {
                    setTitle("Map only")
                    setBrowsable(true)
                    setOnClickListener {
                        screenManager.push(MapScreen(carContext))
                    }
                }
            )
            addItem(
                row {
                    setTitle("Custom screen")
                    setBrowsable(true)
                    setOnClickListener {
                        screenManager.push(CustomScreen(carContext))
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
            setHeaderAction(Action.BACK)
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

object Screens {
    fun installProfileRootBackBehavior(screen: Screen) {
        val carContext = screen.carContext
        val screenManager = screen.screenManager
        val backCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (screenManager.stackSize == 1) {
                    val i = Intent(carContext, CarActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    carContext.startActivity(i)
                } else {
                    screenManager.pop()
                }
            }
        }
        carContext.onBackPressedDispatcher.addCallback(backCallback)
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
                        screenManager.push(NoPermissionScreen(carContext, PermissionContent.all))
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
    override fun onGetTemplate(): Template = mapTemplate {
        setPane(
            pane {
                addAction(Action.BACK)
                addRow(
                    row {
                        setTitle("This shows a map.")
                    }
                )
            }
        )
    }
}

class CustomScreen(carContext: CarContext) : Screen(carContext) {
    override fun onGetTemplate(): Template = paneTemplate(pane {
        addRow(row {
            setTitle("Hej")
        })
        addAction(
            action {
                setTitle("Primary action")
                setOnClickListener {
                    CarToast.makeText(carContext, "Clicked action", CarToast.LENGTH_SHORT).show()
                }
                setFlags(Action.FLAG_PRIMARY)
            }
        )
        addAction(
            action {
                setTitle("Secondary action")
                setOnClickListener {
                    CarToast.makeText(carContext, "Clicked secondary action", CarToast.LENGTH_SHORT).show()
                }
            }
        )
        addRow(row {
            setTitle("Hej again")
        })
    }) {
        setTitle("This is a pane template")
        setHeaderAction(Action.BACK)
    }
}

class NavigationScreen(carContext: CarContext) : Screen(carContext) {
    override fun onGetTemplate(): Template = navigationTemplate {
        setActionStrip(
            actionStrip {
                addAction(Action.BACK)
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
