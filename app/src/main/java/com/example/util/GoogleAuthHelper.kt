package com.example.util

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.tasks.await

object GoogleAuthHelper {
    private const val TAG = "GoogleAuthHelper"

    fun getAuth(): FirebaseAuth = Firebase.auth

    fun getCurrentUser(): FirebaseUser? = Firebase.auth.currentUser

    suspend fun signInWithGoogle(context: Context): Result<FirebaseUser> {
        return try {
            val webClientId = try {
                context.getString(R.string.default_web_client_id)
            } catch (e: Exception) {
                "715807070390-be57nktutsnl4j0bckbpn1p48qti8jtc.apps.googleusercontent.com"
            }

            val credentialManager = CredentialManager.create(context)
            val googleIdOption = GetSignInWithGoogleOption.Builder(serverClientId = webClientId)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(context = context, request = request)
            val credential = result.credential

            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = Firebase.auth.signInWithCredential(authCredential).await()
                val user = authResult.user
                if (user != null) {
                    Log.d(TAG, "Google Sign In successful for ${user.email} (${user.uid})")
                    Result.success(user)
                } else {
                    Result.failure(Exception("Failed to obtain signed in user"))
                }
            } else {
                Result.failure(Exception("Unexpected credential type: ${credential.javaClass.name}"))
            }
        } catch (e: GetCredentialCancellationException) {
            Log.d(TAG, "User cancelled Google Sign In")
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error during Google Sign In", e)
            Result.failure(e)
        }
    }

    fun signOut() {
        Firebase.auth.signOut()
    }
}
