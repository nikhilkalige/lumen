package com.benki.lumen

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import com.benki.lumen.data.UserRepository
import com.benki.lumen.ui.navigation.LumenNavHost
import com.example.lumen.ui.theme.LumenTheme

private val Context.dataStore: DataStore<SheetEntryList> by dataStore(
    fileName = "sheet_entries.pb",
    serializer = SheetEntryListSerializer
)

private val Context.settingsDataStore: DataStore<Settings> by dataStore(
    fileName = "settings.pb",
    serializer = SettingsSerializer
)

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels {
        MainViewModel.Factory(UserRepository(dataStore, settingsDataStore))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.handleIntent(intent)
        setContent {
            LumenTheme {
                LumenNavHost(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { viewModel.handleIntent(it) }
    }
}