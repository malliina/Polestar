package com.skogberglabs.polestar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.skogberglabs.polestar.ui.CarProgressBar
import com.skogberglabs.polestar.ui.ProfileView
import com.skogberglabs.polestar.ui.CarViewModelInterface
import com.skogberglabs.polestar.ui.SettingsView

object NavRoutes {
    const val PROFILE = "profile"
    const val SETTINGS = "settings"
}

@Composable
fun CarNavGraph(vm: CarViewModelInterface,
                onSignIn: () -> Unit,
                navController: NavHostController,
                start: String = NavRoutes.PROFILE) {
    val confOutcome by vm.conf.collectAsStateWithLifecycle(initialValue = Outcome.Idle)
    when (val conf = confOutcome) {
        is Outcome.Success -> {
            val lang = conf.result
            NavHost(navController = navController, startDestination = start) {
                composable(NavRoutes.PROFILE) {
                    ProfileView(lang, vm, navController, onSignIn)
                }
                composable(NavRoutes.SETTINGS) {
                    SettingsView(lang, vm, navController)
                }
            }
        }
        else -> CarProgressBar()
    }
}
