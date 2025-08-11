package com.benki.lumen.ui

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.benki.lumen.network.SelectedSpreadsheet
import com.benki.lumen.network.createDrivePickerIntent
import com.benki.lumen.network.createFilePickerIntent
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CloudOff // Correct import
import androidx.compose.material.icons.filled.Description // Correct import
import androidx.compose.material.icons.filled.LinkOff // Correct import
import androidx.compose.material.icons.filled.Login // Correct import
import androidx.compose.material.icons.filled.Logout // Correct import
import androidx.compose.material.icons.filled.Person // Correct import
import androidx.compose.ui.text.style.TextAlign

@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val authLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
        onResult = { viewModel.signIn() } // Retry sign-in after auth intent returns
    )

    // Launcher for file picker
    val sheetPickerLauncher = rememberLauncherForActivityResult(
       contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // The user selected a file. Get the Uri and pass it to the ViewModel.
            result.data?.data?.let { uri ->
                 viewModel.onSheetSelectedFromPicker(uri)
                //context.contentResolver.takePersistableUriPermission(
                 //   uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                //)
            //    //iewModel.onSheetPicked(uri)
            }

    }}

    LaunchedEffect(uiState.launchSheetPicker) {
        if (uiState.launchSheetPicker) {
            val intent = createDrivePickerIntent()
            sheetPickerLauncher.launch(intent)
            viewModel.onSheetPickerLaunched()
        }
    }


    uiState.authorizationIntent?.let {
        LaunchedEffect(it) {
            authLauncher.launch(IntentSenderRequest.Builder(it).build())
            viewModel.onAuthIntentHandled()
        }
    }

    SettingsScreenContent(
        uiState = uiState,
        onSignInClick = viewModel::signIn,
        onSignOutClick = viewModel::signOut,
        onSelectSheetClick = viewModel::onSelectSheetClicked,
        //onSheetSelected = viewModel::onSheetSelected,
        //onDismissSheetPicker = viewModel::dismissSheetPicker,
        onBackClick = { navController.popBackStack() }
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenContentX(
    uiState: SettingsUiState,
    onSignInClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onSelectSheetClick: () -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator()
            }
            if (uiState.isLoggedIn) {
                Text("Signed in as: ${uiState.userEmail}")
                Spacer(modifier = Modifier.height(16.dp))
                if (uiState.selectedSheetId != null) {
                    Text("Selected Sheet: ${uiState.selectedSheetName}  [${uiState.selectedSheetId}]")
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onSelectSheetClick) {
                        Text("Change Spreadsheet")
                    }
                } else {
                    Button(onClick = onSelectSheetClick) {
                        Text("Select Spreadsheet")
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
                Button(onClick = onSignOutClick) {
                    Text("Sign Out")
                }
            } else {
                Text("Sign in with your Google Account to sync your fuel entries.")
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onSignInClick) {
                    Text("Sign In with Google")
                }
            }
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenContent(
    uiState: SettingsUiState,
    onSignInClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onSelectSheetClick: () -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sync Settings") }, // More specific title
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (uiState.isLoading) {
                // Keep the loader centered for a clean loading state
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.isLoggedIn) {
                // Display the structured, logged-in state
                LoggedInContent(uiState, onSelectSheetClick, onSignOutClick)
            } else {
                // Display the engaging, logged-out state
                LoggedOutContent(onSignInClick)
            }
        }
    }
}

@Composable
private fun LoggedInContent(
    uiState: SettingsUiState,
    onSelectSheetClick: () -> Unit,
    onSignOutClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.Start // Align content to the start for a standard settings look
    ) {
        // --- Account Info Section ---
        Text(
            text = "ACCOUNT",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                SettingsRow(
                    icon = Icons.Default.Person,
                    label = "Signed in as",
                    value = uiState.userEmail ?: "Loading..."
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- Spreadsheet Section ---
        Text(
            text = "SPREADSHEET",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = onSelectSheetClick,
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                if (uiState.selectedSheetId != null) {
                    SettingsRow(
                        icon = Icons.Default.Description,
                        label = "Syncing to",
                        value = uiState.selectedSheetName ?: "Unnamed Sheet"
                    )
                    Text(
                        text = "ID: ${uiState.selectedSheetId}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 40.dp) // Align with value text
                    )
                } else {
                    SettingsRow(
                        icon = Icons.Default.LinkOff,
                        label = "No spreadsheet selected",
                        value = "Tap to select a sheet to sync your data."
                    )
                }
            }
        }

        // Push Sign Out button to the bottom of the screen
        Spacer(modifier = Modifier.weight(1f))

        OutlinedButton(
            onClick = onSignOutClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Sign Out")
        }
    }
}

@Composable
private fun LoggedOutContent(onSignInClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CloudOff,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Sync Disabled",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Sign in with your Google Account to sync your fuel entries across devices.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onSignInClick,
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
        ) {
            // In a real app, you would use a drawable resource for the Google logo
            Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Sign In with Google")
        }
    }
}

/**
 * A reusable composable for a consistent settings row layout.
 */
@Composable
private fun SettingsRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

//@Composable
//fun SpreadsheetPickerDialog(
//    spreadsheets: List<com.google.api.services.drive.model.File>,
//    onDismiss: () -> Unit,
//    onSheetSelected: (SelectedSpreadsheet) -> Unit
//) {
//    AlertDialog(
//        onDismissRequest = onDismiss,
//        title = { Text("Select a Spreadsheet") },
//        text = {
//            LazyColumn {
//                items(spreadsheets) { sheet ->
//                    ListItem(
//                        headlineContent = { Text(sheet.name) },
//                        modifier = Modifier.clickable { onSheetSelected(sheet) }
//                    )
//                }
//            }
//        },
//        confirmButton = { }
//    )
//}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview_LoggedOut() {
    MaterialTheme {
        SettingsScreenContent(
            uiState = SettingsUiState(isLoggedIn = false),
            onSignInClick = {}, onSignOutClick = {}, onSelectSheetClick = {},
            onBackClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview_LoggedIn_NoSheet() {
    MaterialTheme {
        SettingsScreenContent(
            uiState = SettingsUiState(isLoggedIn = true, userEmail = "test@example.com"),
            onSignInClick = {}, onSignOutClick = {}, onSelectSheetClick = {},
            onBackClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview_LoggedIn_SheetSelected() {
    MaterialTheme {
        SettingsScreenContent(
            uiState = SettingsUiState(
                isLoggedIn = true,
                userEmail = "test@example.com",
                selectedSheetId = "12345",
                selectedSheetName = "My Fuel Log"
            ),
            onSignInClick = {}, onSignOutClick = {}, onSelectSheetClick = {},
            onBackClick = {}
        )
    }
}