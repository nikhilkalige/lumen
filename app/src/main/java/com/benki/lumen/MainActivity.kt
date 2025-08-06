package com.benki.lumen

import android.app.Activity
import android.content.Intent
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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.benki.lumen.ui.FuelViewModel
import com.benki.lumen.ui.MainScreenRoute
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
            } else {
                // Optionally, handle the case where the user cancels authorization
                // For example, show a message
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intentUri: Uri? = intent?.data

        // Handle the intent that started the app
        handleDeepLink(intent)

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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Handle the new intent
        handleDeepLink(intent)
    }

    /**
     * A shared function to process the intent's URI and clear it.
     */
    private fun handleDeepLink(intent: Intent?) {
        val uri = intent?.data

        // Pass the URI to the ViewModel to handle the business logic
        if (intent?.action == Intent.ACTION_VIEW && uri?.host == "gas") {
            val gallonsStr = uri.getQueryParameter("gallons")
            val milesStr = uri.getQueryParameter("odometer")
            val costStr = uri.getQueryParameter("cost")
            val dateStr = uri.getQueryParameter("date")

            val requiredFields = listOf(gallonsStr, milesStr, costStr)
            val missingFieldCount = requiredFields.count { it.isNullOrBlank() }

            when (missingFieldCount) {
                0 -> {
                    // All fields are present, proceed as before.
                    // The '!!' are safe here because we've confirmed they are not null.
                    fuelViewModel.addFuelEntryFromIntent(
                        gallons = gallonsStr!!,
                        odometer = milesStr!!,
                        cost = costStr!!,
                        date = dateStr
                    )
                }
                1 -> {
                    // One field is missing, trigger the edit dialog via the ViewModel.
                    fuelViewModel.showEditEntryDialog(
                        gallons = gallonsStr,
                        odometer = milesStr,
                        cost = costStr,
                        date = dateStr
                    )
                }
                else -> {
                    // Two or more fields are missing, show an error dialog.
                    fuelViewModel.showTooMuchMissingErrorDialog(
                        gallons = gallonsStr,
                        odometer = milesStr,
                        cost = costStr
                    )
                }
            }

            if (gallonsStr != null && milesStr != null && costStr != null) {
                // Call the new, clean function in the ViewModel
                fuelViewModel.addFuelEntryFromIntent(
                    gallons = gallonsStr,
                    odometer = milesStr,
                    cost = costStr,
                    date = dateStr
                )
            }
        } else if (intent?.action == Intent.ACTION_VIEW && intent.data?.host == "picker-result") {
            val fileId = intent.getStringExtra("fileId")
            val fileName = intent.getStringExtra("fileName")
            val mimeType = intent.getStringExtra("mimeType")

            if (fileId != null && fileName != null) {
                // Call the new function in your ViewModel
                settingsViewModel.onFileSelectedFromWeb(fileId, fileName, mimeType)
            }
        }
        setIntent(Intent())
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
            MainScreenRoute(navController, fuelViewModel)
        }
        composable("settings") {
            SettingsScreen(navController, settingsViewModel)
        }
    }
}