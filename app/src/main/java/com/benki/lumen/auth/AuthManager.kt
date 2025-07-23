package com.benki.lumen.auth

import android.content.Context
import android.util.Log
import androidx.activity.result.IntentSenderRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.benki.lumen.R
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.auth.oauth2.AccessToken
import com.google.auth.oauth2.GoogleCredentials
import kotlinx.coroutines.tasks.await

class AuthManager(private val context: Context) {

    suspend fun signIn(): GoogleIdTokenCredential? {
        val credentialManager = CredentialManager.create(context)
        val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(context.getString(R.string.web_client_id))
            .setAutoSelectEnabled(true)
            .build()

        val request: GetCredentialRequest = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val result = credentialManager.getCredential(context, request)
            GoogleIdTokenCredential.createFrom(result.credential.data)
        } catch (e: GetCredentialException) {
            Log.e("AuthManager", "Sign-in failed", e)
            null
        }
    }

    suspend fun authorize(): AuthorizationResult {
        val authorizationRequest = AuthorizationRequest.builder().setRequestedScopes(
            listOf(Scope(SheetsScopes.SPREADSHEETS))
        ).build()

        val authorizationClient = Identity.getAuthorizationClient(context)
        return try {
            val result = authorizationClient.authorize(authorizationRequest).await()

            if (result?.accessToken != null) {
                val token = AccessToken(result.accessToken, null)
                val credentials = GoogleCredentials(token)
                AuthorizationResult.Success(credentials)
            } else if (result?.pendingIntent != null) {
                val intentSenderRequest = IntentSenderRequest.Builder(result.pendingIntent!!.intentSender).build()
                AuthorizationResult.ResolutionRequired(intentSenderRequest)
            } else {
                Log.w("AuthManager", "Authorization failed: result is null or contains no data.")
                AuthorizationResult.Failure
            }
        } catch (e: ApiException) {
            Log.e("AuthManager", "Authorization failed with ApiException", e)
            AuthorizationResult.Failure
        }
    }
} 