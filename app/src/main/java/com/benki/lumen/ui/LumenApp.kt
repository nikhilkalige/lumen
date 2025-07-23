package com.benki.lumen.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.benki.lumen.MainViewModel
import com.benki.lumen.auth.AuthManager
import com.benki.lumen.ui.navigation.LumenNavHost
import com.example.lumen.ui.theme.LumenTheme
import com.google.auth.oauth2.GoogleCredentials
import kotlinx.coroutines.launch

@Composable
fun LumenApp(
    viewModel: MainViewModel,
    authManager: AuthManager,
    onSignedIn: (GoogleCredentials) -> Unit
) {
    var isSignedIn by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            when (val result = authManager.authorize()) {
                is com.benki.lumen.auth.AuthorizationResult.Success -> {
                    onSignedIn(result.credentials)
                    isSignedIn = true
                    isLoading = false
                }
                is com.benki.lumen.auth.AuthorizationResult.ResolutionRequired -> {
                    // TODO: Handle intent sender for user interaction (not possible in pure @Composable)
                    // For now, treat as failure or keep loading
                    isLoading = false
                    errorMessage = "User interaction required for sign-in. Please restart the app."
                }
                is com.benki.lumen.auth.AuthorizationResult.Failure -> {
                    isLoading = false
                    errorMessage = "Authorization failed."
                }
            }
        }
    }

    LumenTheme {
        when {
            isSignedIn -> {
                LumenNavHost(viewModel = viewModel)
            }
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            errorMessage != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Text(text = errorMessage!!)
                }
            }
        }
    }
} 