package com.benki.lumen

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.benki.lumen.ui.FuelViewModel
import com.benki.lumen.ui.MainScreen
import com.benki.lumen.ui.SettingsScreen
import com.benki.lumen.ui.SettingsViewModel
import com.benki.lumen.ui.UiEvent

import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val fuelViewModel: FuelViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    private val authLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                fuelViewModel.retryLastOperation()
            } else {
                // Optionally, handle the case where the user cancels authorization
                // For example, show a message
            }
        }

    

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intentUri: Uri? = intent?.data

        lifecycleScope.launch {
            fuelViewModel.uiEvent.collect { event ->
                if (event is UiEvent.ShowAuthorizationPrompt) {
                    val request = IntentSenderRequest.Builder(event.pendingIntent).build()
                    authLauncher.launch(request)
                }
            }
        }

        setContent {
            LumenApp(fuelViewModel, settingsViewModel, intentUri)
        }
    }
}

@Composable
fun LumenApp(
    fuelViewModel: FuelViewModel,
    settingsViewModel: SettingsViewModel,
    intentUri: Uri?
) {
    val navController = rememberNavController()
    MaterialTheme {
        Surface(modifier = Modifier) {
            NavGraph(navController, fuelViewModel, settingsViewModel, intentUri)
        }
    }
}

@Composable
fun NavGraph(
    navController: NavHostController,
    fuelViewModel: FuelViewModel,
    settingsViewModel: SettingsViewModel,
    intentUri: Uri?
) {
    NavHost(navController, startDestination = "main") {
        composable("main") {
            MainScreen(navController, fuelViewModel)
            LaunchedEffect(intentUri) {
                fuelViewModel.handleIntentUri(intentUri)
            }
        }
        composable("settings") {
            SettingsScreen(navController, settingsViewModel)
        }
    }
}