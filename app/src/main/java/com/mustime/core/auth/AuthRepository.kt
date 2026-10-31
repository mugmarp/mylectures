package com.mustime.core.auth

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Robust authentication repository integrating Firebase Auth with Jetpack Credential Manager.
 *
 * Implements:
 * 1. Google Identity Services via CredentialManager & GetSignInWithGoogleOption.
 * 2. Real-time authState StateFlow for UI reactivity.
 * 3. Graceful handling of cancellations and configuration fallbacks.
 * 4. Safe sign-out with credential state clearing.
 */
class AuthRepository private constructor() {

    private val auth: FirebaseAuth? = try {
        FirebaseAuth.getInstance()
    } catch (e: Throwable) {
        Log.w(TAG, "FirebaseAuth initialization warning: ${e.message}")
        null
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth?.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    init {
        auth?.addAuthStateListener { firebaseAuth ->
            _currentUser.value = firebaseAuth.currentUser
        }
    }

    /**
     * Initiates Google Sign-In using Android's Jetpack CredentialManager.
     */
    suspend fun signInWithGoogle(context: Context): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        val firebaseAuth = auth ?: return@withContext Result.failure(
            IllegalStateException("Firebase Auth is not available on this device.")
        )

        try {
            val credentialManager = CredentialManager.create(context)

            // Resolve Web Client ID from resources or fallback
            val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            val serverClientId = if (resId != 0) {
                context.getString(resId)
            } else {
                // Fallback client id placeholder if google-services has not generated one
                "default_web_client_id"
            }

            val googleIdOption = GetSignInWithGoogleOption.Builder(serverClientId)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = context
            )

            val credential = result.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken

                val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = firebaseAuth.signInWithCredential(authCredential).await()
                val user = authResult.user ?: return@withContext Result.failure(
                    IllegalStateException("Firebase authentication returned no user profile.")
                )
                _currentUser.value = user
                Result.success(user)
            } else {
                Result.failure(IllegalStateException("Unsupported credential type returned from CredentialManager."))
            }
        } catch (e: GetCredentialCancellationException) {
            Result.failure(e)
        } catch (e: GetCredentialException) {
            Log.e(TAG, "CredentialManager error: ${e.message}", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Sign-in exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Signs out of Firebase Auth and clears Android system credential state.
     */
    suspend fun signOut(context: Context) = withContext(Dispatchers.IO) {
        try {
            val credentialManager = CredentialManager.create(context)
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Throwable) {
            Log.w(TAG, "Clear credential state notice: ${e.message}")
        }
        try {
            auth?.signOut()
        } catch (e: Throwable) {
            Log.w(TAG, "FirebaseAuth sign out error: ${e.message}")
        }
        _currentUser.value = null
    }

    companion object {
        private const val TAG = "AuthRepository"

        @Volatile
        private var INSTANCE: AuthRepository? = null

        fun getInstance(): AuthRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AuthRepository().also { INSTANCE = it }
            }
        }
    }
}
