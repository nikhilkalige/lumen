package com.benki.lumen

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import com.benki.lumen.auth.AuthManager
import com.benki.lumen.data.GoogleSheetsService
import com.benki.lumen.data.UserRepository
import com.benki.lumen.ui.LumenApp
import com.benki.lumen.GoogleSheetsServiceViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
// import androidx.compose.ui.platform.LocalDataStore
//import androidx.compose.ui.platform.LocalSettingsDataStore

import androidx.compose.runtime.remember

private val Context.dataStore: DataStore<SheetEntryList> by dataStore(
    fileName = "sheet_entries.pb",
    serializer = SheetEntryListSerializer
)

private val Context.settingsDataStore: DataStore<Settings> by dataStore(
    fileName = "settings.pb",
    serializer = SettingsSerializer
)

class MainActivity : ComponentActivity() {
    private val authManager by lazy { AuthManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val sheetsServiceViewModel: GoogleSheetsServiceViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            val userRepository = remember {
                UserRepository(dataStore, settingsDataStore, sheetsServiceViewModel.serviceFlow)
            }
            val mainViewModel: MainViewModel = androidx.lifecycle.viewmodel.compose.viewModel(factory = MainViewModel.Factory(userRepository))

            // Handle intent after mainViewModel is available
            androidx.compose.runtime.LaunchedEffect(Unit) {
                handleIntent(intent, mainViewModel)
            }

            LumenApp(
                viewModel = mainViewModel,
                authManager = authManager,
                onSignedIn = { credentials ->
                    val service = com.benki.lumen.data.GoogleSheetsService(credentials)
                    sheetsServiceViewModel.setService(service)
                    android.util.Log.d("MainActivity", "Set service in ViewModel: $service")
                }
            )
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        // The intent handling is now moved to setContent, so this method is no longer needed
        // unless the intent is received before setContent is called.
        // For now, keeping it as is, but it might be redundant.
    }

    private fun handleIntent(intent: Intent?, mainViewModel: MainViewModel) {
        if (intent?.action == Intent.ACTION_VIEW && intent.data != null) {
            val uri = intent.data!!
            if (uri.path?.startsWith("/gas") == true) {
                val odometer = uri.getQueryParameter("odometer")?.toDoubleOrNull()
                val gallons = uri.getQueryParameter("gallons")?.toDoubleOrNull()
                val cost = uri.getQueryParameter("cost")?.toDoubleOrNull()

                if (odometer != null && gallons != null && cost != null) {
                    mainViewModel.addGasEntry(odometer, gallons, cost)
                } else {
                    // Optionally, provide feedback to the user about the invalid intent.
                }
            }
        }
    }
}