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
import kotlinx.coroutines.flow.MutableStateFlow

private val Context.dataStore: DataStore<SheetEntryList> by dataStore(
    fileName = "sheet_entries.pb",
    serializer = SheetEntryListSerializer
)

private val Context.settingsDataStore: DataStore<Settings> by dataStore(
    fileName = "settings.pb",
    serializer = SettingsSerializer
)

class MainActivity : ComponentActivity() {
    private val googleSheetsServiceFlow = MutableStateFlow<GoogleSheetsService?>(null)
    private val authManager by lazy { AuthManager(this) }
    private val userRepository by lazy {
        UserRepository(dataStore, settingsDataStore, googleSheetsServiceFlow)
    }
    private val viewModel: MainViewModel by viewModels {
        MainViewModel.Factory(userRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)

        setContent {
            LumenApp(
                viewModel = viewModel,
                authManager = authManager,
                onSignedIn = { credentials ->
                    googleSheetsServiceFlow.value = GoogleSheetsService(credentials)
                }
            )
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW && intent.data != null) {
            val uri = intent.data!!
            if (uri.path?.startsWith("/gas") == true) {
                val odometer = uri.getQueryParameter("odometer")?.toDoubleOrNull()
                val gallons = uri.getQueryParameter("gallons")?.toDoubleOrNull()
                val cost = uri.getQueryParameter("cost")?.toDoubleOrNull()

                if (odometer != null && gallons != null && cost != null) {
                    viewModel.addGasEntry(odometer, gallons, cost)
                } else {
                    // Optionally, provide feedback to the user about the invalid intent.
                }
            }
        }
    }
}