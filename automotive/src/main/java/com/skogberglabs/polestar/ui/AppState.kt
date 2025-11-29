package com.skogberglabs.polestar.ui

import com.skogberglabs.polestar.CarLang
import com.skogberglabs.polestar.ProfileInfo

sealed class AppState {
    data class LoggedIn(val user: ProfileInfo, val lang: CarLang) : AppState()

    data class Anon(val lang: CarLang) : AppState()

    data class Loading(val lang: CarLang?) : AppState()

    fun carLang(): CarLang? =
        when (this) {
            is Anon -> lang
            is LoggedIn -> lang
            is Loading -> lang
        }
}
