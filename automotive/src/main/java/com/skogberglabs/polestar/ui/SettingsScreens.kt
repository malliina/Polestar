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
                     private val profile: ProfileInfo,
                     private val languages: List<CarLanguage>,
                     private val google: Google,
                     private val mainScope: CoroutineScope): Screen(carContext) {
    override fun onGetTemplate(): Template {
        return listTemplate {
            setTitle("Settings")
            val list = itemList {
                addRow {
                    setTitle("Car")
                    setOnClickListener {
                        screenManager.push(SelectCarScreen(carContext, profile.user.boats))
                    }
                }
                addRow {
                    setTitle("Language")
                    setOnClickListener {
                        screenManager.push(SelectLanguageScreen(carContext, languages))
                    }
                }
                addRow {
                    setTitle("Sign out")
                    setOnClickListener {
                        mainScope.launch {
                            google.signOut()
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

class SelectLanguageScreen(carContext: CarContext, private val languages: List<CarLanguage>): Screen(carContext) {
    override fun onGetTemplate(): Template {
        return listTemplate {
            setTitle("Select language")
            val list = itemList {
                languages.forEach { lang ->
                    addRow { setTitle(lang.name) }
                }
                setOnSelectedListener { v ->
                    val selected = languages[v]
                    Timber.i("Selected language $selected")
                }
            }
            setSingleList(list)
            setHeaderAction(Action.BACK)
        }
    }
}

class SelectCarScreen(carContext: CarContext, private val cars: List<CarInfo>): Screen(carContext) {
    override fun onGetTemplate(): Template {
        return listTemplate {
            setTitle("Select car")
            val list = itemList {
                if (cars.isNotEmpty()) {
                    cars.forEach { car ->
                        addItem(row { setTitle(car.name) })
                    }
                    setOnSelectedListener { v ->
                        val selected = cars[v]
                        Timber.i("Selected car ${selected.name}")
                    }
                } else {
                    setNoItemsMessage("No cars.")
                }
            }
            setSingleList(list)
            setHeaderAction(Action.BACK)
        }
    }
}
