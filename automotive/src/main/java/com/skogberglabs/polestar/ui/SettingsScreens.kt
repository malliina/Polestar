package com.skogberglabs.polestar.ui

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.Template
import com.skogberglabs.polestar.AppService
import com.skogberglabs.polestar.BuildConfig
import com.skogberglabs.polestar.CarLang
import com.skogberglabs.polestar.addRow
import com.skogberglabs.polestar.itemList
import com.skogberglabs.polestar.listTemplate
import com.skogberglabs.polestar.row
import kotlinx.coroutines.launch
import timber.log.Timber

class SettingsScreen(
    carContext: CarContext,
    private val lang: CarLang,
    private val service: AppService,
) : Screen(carContext) {
    override fun onGetTemplate(): Template {
        val locations = service.locationSource
        val locationStatus =
            when (locations.locationServicesAvailable.value) {
                true -> "Location services are available."
                false -> "Location services are not available."
                null -> "Location services availability not obtained."
            }
        val currentStatus =
            locations.currentLocation.value?.let { current ->
                "Latest location lat ${current.latitude} lon ${current.longitude} at ${current.date}."
            } ?: "No location obtained."
        return listTemplate {
            setTitle(lang.settings.title)
            val list =
                itemList {
                    addRow {
                        setTitle(lang.settings.selectCar)
                        setOnClickListener {
                            screenManager.push(SelectCarScreen(carContext, lang, service))
                        }
                    }
                    addRow {
                        setTitle(lang.profile.chooseLanguage)
                        setOnClickListener {
                            screenManager.push(SelectLanguageScreen(carContext, lang, service))
                        }
                    }
                    addRow {
                        setTitle(lang.profile.signOut)
                        setOnClickListener {
                            service.mainScope.launch {
                                service.google.signOut()
                                // .pop() crashes the app since the AllGoodScreen template has changed since last time
//                            screenManager.pop()
                                screenManager.push(HomeScreen(carContext, service))
                            }
                        }
                    }
                    addRow {
                        setTitle("${lang.profile.version} ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                    }
                    addRow {
                        setTitle(locationStatus)
                    }
                    addRow {
                        setTitle(currentStatus)
                    }
                }
            setSingleList(list)
            setHeaderAction(Action.BACK)
        }
    }
}

class SelectLanguageScreen(
    carContext: CarContext,
    val lang: CarLang,
    private val service: AppService,
) : Screen(carContext) {
    override fun onGetTemplate(): Template {
        return listTemplate {
            setTitle(lang.profile.chooseLanguage)
            val list =
                itemList {
                    service.languagesLatest().forEach { lang ->
                        addRow { setTitle(lang.name) }
                    }
                    setOnSelectedListener { v ->
                        val selected = service.languagesLatest()[v]
                        service.saveLanguage(selected.code)
                        Timber.i("Selected language $selected")
                    }
                    val idx = service.languagesLatest().indexOfFirst { l -> l.code == service.currentLanguage() }
                    if (idx >= 0) {
                        setSelectedIndex(idx)
                    }
                }
            setSingleList(list)
            setHeaderAction(Action.BACK)
        }
    }
}

class SelectCarScreen(
    carContext: CarContext,
    val lang: CarLang,
    private val service: AppService,
) : Screen(carContext) {
    init {
        service.mainScope.launch {
            service.profile.collect {
                invalidate()
            }
        }
    }

    private fun cars() = service.profileLatest()?.user?.boats ?: emptyList()

    override fun onGetTemplate(): Template {
        val idx = cars().indexOfFirst { car -> car.id == service.profileLatest()?.activeCar?.id }
        val hasSelected = idx >= 0
        return listTemplate {
            setTitle(lang.settings.selectCar)
            val list =
                itemList {
                    if (cars().isNotEmpty()) {
                        cars().forEach { car ->
                            addItem(
                                row {
                                    setTitle(car.name)
                                    if (!hasSelected) {
                                        setOnClickListener {
                                            service.selectCar(car.id)
                                            Timber.i("Clicked car ${car.id} (${car.name})")
                                            screenManager.pop()
                                        }
                                    }
                                },
                            )
                        }
                        if (hasSelected) {
                            setOnSelectedListener { v ->
                                val selected = cars()[v]
                                service.selectCar(selected.id)
                                Timber.i("Selected car ${selected.name}")
                            }
                        }
                    } else {
                        setNoItemsMessage(lang.settings.noCars)
                    }
                    if (hasSelected) {
                        setSelectedIndex(idx)
                    }
                }
            setSingleList(list)
            setHeaderAction(Action.BACK)
        }
    }
}
