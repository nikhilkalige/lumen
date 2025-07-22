package com.benki.lumen.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.benki.lumen.MainViewModel
import com.benki.lumen.ui.MainRoute
import com.benki.lumen.ui.SettingsScreen

sealed class Screen(val route: String) {
    object Main : Screen("main")
    object Settings : Screen("settings")
}

@Composable
fun LumenNavHost(viewModel: MainViewModel) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Screen.Main.route) {
        composable(Screen.Main.route) {
            MainRoute(
                viewModel = viewModel,
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }
        composable(Screen.Settings.route) {
            val uiState by viewModel.uiState.collectAsState()
            SettingsScreen(
                initialUrl = uiState.sheetLink ?: "",
                initialApiKey = uiState.apiKey ?: "",
                onSave = { url, apiKey ->
                    viewModel.saveSettings(url, apiKey)
                    navController.popBackStack()
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
} 