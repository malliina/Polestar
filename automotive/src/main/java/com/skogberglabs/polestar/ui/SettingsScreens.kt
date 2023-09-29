package com.skogberglabs.polestar.ui

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.Template
import com.skogberglabs.polestar.Google
import com.skogberglabs.polestar.itemList
import com.skogberglabs.polestar.listTemplate
import com.skogberglabs.polestar.row
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

class SettingsScreen(carContext: CarContext,
                     private val google: Google,
                     private val mainScope: CoroutineScope): Screen(carContext) {
    override fun onGetTemplate(): Template {
        return listTemplate {
            setTitle("Settings")
            val list = itemList {
                addItem(row {
                    setTitle("Car")
                    setOnClickListener {
                        screenManager.push(SelectCarScreen(carContext))
                    }
                })
                addItem(row {
                    setTitle("Language")
                    setOnClickListener {
                        screenManager.push(SelectLanguageScreen(carContext))
                    }
                })
                addItem(row {
                    setTitle("Sign out")
                    setOnClickListener {
                        mainScope.launch {
                            google.signOut()
                            screenManager.pop()
                        }
                    }
                })
            }
            setSingleList(list)
            setHeaderAction(Action.BACK)
        }
    }
}

class SelectLanguageScreen(carContext: CarContext): Screen(carContext) {
    override fun onGetTemplate(): Template {
        return listTemplate {
            setTitle("Select language")
            val list = itemList {
                addItem(row { setTitle("Svenska") })
                addItem(row { setTitle("Suomeksi") })
                setOnSelectedListener { v ->
                    Timber.i("Selected index $v")
                }
            }
            setSingleList(list)
            setHeaderAction(Action.BACK)
        }
    }
}

class SelectCarScreen(carContext: CarContext): Screen(carContext) {
    private val cars = listOf("Amina", "Mos")

    override fun onGetTemplate(): Template {
        return listTemplate {
            setTitle("Select car")
            val list = itemList {
                if (cars.isNotEmpty()) {
                    cars.forEach { car ->
                        addItem(row { setTitle(car) })
                    }
                    setOnSelectedListener { v ->
                        Timber.i("Selected index $v")
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
