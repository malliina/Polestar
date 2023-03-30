package com.skogberglabs.polestar

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ProfileViewModel {
    companion object {
        val instance = ProfileViewModel()
    }

    private val userState: MutableStateFlow<UserInfo?> = MutableStateFlow(null)
    val user: StateFlow<UserInfo?> = userState

    fun update(user: UserInfo?) {
        userState.value = user
    }
}
