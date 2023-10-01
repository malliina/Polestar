package com.skogberglabs.polestar.ui

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.Template
import com.skogberglabs.polestar.CarInfo
import com.skogberglabs.polestar.CarLanguage
import com.skogberglabs.polestar.Google
import com.skogberglabs.polestar.ProfileInfo
import com.skogberglabs.polestar.addRow
import com.skogberglabs.polestar.itemList
import com.skogberglabs.polestar.listTemplate
import com.skogberglabs.polestar.row
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

class SettingsScreen(carContext: CarContext,
                     private val service: AppService): Screen(carContext) {
    override fun onGetTemplate(): Template {
        return listTemplate {
            setTitle("Settings")
            val list = itemList {
                addRow {
                    setTitle("Car")
                    setOnClickListener {
                        screenManager.push(SelectCarScreen(carContext, service))
                    }
                }
                addRow {
                    setTitle("Language")
                    setOnClickListener {
                        screenManager.push(SelectLanguageScreen(carContext, service))
                    }
                }
                addRow {
                    setTitle("Sign out")
                    setOnClickListener {
                        service.mainScope.launch {
                            service.google.signOut()
                            screenManager.pop()
                        }
                    }
                }
            }
            setSingleList(list)
            setHeaderAction(Action.BACK)
        }
    }
}

class SelectLanguageScreen(carContext: CarContext, private val service: AppService): Screen(carContext) {
    override fun onGetTemplate(): Template {
        return listTemplate {
            setTitle("Select language")
            val list = itemList {
                service.languagesLatest().forEach { lang ->
                    addRow { setTitle(lang.name) }
                }
                setOnSelectedListener { v ->
                    val selected = service.languagesLatest()[v]
                    service.saveLanguage(selected.code)
                    Timber.i("Selected language $selected")
                }
                setSelectedIndex(service.languagesLatest().indexOfFirst { l -> l.code == service.currentLanguage() } )
            }
            setSingleList(list)
            setHeaderAction(Action.BACK)
        }
    }
}

class SelectCarScreen(carContext: CarContext, private val service: AppService): Screen(carContext) {
    init {
        service.mainScope.launch {
            service.profile.collect {
                invalidate()
            }
        }
    }
    private fun cars() = service.profileLatest()?.user?.boats ?: emptyList()

    override fun onGetTemplate(): Template {
        return listTemplate {
            setTitle("Select car")
            val list = itemList {
                if (cars().isNotEmpty()) {
                    cars().forEach { car ->
                        addItem(row { setTitle(car.name) })
                    }
                    setOnSelectedListener { v ->
                        val selected = cars()[v]
                        service.selectCar(selected.id)
                        Timber.i("Selected car ${selected.name}")
                    }
                } else {
                    setNoItemsMessage("No cars.")
                }
                setSelectedIndex(cars().indexOfFirst { car -> car.id == service.profileLatest()?.activeCar?.id })
            }
            setSingleList(list)
            setHeaderAction(Action.BACK)
        }
    }
}
