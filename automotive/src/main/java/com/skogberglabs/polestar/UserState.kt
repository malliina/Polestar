package com.skogberglabs.polestar

import androidx.core.graphics.drawable.IconCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class UserState {
    companion object {
        val instance = UserState()
    }

    private val flow: MutableStateFlow<Outcome<UserInfo>> = MutableStateFlow(Outcome.Idle)

    val userResult: StateFlow<Outcome<UserInfo>> = flow
    val isSuccess: Boolean get() = userResult.value.isSuccess()
    fun update(outcome: Outcome<UserInfo>) {
        flow.value = outcome
    }
}

data class ProfileInfo(val user: ApiUserInfo, val carId: String?, val localCarImage: IconCompat?) {
    val email = user.email
    val activeCar = user.boats.find { car -> car.idStr == carId }
    val hasCars = user.boats.isNotEmpty()
    val cars = user.cars
}
