package com.skogberglabs.polestar.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.skogberglabs.polestar.CarLang
import com.skogberglabs.polestar.Outcome
import com.skogberglabs.polestar.Paddings

@Composable
fun CheckIcon(visible: Boolean = false) {
    Icon(
        imageVector = Icons.Filled.Check,
        contentDescription = "Select",
        modifier = Modifier
            .size(36.dp)
            .alpha(if (visible) 1f else 0f)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsView(lang: CarLang, vm: CarViewModelInterface, navController: NavController) {
    val profile by vm.profile.collectAsStateWithLifecycle(Outcome.Idle)
    val languages by vm.languages.collectAsStateWithLifecycle(emptyList())
    val savedLanguage by vm.savedLanguage.collectAsStateWithLifecycle(null)
    val user by vm.user.collectAsStateWithLifecycle()
    val isSignedIn = user.toOption() != null

    val carId = profile.toOption()?.activeCar?.id
    val plang = lang.profile
    val slang = lang.settings
    Scaffold(
        topBar = {
            CarTopAppBar(slang.title, navigationIcon = {
                CarIconButton(
                    onClick = { navController.popBackStack() },
                    image = Icons.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            })
        }
    ) { pd ->
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Paddings.xxl)
                .padding(pd)) {
            when (val profileOutcome = profile) {
                is Outcome.Success -> {
                    profileOutcome.result?.let { p ->
                        if (p.hasCars) {
                            ReadableText(slang.selectCar, Modifier.padding(vertical = Paddings.large))
                            SettingsGrid {
                                items(p.user.boats) { boat ->
                                    val isSelected = boat.id == carId
                                    SettingsButton(boat.name, isSelected) {
                                        vm.selectCar(boat.id)
                                    }
                                }
                            }
                        } else {
                            ReadableText(slang.noCars, Modifier.padding(pd))
                        }
                    }
                }
                Outcome.Loading -> CarProgressBar(Modifier.padding(pd))
                is Outcome.Error -> ErrorText(plang.failedToLoadProfile, Modifier.padding(pd))
                Outcome.Idle -> ErrorText(plang.nothingHere, Modifier.padding(pd))
            }
            CarDivider()
            ReadableText(plang.chooseLanguage, Modifier.padding(vertical = Paddings.large))
            SettingsGrid {
                items(languages) { l ->
                    SettingsButton(l.name, isSelected = l.code == savedLanguage) {
                        vm.saveLanguage(l.code)
                    }
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            CarDivider()
            if (isSignedIn) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    Button(
                        onClick = { vm.signOut() },
                        modifier = Modifier.padding(Paddings.xxl),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(plang.signOut, Modifier.padding(Paddings.normal), fontSize = 32.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsGrid(content: LazyGridScope.() -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 256.dp),
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Paddings.normal),
        verticalArrangement = Arrangement.spacedBy(Paddings.normal),
        content = content
    )
}

@Composable
fun SettingsButton(label: String, isSelected: Boolean, onClick: () -> Unit) {
    val containerColor =
        if (isSelected) MaterialTheme.colorScheme.primaryContainer
        else Color.Transparent
    OutlinedButton(
        onClick = { onClick() },
        colors = ButtonDefaults.outlinedButtonColors(containerColor = containerColor),
        contentPadding = PaddingValues(vertical = Paddings.large)) {
        CheckIcon(visible = isSelected)
        Text(
            label,
            modifier = Modifier.padding(horizontal = Paddings.small),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            fontSize = 32.sp
        )
        CheckIcon(visible = false)
    }
}

@Preview
@Composable
fun SettingsPreview() {
    val ctx = LocalContext.current
    SettingsView(Previews.lang(ctx), CarViewModelInterface.preview(ctx), rememberNavController())
}