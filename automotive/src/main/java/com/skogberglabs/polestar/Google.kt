package com.skogberglabs.polestar

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class Google(private val client: GoogleSignInClient, private val userState: UserState) {
    companion object {
        private const val webClientId = "497623115973-c6v1e9khup8bqj41vf228o2urnv86muh.apps.googleusercontent.com"

        fun build(ctx: Context, userState: UserState) =
            Google(GoogleSignIn.getClient(ctx, options()), userState)

        fun readUser(account: GoogleSignInAccount): UserInfo? {
            val idToken = account.idToken
            val email = account.email
            return idToken?.let { token ->
                email?.let { email ->
                    UserInfo(Email(email), IdToken(token))
                }
            }
        }

        private fun options() = GoogleSignInOptions.Builder()
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
    }

    fun startSignIn(): Intent {
        userState.update(Outcome.Loading)
        return client.signInIntent
    }

    fun fail(e: Exception) {
        userState.update(Outcome.Error(e))
    }

    suspend fun signInSilently(): UserInfo? =
        try {
            userState.update(Outcome.Loading)
            val task = client.silentSignIn()
            val account = if (task.isSuccessful) task.result else task.await()
            handleSignIn(account, silent = true)
        } catch (e: Exception) {
            // "error" might be "Sign in required", so we don't fail this exceptionally
            Timber.w(e, "Silent sign in failed exceptionally.")
            userState.update(Outcome.Idle)
            null
        }

    fun handleSignIn(account: GoogleSignInAccount, silent: Boolean): UserInfo? {
        try {
            readUser(account)?.let { user ->
//                Timber.d("Got user ${user.email}...")
                userState.update(Outcome.Success(user))
                return user
            }
            // I think this never happens, instead the exceptional path is taken
            Timber.w("Unable to read user info from account.")
            userState.update(Outcome.Error(Exception("Unable to read signed in user.")))
        } catch (e: Exception) {
            // "error" might be "Sign in required", so we don't fail this exceptionally
            val word = if (silent) "Silent" else "Non-silent"
            Timber.w(e, "$word sign in failed exceptionally.")
            val outcome = if (silent) Outcome.Idle else Outcome.Error(e)
            userState.update(outcome)
        }
        return null
    }

    suspend fun signOut() {
        Timber.i("Signing out...")
        try {
            userState.update(Outcome.Loading)
            client.signOut().awaitVoid()
        } catch (e: Exception) {
            Timber.e(e, "Failed to sign out.")
        } finally {
            userState.update(Outcome.Idle)
            Timber.i("Signed out.")
        }
    }
}

suspend fun <T> Task<T>.await(): T = suspendCoroutine { cont ->
    addOnCompleteListener { task ->
        try {
            val t = task.getResult(ApiException::class.java)
            if (t != null) {
                cont.resume(t)
            } else {
                cont.resumeWithException(Exception("No result in task."))
            }
        } catch (e: ApiException) {
            cont.resumeWithException(e)
        }
    }
}

suspend fun Task<Void>.awaitVoid() = suspendCoroutine { cont ->
    addOnCompleteListener { task ->
        try {
            task.getResult(ApiException::class.java)
            cont.resume(Unit)
        } catch (e: ApiException) {
            cont.resumeWithException(e)
        }
    }
}
