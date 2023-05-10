package com.skogberglabs.polestar

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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Divider
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
fun ProfileView(vm: ProfileViewModelInterface, navController: NavController, onSignIn: () -> Unit) {
    val context = LocalContext.current
    val user by vm.user.collectAsStateWithLifecycle()
    val profile by vm.profile.collectAsStateWithLifecycle(Outcome.Idle)
    val currentLocation by vm.locationSource.currentLocation.collectAsStateWithLifecycle(null)
    val uploadMessage by vm.uploadMessage.collectAsStateWithLifecycle(Outcome.Idle)
    val isSignedIn = user.toOption() != null
    val carState by vm.carState.collectAsStateWithLifecycle()
    Scaffold(
        modifier = Modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = { TitleText("Car-Tracker") },
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
                        "Signed in as ${u.result.email}.",
                        modifier = Modifier.padding(Paddings.large),
                        fontSize = 32.sp
                    )
                    when (val profileOutcome = profile) {
                        is Outcome.Success -> {
                            profileOutcome.result?.let { p ->
                                p.activeCar?.let { car ->
                                    ReadableText("Driving ${car.name}.")
                                } ?: run {
                                    OutlinedButton(
                                        onClick = { navController.navigate(NavRoutes.SETTINGS) },
                                        modifier = Modifier.padding(Paddings.large)) {
                                        Text("Select car")
                                    }
                                }
                            }
                        }
                        Outcome.Idle -> Text("")
                        Outcome.Loading -> CarProgressBar()
                        is Outcome.Error -> ErrorText("Failed to load profile.")
                    }
                }
                is Outcome.Error -> {
                    SignInButton(onSignIn)
                    when (val ex = u.e) {
                        is ApiException -> {
                            val str = CommonStatusCodes.getStatusCodeString(ex.statusCode)
                            Text("Failed to sign in. API exception status code '${ex.statusCode}': $str. ${ex.message} $ex")
                        }
                        else -> {
                            Text("Failed to sign in. ${ex.message} $ex")
                        }
                    }
                }
                Outcome.Loading -> CarProgressBar()
                Outcome.Idle -> SignInButton(onSignIn)
            }
            Divider(Modifier.padding(vertical = Paddings.large))
            Column(Modifier.padding(Paddings.xxl)) {
                currentLocation?.let { loc ->
                    SpacedRow {
                        val accuracy = loc.accuracyMeters?.let { " accuracy $it meters" } ?: ""
                        ProfileText("GPS ${loc.latitude.formatted(5)}, ${loc.longitude.formatted(5)}$accuracy")
                    }
                    SpacedRow {
                        loc.altitudeMeters?.let { altitude ->
                            ProfileText("Altitude $altitude meters")
                        }
                        loc.bearing?.let { bearing ->
                            val accuracyText = loc.bearingAccuracyDegrees?.let { " accuracy $it degrees" } ?: ""
                            ProfileText("Bearing $bearing$accuracyText")
                        }
                    }
                    SpacedRow {
                        ProfileText(loc.date.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                        when (val msg = uploadMessage) {
                            is Outcome.Success -> ProfileText(msg.result.message)
                            is Outcome.Error -> ProfileText(msg.e.message ?: "Failed to upload. ${msg.e}", color = MaterialTheme.colorScheme.error)
                            Outcome.Idle -> Text("")
                            Outcome.Loading -> Text("")
                        }
                    }
                }
                if (!carState.isEmpty) {
                    SpacedRow {
                        carState.batteryLevel?.let { energy ->
                            ProfileText("Battery level ${energy.describeKWh}")
                        }
                        carState.batteryCapacity?.let { capacity ->
                            ProfileText("Capacity ${capacity.describeKWh}")
                        }
                        carState.rangeRemaining?.let { distance ->
                            ProfileText("Range ${distance.describeKm}")
                        }
                    }
                    SpacedRow {
                        carState.speed?.let { speed ->
                            ProfileText("Speed ${speed.describeKmh}")
                        }
                        carState.outsideTemperature?.let { temperature ->
                            ProfileText("Outside temperature ${temperature.describeCelsius}")
                        }
                        carState.nightMode?.let { nightMode ->
                            val mode = if (nightMode) "Night" else "Day"
                            ProfileText("$mode mode")
                        }
                    }
                }
            }
            Divider(Modifier.padding(vertical = Paddings.large))
            Spacer(modifier = Modifier.weight(1f))
            Button(onClick = {
                val i = Intent(context, CarAppActivity::class.java)
                context.startActivity(i)
            }, Modifier.padding(Paddings.xxl)) {
                val label =
                    if (!context.isAllPermissionsGranted()) "Grant permissions"
                    else "Go to map"
                Text(
                    label,
                    Modifier.padding(Paddings.normal),
                    fontSize = 32.sp
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Divider(Modifier.padding(vertical = Paddings.large))
            if (isSignedIn) {
                Button(
                    onClick = { vm.signOut() },
                    modifier = Modifier.padding(Paddings.xxl),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Sign out", Modifier.padding(Paddings.normal), fontSize = 32.sp)
                }
            }
            Text(
                "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                Modifier.padding(Paddings.normal)
            )
            Spacer(modifier = Modifier.height(Paddings.large))
        }
    }
}

@Preview
@Composable
fun ProfilePreview() {
    MaterialTheme {
        ProfileView(ProfileViewModelInterface.preview, rememberNavController()) {
        }
    }
}

@Composable fun SignInButton(onSignIn: () -> Unit) {
    Button(
        onClick = { onSignIn() },
        Modifier
            .padding(Paddings.normal)
            .widthIn(max = 800.dp)
    ) {
        Text("Sign in with Google", Modifier.padding(Paddings.normal), fontSize = 32.sp)
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
