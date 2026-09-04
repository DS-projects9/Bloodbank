package com.medkeen.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/**
 * Obtains a Google ID token via the Credential Manager (AndroidX). The token is
 * forwarded to the backend `/auth/google` endpoint, which verifies it and issues
 * our own JWT. Requires `R.string.google_web_client_id` to be set to the
 * OAuth web client ID created in Google Cloud Console.
 */
object GoogleAuthProvider {
    suspend fun getGoogleIdToken(context: Context, webClientId: String): String? {
        if (webClientId.isBlank() || webClientId.startsWith("REPLACE_")) return null
        return try {
            val credentialManager = CredentialManager.create(context)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setServerClientId(webClientId)
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()
            val result = credentialManager.getCredential(context, request)
            GoogleIdTokenCredential.createFrom(result.credential.data).idToken
        } catch (_: GetCredentialException) {
            null
        } catch (_: Exception) {
            null
        }
    }
}
