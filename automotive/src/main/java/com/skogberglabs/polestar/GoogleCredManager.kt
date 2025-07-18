package com.skogberglabs.polestar

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.delay
import timber.log.Timber
import kotlin.time.Duration.Companion.milliseconds

class GoogleCredManager(
    private val client: CredentialManager,
    private val userState: UserState,
) {
    companion object {
        private const val webClientId = "497623115973-c6v1e9khup8bqj41vf228o2urnv86muh.apps.googleusercontent.com"

        fun build(
            ctx: Context,
            userState: UserState,
        ) = GoogleCredManager(CredentialManager.create(ctx), userState)

        fun readUser(account: GetCredentialResponse): UserInfo? {
            when (val cred = account.credential) {
                is CustomCredential -> {
                    if (cred.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                        val idTokenCred = GoogleIdTokenCredential.createFrom(cred.data)
                        return UserInfo(Email(idTokenCred.id), IdToken(idTokenCred.idToken))
                    }
                }
                else -> {
                    Timber.w("Unsupported credential type.")
                }
            }
            return null
        }

        private fun options(filterByAuthorized: Boolean): GetGoogleIdOption =
            GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(filterByAuthorized)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(true)
                .build()
    }

    suspend fun startSignIn(activityContext: Context, opportunistic: Boolean) {
        userState.update(Outcome.Loading)
        try {
            val result =
                try {
                    client.getCredential(activityContext, buildRequest(filterByAuthorized = true, immediatelyAvailable = opportunistic))
                } catch (e: NoCredentialException) {
                    Timber.i("No credential available. Trying with setFilterByAuthorizedAccounts = false...")
                    client.getCredential(activityContext, buildRequest(filterByAuthorized = false, immediatelyAvailable = opportunistic))
                }
            handleSignIn(result)
        } catch (e: GetCredentialException) {
            fail(e)
            userState.update(Outcome.Error(e))
        } catch (e: Exception) {
            fail(e)
            userState.update(Outcome.Error(e))
        }
    }

    private fun buildRequest(
        filterByAuthorized: Boolean,
        immediatelyAvailable: Boolean,
    ): GetCredentialRequest =
        GetCredentialRequest.Builder()
            .addCredentialOption(options(filterByAuthorized))
            .setPreferImmediatelyAvailableCredentials(immediatelyAvailable)
            .build()

    private fun fail(e: Exception) {
        Timber.w(e, "Google auth failed.")
        userState.update(Outcome.Error(e))
    }

    private fun handleSignIn(
        account: GetCredentialResponse,
    ): UserInfo? {
        try {
            readUser(account)?.let { user ->
                Timber.i("Got user ${user.email}...")
                userState.update(Outcome.Success(user))
                return user
            }
            // I think this never happens, instead the exceptional path is taken
            Timber.w("Unable to read user info from account.")
            userState.update(Outcome.Error(Exception("Unable to read signed in user.")))
        } catch (e: Exception) {
            // "error" might be "Sign in required", so we don't fail this exceptionally
            Timber.w(e, "Non-silent sign in failed exceptionally.")
            val outcome = Outcome.Error(e)
            userState.update(outcome)
        }
        return null
    }

    suspend fun signOut() {
        Timber.i("Signing out...")
        try {
//            userState.update(Outcome.Loading)
            client.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Timber.e(e, "Failed to sign out.")
        } finally {
            userState.update(Outcome.Idle)
            Timber.i("Signed out. Returning soon...")
            delay(550.milliseconds)
        }
    }
}
