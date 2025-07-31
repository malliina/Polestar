package com.skogberglabs.polestar.ui

import androidx.car.app.Screen
import androidx.car.app.ScreenManager
import timber.log.Timber

fun Double.formatted(n: Int): String = String.format("%.${n}f", this)

fun Float.formatted(n: Int): String = String.format("%.${n}f", this)

fun ScreenManager.pushLogged(screen: Screen) {
    Timber.i("Pushing $screen...")
    push(screen)
}
