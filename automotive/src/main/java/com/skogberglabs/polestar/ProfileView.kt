package com.skogberglabs.polestar

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.car.app.activity.CarAppActivity
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.format.DateTimeFormatter

class ProfileActivity : ComponentActivity() {
    private val requestCodeSignIn = 100
    private val profile: ProfileViewModel by viewModels()
    private val scope = CoroutineScope(Dispatchers.IO)

    private val google: Google get() = profile.google

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scope.launch {
            google.signInSilently()
        }
        Timber.i("Creating Profile activity...")
        setContent {
            AppTheme {
                Surface(Modifier.fillMaxSize()) {
                    ProfileView(profile) { signIn() }
                }
            }
        }
    }

    private fun signIn() {
        Timber.i("Signing in...")
        startActivityForResult(google.startSignIn(), requestCodeSignIn)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Timber.i("Got activity result of request $requestCode. Result code $resultCode.")
        if (requestCode == requestCodeSignIn) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                google.handleSignIn(account, silent = false)
            } catch (e: ApiException) {
                Timber.w(e, "Failed to handle sign in.")
                google.fail(e)
            }
        }
    }
}

@Composable
fun ProfileView(vm: ProfileViewModelInterface, onSignIn: () -> Unit) {
    val context = LocalContext.current
    val user by vm.user.collectAsStateWithLifecycle()
    val profile by vm.profile.collectAsStateWithLifecycle(Outcome.Idle)
    val carId = profile.toOption()?.activeCar?.id
    val currentLocation by vm.locationSource.currentLocation.collectAsStateWithLifecycle(null)
    val uploadMessage by vm.uploadMessage.collectAsStateWithLifecycle(Outcome.Idle)
    val isSignedIn = user.toOption() != null
    val carState by vm.carState.collectAsStateWithLifecycle(null)
    Column(
        Modifier
            .padding(horizontal = Paddings.xxl)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Car-Tracker", Modifier.padding(Paddings.xxl), fontSize = 48.sp)
        when (val u = user) {
            is Outcome.Success -> {
                Text("Signed in as ${u.result.email}.", fontSize = 32.sp)
                when (val profileOutcome = profile) {
                    is Outcome.Success -> {
                        profileOutcome.result?.let { p ->
                            val msg = p.activeCar?.let { car -> "Driving ${car.name}." } ?: run {
                                if (p.hasCars) "Select car to continue." else "No cars."
                            }
                            Text(
                                msg,
                                Modifier.padding(Paddings.large),
                                style = MaterialTheme.typography.titleLarge,
                                fontSize = 28.sp
                            )
                            LazyRow(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(Paddings.small),
                                horizontalArrangement = Arrangement.spacedBy(Paddings.small)
                            ) {
                                items(p.user.boats) { boat ->
                                    Box(
                                        Modifier
                                            .border(
                                                if (boat.id == carId) 8.dp else 2.dp,
                                                Color.Blue
                                            )
                                            .sizeIn(minWidth = 220.dp, minHeight = 128.dp)
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
                    }
                    Outcome.Idle -> Text("")
                    Outcome.Loading -> CircularProgressIndicator(Modifier.padding(Paddings.xxl))
                    is Outcome.Error -> Text(
                        "Failed to load profile.",
                        Modifier.padding(Paddings.large),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.titleLarge,
                        fontSize = 28.sp
                    )
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
            Outcome.Loading -> CircularProgressIndicator(Modifier.padding(Paddings.xxl))
            Outcome.Idle -> SignInButton(onSignIn)
        }
        currentLocation?.let { loc ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = Paddings.large),
                horizontalAlignment = Alignment.Start
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    val accuracy = loc.accuracyMeters?.let { " accuracy $it meters" } ?: ""
                    ProfileText("GPS ${loc.latitude.formatted(5)}, ${loc.longitude.formatted(5)}$accuracy")
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    loc.altitudeMeters?.let { altitude ->
                        ProfileText("Altitude $altitude meters")
                    }
                    loc.bearing?.let { bearing ->
                        val accuracyText = loc.bearingAccuracyDegrees?.let { " accuracy $it degrees" } ?: ""
                        ProfileText("Bearing $bearing$accuracyText")
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    ProfileText(loc.date.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                    when (val msg = uploadMessage) {
                        is Outcome.Success -> ProfileText(msg.result.message)
                        is Outcome.Error -> ProfileText(msg.e.message ?: "Failed to upload. ${msg.e}", color = MaterialTheme.colorScheme.error)
                        Outcome.Idle -> Text("")
                        Outcome.Loading -> Text("")
                    }
                }
            }
        }
        carState?.let { state ->
            if (!state.isEmpty) {
                ProfileText(Adapters.carState.toJson(state))
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Button(onClick = {
            val i = Intent(context, CarAppActivity::class.java)
            context.startActivity(i)
        }, Modifier.padding(Paddings.xxl)) {
            val label =
                if (!context.isLocationGranted()) "Grant location permission"
                else if (!context.isCarPermissionGranted()) "Grant car permissions"
                else "Go to map"
            Text(
                label,
                Modifier.padding(Paddings.normal),
                fontSize = 32.sp
            )
        }
        Spacer(modifier = Modifier.weight(1f))
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
    }
}

@Preview
@Composable
fun ProfilePreview() {
    MaterialTheme {
        ProfileView(ProfileViewModelInterface.preview) {
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
