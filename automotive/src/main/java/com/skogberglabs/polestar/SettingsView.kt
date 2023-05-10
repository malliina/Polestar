package com.skogberglabs.polestar

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsView(vm: ProfileViewModelInterface, navController: NavController) {
    val profile by vm.profile.collectAsStateWithLifecycle(Outcome.Idle)
    val carId = profile.toOption()?.activeCar?.id
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { TitleText("Settings") },
                modifier = Modifier.padding(Paddings.normal),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }, modifier = Modifier.size(64.dp)) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            )
        }
    ) { pd ->
        when (val profileOutcome = profile) {
            is Outcome.Success -> {
                profileOutcome.result?.let { p ->
                    if (p.hasCars) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(Paddings.xxl)
                                .padding(pd)) {
                            ReadableText("Select car", Modifier.padding(vertical = Paddings.large))
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 328.dp),
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(Paddings.small)
                            ) {
                                items(p.user.boats) { boat ->
                                    Box(
                                        Modifier
                                            .border(
                                                if (boat.id == carId) 8.dp else 2.dp,
                                                Color.Blue
                                            )
                                            .sizeIn(minWidth = 420.dp, minHeight = 128.dp)
                                            .clickable { vm.selectCar(boat.id) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            boat.name,
                                            style = MaterialTheme.typography.titleLarge,
                                            textAlign = TextAlign.Center,
                                            fontSize = 32.sp
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        ReadableText("No cars.", Modifier.padding(pd))
                    }
                }
            }
            Outcome.Loading -> CarProgressBar(Modifier.padding(pd))
            is Outcome.Error -> ErrorText("Failed to load profile.", Modifier.padding(pd))
            Outcome.Idle -> ErrorText("Nothing to see here.", Modifier.padding(pd))
        }
    }
}

@Preview
@Composable
fun SettingsPreview() {
    SettingsView(ProfileViewModelInterface.preview, rememberNavController())
}