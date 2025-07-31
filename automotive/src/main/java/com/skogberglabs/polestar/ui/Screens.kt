package com.skogberglabs.polestar.ui

import android.content.Intent
import androidx.activity.OnBackPressedCallback
import androidx.car.app.CarContext
import androidx.car.app.CarToast
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.Template
import com.skogberglabs.polestar.action
import com.skogberglabs.polestar.actionStrip
import com.skogberglabs.polestar.itemList
import com.skogberglabs.polestar.mapTemplate
import com.skogberglabs.polestar.messageTemplate
import com.skogberglabs.polestar.navigationTemplate
import com.skogberglabs.polestar.pane
import com.skogberglabs.polestar.paneTemplate
import com.skogberglabs.polestar.placeListNavigationTemplate
import com.skogberglabs.polestar.row
import timber.log.Timber

class EmptyScreen(carContext: CarContext) : Screen(carContext) {
    override fun onGetTemplate(): Template {
        Screens.installProfileRootBackBehavior(this)
        return messageTemplate("?") {
            setHeaderAction(Action.BACK)
        }
    }
}

object Screens {
    fun installProfileRootBackBehavior(screen: Screen) {
        val carContext = screen.carContext
        val screenManager = screen.screenManager
        val backCallback =
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (screenManager.stackSize == 1) {
                        val i =
                            Intent(carContext, GoogleSignInActivity::class.java).apply {
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
        val myItems =
            itemList {
                addItem(
                    row {
                        setTitle("My places")
                        setBrowsable(true)
                        setOnClickListener {
//                        screenManager.pushLogged(NoPermissionScreen(carContext, PermissionContent.allForeground))
                        }
                    },
                )
            }
        return placeListNavigationTemplate {
            setHeaderAction(Action.APP_ICON)
            setItemList(myItems)
        }
    }
}

class MapScreen(carContext: CarContext) : Screen(carContext) {
    override fun onGetTemplate(): Template =
        mapTemplate {
            setPane(
                pane {
                    addAction(Action.BACK)
                    addRow(
                        row {
                            setTitle("This shows a map.")
                        },
                    )
                },
            )
        }
}

class ParkingScreen(carContext: CarContext) : Screen(carContext) {
    override fun onGetTemplate(): Template {
        return paneTemplate(
            pane {
                addRow(
                    row {
                        setTitle("Testing")
                    },
                )
            },
        ) {
            setTitle("This is a parking template")
            setHeaderAction(Action.BACK)
        }
    }
}

class CustomScreen(carContext: CarContext) : Screen(carContext) {
    override fun onGetTemplate(): Template =
        paneTemplate(
            pane {
                addRow(
                    row {
                        setTitle("Hej")
                    },
                )
                addAction(
                    action {
                        setTitle("Primary action")
                        setOnClickListener {
                            CarToast.makeText(carContext, "Clicked action", CarToast.LENGTH_SHORT).show()
                        }
                        setFlags(Action.FLAG_PRIMARY)
                    },
                )
                addAction(
                    action {
                        setTitle("Secondary action")
                        setOnClickListener {
                            CarToast.makeText(carContext, "Clicked secondary action", CarToast.LENGTH_SHORT).show()
                        }
                    },
                )
                addRow(
                    row {
                        setTitle("Hej again")
                    },
                )
            },
        ) {
            setTitle("This is a pane template")
            setHeaderAction(Action.BACK)
        }
}

class NavigationScreen(carContext: CarContext) : Screen(carContext) {
    override fun onGetTemplate(): Template =
        navigationTemplate {
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
                        },
                    )
                },
            )
            setPanModeListener { isPan ->
                Timber.i("Pan $isPan")
            }
        }
}
