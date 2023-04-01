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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
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
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.DateFormat
import java.time.format.DateTimeFormatter

class SignInActivity : ComponentActivity() {
    private val requestCodeSignIn = 100
    private val profile: ProfileViewModel by viewModels()
    private val scope = CoroutineScope(Dispatchers.IO)

    private val userState = UserState.instance
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scope.launch {
            Google.instance.signInSilently(applicationContext)
        }
        profile.locations.startIfGranted()
        setContent {
            AppTheme {
                Surface(Modifier.fillMaxSize()) {
                    SignIn(profile, profile.locationSource) { signIn() }
                }
            }
        }
    }

    private fun signIn() {
        Timber.i("Signing in...")
        val signInIntent = profile.google.signInIntent
        startActivityForResult(signInIntent, requestCodeSignIn)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Timber.i("Got activity result of request $requestCode. Result code $resultCode.")
        if (requestCode == requestCodeSignIn) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            val user = account?.let { a -> Google.readUser(a) }
            Timber.i("Sign in success.")
            userState.update(user)
        } catch (e: ApiException) {
            val str = CommonStatusCodes.getStatusCodeString(e.statusCode)
            Timber.w(e, "Sign in failed. Code ${e.statusCode}. $str.")
//            updateFeedback("Sign in failed.")
        }
    }
}

@Composable
fun SignIn(vm: ProfileViewModel, locs: LocationSource, onSignIn: () -> Unit) {
    val context = LocalContext.current
    val user by vm.user.collectAsStateWithLifecycle()
    val currentLocation by locs.currentLocation.collectAsStateWithLifecycle(null)
    Column(Modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Car-Tracker", Modifier.padding(52.dp), fontSize = 48.sp)
        user?.let { u ->
            Text("Signed in as ${u.email}.", fontSize = 32.sp)
        } ?: run {
            Button(
                onClick = {
                    Timber.i("Signing in!")
                    onSignIn()
                },
                Modifier
                    .padding(Paddings.normal)
                    .widthIn(max = 800.dp)
            ) {
                Text("Sign in with Google", Modifier.padding(Paddings.normal), fontSize = 32.sp)
            }
        }
        Button(onClick = {
            val i = Intent(context, CarAppActivity::class.java)
            context.startActivity(i)
        }, Modifier.padding(Paddings.xxl)) {
            Text("Go to map", Modifier.padding(Paddings.normal), fontSize = 32.sp)
        }
        currentLocation?.let { loc ->
            Column(horizontalAlignment = Alignment.Start) {
                LocationText("GPS ${loc.latitude}, ${loc.longitude}")
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
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(
            "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            Modifier.padding(Paddings.normal)
        )
    }
}

@Composable fun LocationText(text: String) =
    Text(text, Modifier.padding(Paddings.xs), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Start)