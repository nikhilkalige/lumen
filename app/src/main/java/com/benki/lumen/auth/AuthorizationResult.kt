package com.benki.lumen.auth

import androidx.activity.result.IntentSenderRequest
import com.google.auth.oauth2.GoogleCredentials

sealed class AuthorizationResult {
    data class Success(val credentials: GoogleCredentials) : AuthorizationResult()
    data class ResolutionRequired(val intentSenderRequest: IntentSenderRequest) : AuthorizationResult()
    object Failure : AuthorizationResult()
} 