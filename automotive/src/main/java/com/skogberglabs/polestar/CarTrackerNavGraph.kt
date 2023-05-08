package com.skogberglabs.polestar

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

object NavRoutes {
    const val PROFILE = "profile"
    const val SETTINGS = "settings"
}

@Composable
fun CarTrackerNavGraph(vm: ProfileViewModelInterface,
                       onSignIn: () -> Unit,
                       navController: NavHostController,
                       start: String = NavRoutes.PROFILE) {
    NavHost(navController = navController, startDestination = start) {
        composable(NavRoutes.PROFILE) {
            ProfileView(vm, navController, onSignIn)
        }
        composable(NavRoutes.SETTINGS) {
            SettingsView(vm, navController)
        }
    }
}
