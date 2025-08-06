package com.benki.lumen

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.benki.lumen.ui.FuelViewModel
import com.benki.lumen.ui.MainScreen
import com.benki.lumen.ui.MainViewModel
import com.benki.lumen.ui.SettingsScreen
import com.benki.lumen.ui.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // MainViewModel will be provided by Hilt
    private val mainViewModel: FuelViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intentUri: Uri? = intent?.data

        setContent {
            // SettingsDataStore is no longer passed directly here.
            // MainViewModel is obtained via Hilt within LumenApp if needed, or passed down.
            LumenApp(intentUri)
        }
    }
}

@Composable
fun LumenApp(
    intentUri: Uri?
    // mainViewModel is now typically accessed via hiltViewModel() in screens that need it
    // or passed down from a Hilt-managed ViewModel at a higher level if preferred.
) {
    val navController = rememberNavController()
    // If LumenApp itself doesn't need MainViewModel directly,
    // it can be removed from here. Screens like MainScreen will get it via hiltViewModel().
    // For this example, I'm assuming MainScreen will fetch its own MainViewModel.
    // Similarly for SettingsScreen and its SettingsViewModel.

    MaterialTheme {
        Surface(modifier = Modifier) {
            NavGraph(navController, intentUri)
        }
    }
}

@Composable
fun NavGraph(
    navController: NavHostController,
    intentUri: Uri?
    // mainViewModel & settingsDataStore removed, will be obtained via hiltViewModel() in composable screens
) {
    NavHost(navController, startDestination = "main") {
        composable("main") {
            // MainViewModel obtained via Hilt within MainScreen
            val mainViewModel: FuelViewModel = hiltViewModel()
            MainScreen(navController, mainViewModel)
            LaunchedEffect(intentUri) {
                mainViewModel.handleIntentUri(intentUri)
            }
        }
        composable("settings") {
            // SettingsViewModel obtained via Hilt within SettingsScreen
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            SettingsScreen(navController, settingsViewModel)
        }
    }
}