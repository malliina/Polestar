package com.skogberglabs.polestar.ui

import androidx.annotation.OptIn
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.annotations.ExperimentalCarApi
import androidx.car.app.model.CarIcon
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.MapController
import com.skogberglabs.polestar.action
import com.skogberglabs.polestar.actionStrip
import com.skogberglabs.polestar.addRow
import com.skogberglabs.polestar.installHeader
import com.skogberglabs.polestar.itemList
import com.skogberglabs.polestar.listTemplate
import com.skogberglabs.polestar.mapWithContentTemplate
import timber.log.Timber

class MapContentScreen(carContext: CarContext) : Screen(carContext) {
    @OptIn(ExperimentalCarApi::class)
    override fun onGetTemplate(): Template {
        val mapController =
            MapController.Builder()
                .setMapActionStrip(
                    actionStrip {
                        addAction(
                            action {
                                setIcon(CarIcon.APP_ICON)
//                            setTitle("Hej")
                                setOnClickListener {
                                    Timber.i("Click")
                                }
                            },
                        )
                    },
                )
                .setPanModeListener { isPan ->
                    Timber.i("Pan $isPan")
                }
                .build()
        return mapWithContentTemplate {
            setMapController(mapController)
            setContentTemplate(
                listTemplate {
                    installHeader {
                        setTitle("List here")
                    }
                    setSingleList(
                        itemList {
                            addRow {
                                setTitle("Row 1")
                                setOnClickListener {
                                    Timber.i("Clicked row")
                                }
                            }
                        },
                    )
                },
            )
        }
    }
}
