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
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            val credential = authManager.signInAndAuthorize()
            if (credential != null) {
                onSignedIn(credential)
                isSignedIn = true
            } else {
                // Optionally handle sign-in failure, e.g., show an error message
            }
        }
    }

    LumenTheme {
        if (isSignedIn) {
            LumenNavHost(viewModel = viewModel)
        } else {
            // Show a loading indicator while signing in
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
} 