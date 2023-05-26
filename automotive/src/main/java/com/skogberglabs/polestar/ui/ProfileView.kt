package com.skogberglabs.polestar.ui

import android.content.Intent
import androidx.car.app.activity.CarAppActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.skogberglabs.polestar.BuildConfig
import com.skogberglabs.polestar.CarLang
import com.skogberglabs.polestar.NavRoutes
import com.skogberglabs.polestar.Outcome
import com.skogberglabs.polestar.Paddings
import com.skogberglabs.polestar.location.isAllPermissionsGranted
import java.time.format.DateTimeFormatter

@Composable
fun SpacedRow(content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileView(lang: CarLang, vm: CarViewModelInterface, navController: NavController, onSignIn: () -> Unit) {
    val context = LocalContext.current
    val user by vm.user.collectAsStateWithLifecycle()
    val profile by vm.profile.collectAsStateWithLifecycle(Outcome.Idle)
    val currentLocation by vm.locationSource.currentLocation.collectAsStateWithLifecycle(null)
    val uploadMessage by vm.uploadMessage.collectAsStateWithLifecycle(Outcome.Idle)
    val carState by vm.carState.collectAsStateWithLifecycle()
    val plang = lang.profile
    val slang = lang.stats
    Scaffold(
        modifier = Modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = { TitleText(lang.appName) },
                modifier = Modifier.padding(Paddings.normal),
                actions = {
                    IconButton(onClick = { navController.navigate(NavRoutes.SETTINGS) }, modifier = Modifier.size(64.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Settings",
                            Modifier.fillMaxSize()
                        )
                    }
                }
            ) }
    ) { pd ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pd)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (val u = user) {
                is Outcome.Success -> {
                    Text(
                        "${lang.profile.signedInAs} ${u.result.email}.",
                        modifier = Modifier.padding(Paddings.large),
                        fontSize = 32.sp
                    )
                    when (val profileOutcome = profile) {
                        is Outcome.Success -> {
                            profileOutcome.result?.let { p ->
                                p.activeCar?.let { car ->
                                    ReadableText("${plang.driving} ${car.name}.")
                                } ?: run {
                                    OutlinedButton(
                                        onClick = { navController.navigate(NavRoutes.SETTINGS) },
                                        modifier = Modifier.padding(Paddings.large)) {
                                        Text(lang.settings.selectCar)
                                    }
                                }
                            }
                        }
                        Outcome.Idle -> Text("")
                        Outcome.Loading -> CarProgressBar()
                        is Outcome.Error -> ErrorText(plang.failedToLoadProfile)
                    }
                }
                is Outcome.Error -> {
                    SignInButton("${plang.signInWith} Google", onSignIn)
                    when (val ex = u.e) {
                        is ApiException -> {
                            val str = CommonStatusCodes.getStatusCodeString(ex.statusCode)
                            Text("${plang.failedToSignIn} '${ex.statusCode}': $str. ${ex.message} $ex")
                        }
                        else -> {
                            Text("${plang.failedToSignIn} ${ex.message} $ex")
                        }
                    }
                }
                Outcome.Loading -> CarProgressBar()
                Outcome.Idle -> SignInButton("${plang.signInWith} Google", onSignIn)
            }
            CarDivider()
            Column(Modifier.padding(Paddings.xxl)) {
                currentLocation?.let { loc ->
                    SpacedRow {
                        val accuracy = loc.accuracyMeters?.let { " ${slang.accuracy.lowercase()} $it ${slang.meters}" } ?: ""
                        ProfileText("GPS ${loc.latitude.formatted(5)}, ${loc.longitude.formatted(5)}$accuracy")
                    }
                    SpacedRow {
                        loc.altitudeMeters?.let { altitude ->
                            ProfileText("${slang.altitude} $altitude ${slang.meters}")
                        }
                        loc.bearing?.let { bearing ->
                            val accuracyText = loc.bearingAccuracyDegrees?.let { " ${slang.accuracy.lowercase()} $it ${slang.degrees}" } ?: ""
                            ProfileText("${slang.bearing} $bearing$accuracyText")
                        }
                    }
                    SpacedRow {
                        ProfileText(loc.date.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                        when (val outcome = uploadMessage) {
                            is Outcome.Success -> ProfileText("'${outcome.result.message}'.")
                            is Outcome.Error -> ProfileText(outcome.e.message ?: "${outcome.e}", color = MaterialTheme.colorScheme.error)
                            Outcome.Idle -> Text("")
                            Outcome.Loading -> Text("")
                        }
                    }
                }
                if (!carState.isEmpty) {
                    SpacedRow {
                        carState.batteryLevel?.let { energy ->
                            ProfileText("${slang.batteryLevel} ${energy.describeKWh}")
                        }
                        carState.batteryCapacity?.let { capacity ->
                            ProfileText("${slang.capacity} ${capacity.describeKWh}")
                        }
                        carState.rangeRemaining?.let { distance ->
                            ProfileText("${slang.range} ${distance.describeKm}")
                        }
                    }
                    SpacedRow {
                        carState.speed?.let { speed ->
                            ProfileText("${slang.speed} ${speed.describeKmh}")
                        }
                        carState.outsideTemperature?.let { temperature ->
                            ProfileText("${slang.outsideTemperature} ${temperature.describeCelsius}")
                        }
                        carState.nightMode?.let { nightMode ->
                            ProfileText(if (nightMode) slang.nightMode else slang.dayMode)
                        }
                    }
                }
            }
            CarDivider()
            Spacer(modifier = Modifier.weight(1f))
            Button(onClick = {
                val i = Intent(context, CarAppActivity::class.java)
                context.startActivity(i)
            }, Modifier.padding(Paddings.xxl)) {
                val label =
                    if (!context.isAllPermissionsGranted()) lang.permissions.grantCta
                    else plang.goToMap
                Text(
                    label,
                    Modifier.padding(Paddings.normal),
                    fontSize = 32.sp
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            CarDivider()
            Text(
                "${plang.version} ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                Modifier.padding(Paddings.normal)
            )
            Spacer(modifier = Modifier.height(Paddings.large))
        }
    }
}

@Preview
@Composable
fun ProfilePreview() {
    val ctx = LocalContext.current
    val conf = Previews.lang(ctx)
    MaterialTheme {
        ProfileView(conf, CarViewModelInterface.preview(ctx), rememberNavController()) {
        }
    }
}

@Composable fun SignInButton(label: String, onSignIn: () -> Unit) {
    Button(
        onClick = { onSignIn() },
        Modifier
            .padding(Paddings.normal)
            .widthIn(max = 800.dp)
    ) {
        Text(label, Modifier.padding(Paddings.normal), fontSize = 32.sp)
    }
}

@Composable fun ProfileText(text: String, modifier: Modifier = Modifier, color: Color = Color.Unspecified) =
    Text(
        text,
        modifier.padding(Paddings.xs),
        color = color,
        style = MaterialTheme.typography.titleLarge,
        textAlign = TextAlign.Start
    )

fun Double.formatted(n: Int): String = String.format("%.${n}f", this)
fun Float.formatted(n: Int): String = String.format("%.${n}f", this)
