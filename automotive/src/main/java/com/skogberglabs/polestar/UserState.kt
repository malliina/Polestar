package com.skogberglabs.polestar

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class UserState {
    companion object {
        val instance = UserState()
    }

    private val current: MutableStateFlow<Outcome<UserInfo>> = MutableStateFlow(Outcome.Idle)
    val userResult: StateFlow<Outcome<UserInfo>> = current
    fun latest() = userResult.value.toOption()

    fun update(outcome: Outcome<UserInfo>) {
        current.value = outcome
    }
}

data class ProfileInfo(val user: ApiUserInfo, val carId: String?) {
    val activeCar = user.boats.firstOrNull { car -> car.id == carId }
    val hasCars = user.boats.isNotEmpty()
}
