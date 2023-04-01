package com.skogberglabs.polestar

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.car.app.activity.CarAppActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
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
        profile.locations.startIfGranted()
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
fun ProfileView(vm: ProfileViewModel, onSignIn: () -> Unit) {
    val context = LocalContext.current
    val user by vm.user.collectAsStateWithLifecycle()
    val currentLocation by vm.locationSource.currentLocation.collectAsStateWithLifecycle(null)
    val uploadMessage by vm.uploadMessage.collectAsStateWithLifecycle(Outcome.Idle)
    Column(Modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Car-Tracker", Modifier.padding(52.dp), fontSize = 48.sp)
        when (val u = user) {
            is Outcome.Success -> {
                Text("Signed in as ${u.result.email}.", fontSize = 32.sp)
                Button(
                    onClick = { vm.signOut() },
                    modifier = Modifier.padding(Paddings.xxl),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Sign out", Modifier.padding(Paddings.normal), fontSize = 32.sp)
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
            Outcome.Loading -> CircularProgressIndicator()
            Outcome.Idle -> SignInButton(onSignIn)
        }
        Button(onClick = {
            val i = Intent(context, CarAppActivity::class.java)
            context.startActivity(i)
        }, Modifier.padding(Paddings.xxl)) {
            Text("Go to map", Modifier.padding(Paddings.normal), fontSize = 32.sp)
        }
        currentLocation?.let { loc ->
            Column(Modifier.width(800.dp), horizontalAlignment = Alignment.Start) {
                // String.format("%.2f", d)
                LocationText("GPS ${loc.latitude.formatted(5)}, ${loc.longitude.formatted(5)}")
                loc.accuracyMeters?.let { accuracy ->
                    LocationText("Accuracy $accuracy meters")
                }
                loc.altitudeMeters?.let { altitude ->
                    LocationText("Altitude $altitude meters")
                }
                loc.bearing?.let { bearing ->
                    LocationText("Bearing $bearing")
                }
                loc.bearingAccuracyDegrees?.let { bacc ->
                    LocationText("Bearing accuracy $bacc degrees")
                }
                LocationText(loc.date.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                val message = when (val msg = uploadMessage) {
                    is Outcome.Success -> msg.result.message
                    is Outcome.Error -> msg.e.message ?: "Failed to upload. ${msg.e}"
                    Outcome.Idle -> null
                    Outcome.Loading -> null
                }
                message?.let { msg ->
                    LocationText(msg, Modifier.padding(vertical = Paddings.normal))
                }
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(
            "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            Modifier.padding(Paddings.normal)
        )
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

@Composable fun LocationText(text: String, modifier: Modifier = Modifier) =
    Text(
        text,
        modifier.padding(Paddings.xs),
        style = MaterialTheme.typography.titleLarge,
        textAlign = TextAlign.Start
    )

fun Double.formatted(n: Int): String = String.format("%.${n}f", this)
