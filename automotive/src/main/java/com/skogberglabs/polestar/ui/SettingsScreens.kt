package com.skogberglabs.polestar.ui

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.Template
import com.skogberglabs.polestar.AppService
import com.skogberglabs.polestar.BuildConfig
import com.skogberglabs.polestar.CarLang
import com.skogberglabs.polestar.CarRef
import com.skogberglabs.polestar.Outcome
import com.skogberglabs.polestar.UserPreferences
import com.skogberglabs.polestar.addRow
import com.skogberglabs.polestar.itemList
import com.skogberglabs.polestar.listTemplate
import com.skogberglabs.polestar.row
import kotlinx.coroutines.flow.drop
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
        val uploadStatus =
            when (val s = service.locationUploader.status.value) {
                is Outcome.Error -> "Uploader failed with ${s.e}."
                Outcome.Idle -> "Uploader is idle."
                Outcome.Loading -> "Uploader is loading."
                is Outcome.Success -> "Uploaded locations, got message: '${s.result.message}'."
            }
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
                            screenManager.push(SelectLanguageScreen(carContext, service.prefs.value, service))
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
                    addRow {
                        setTitle(uploadStatus)
                    }
                }
            setSingleList(list)
            setHeaderAction(Action.BACK)
        }
    }
}

class SelectLanguageScreen(
    carContext: CarContext,
    initialPrefs: UserPreferences,
    private val service: AppService,
) : Screen(carContext) {
    private var current: UserPreferences

    init {
        current = initialPrefs
        service.mainScope.launch {
            service.prefs.drop(1).collect { prefs ->
                current = prefs
                invalidate()
            }
        }
    }

    private val langs get() = current.carConf?.languages?.map { it.language } ?: emptyList()

    override fun onGetTemplate(): Template {
        return listTemplate {
            current.lang?.let { lang ->
                setTitle(lang.profile.chooseLanguage)
            }
            val list =
                itemList {
                    langs.forEach { lang ->
                        addRow { setTitle(lang.name) }
                    }
                    setOnSelectedListener { v ->
                        val selected = langs[v]
                        service.saveLanguage(selected.code)
                        Timber.i("Selected language $selected")
                    }
                    val idx = langs.indexOfFirst { l -> l.code == current.language }
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
            service.profile.drop(1).collect {
                Timber.i("Invalidating select car screen...")
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
                                            service.selectCar(CarRef(car.idStr, car.token))
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
                                val ref = CarRef(selected.idStr, selected.token)
                                service.selectCar(ref)
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
