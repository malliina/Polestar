package com.skogberglabs.polestar

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.Intent
import android.content.pm.PackageManager
import androidx.car.app.CarAppService
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.ScreenManager
import androidx.car.app.Session
import androidx.car.app.model.Action
import androidx.car.app.model.CarLocation
import androidx.car.app.model.ItemList
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.ParkedOnlyOnClickListener
import androidx.car.app.model.Place
import androidx.car.app.model.PlaceListMapTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.car.app.validation.HostValidator

class PolestarCarAppService : CarAppService() {
    override fun createHostValidator(): HostValidator = HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    override fun onCreateSession(): Session = PolestarSession()
}

class PolestarSession : Session() {
    override fun onCreateScreen(intent: Intent): Screen {
        val isGranted =
            carContext.checkSelfPermission(ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return if (isGranted) {
            PolestarScreen(carContext)
        } else {
            val sm = carContext.getCarService(ScreenManager::class.java)
            sm.push(PolestarScreen(carContext))
            RequestPermissionScreen(carContext, onGranted = { sm.pop() })
        }
    }
}

class PolestarScreen(carContext: CarContext) : Screen(carContext) {
    override fun onGetTemplate(): Template {
        val itemList = ItemList.Builder()
            .addItem(
                Row.Builder().setTitle("Hej").setBrowsable(true).setOnClickListener {
                    screenManager.push(NoPermissionScreen(carContext))
                }.build()
            ).build()
        val place = Place.Builder(CarLocation.create(60.155, 24.877)).build()
        return PlaceListMapTemplate.Builder().setHeaderAction(Action.APP_ICON).setItemList(itemList)
            .setCurrentLocationEnabled(true)
            .setAnchor(place)
            .build()
    }
}

class RequestPermissionScreen(carContext: CarContext, private val onGranted: () -> Unit) :
    Screen(carContext) {
    override fun onGetTemplate(): Template {
        val pocl = ParkedOnlyOnClickListener.create {
            carContext.requestPermissions(
                listOf(ACCESS_FINE_LOCATION)
            ) { grantedPermissions, rejectedPermissions ->
                if (grantedPermissions.isNotEmpty()) {
                    onGranted()
                }
            }
        }

        val action = Action.Builder().setTitle("Grant access.").setOnClickListener(pocl).build()
        return MessageTemplate.Builder("Location usage explanation.").addAction(action)
            .setHeaderAction(Action.APP_ICON).build()
    }
}

class NoPermissionScreen(carContext: CarContext) : Screen(carContext) {
    override fun onGetTemplate(): Template {
        val row = Row.Builder().setTitle("Please grant permission to use location.").build()
        val pane = Pane.Builder().addRow(row).build()
        return PaneTemplate.Builder(pane).setHeaderAction(Action.APP_ICON).build()
    }
}
