package com.benki.lumen.ui

import android.content.Intent
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.benki.lumen.datastore.DEFAULT_WORKSHEET_NAME


@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current // <-- Get the context here


    val authLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
        onResult = { viewModel.signIn() } // Retry sign-in after auth intent returns
    )

    // --- EFFECT TO LAUNCH THE PICKER ---
    LaunchedEffect(uiState.launchSheetPicker) {
        if (uiState.launchSheetPicker) {
            val url = "https://lumen.shortcircuits.dev"
            val browserIntent = Intent(Intent.ACTION_VIEW, url.toUri())
            context.startActivity(browserIntent)
            viewModel.onSheetPickerLaunched() // Reset the event flag
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
        onBackClick = { navController.popBackStack() },
        onWorksheetNameChange = viewModel::setWorksheetName
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenContent(
    uiState: SettingsUiState,
    onSignInClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onSelectSheetClick: () -> Unit,
    onBackClick: () -> Unit,
    onWorksheetNameChange: (String) -> Unit
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
                LoggedInContent(uiState, onSelectSheetClick, onSignOutClick, onWorksheetNameChange)
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
    onSignOutClick: () -> Unit,
    onWorksheetNameChange: (String) -> Unit
) {
    var showWorksheetDialog by remember { mutableStateOf(false) }
    var tempWorksheetName by remember(uiState.worksheetName) {
        mutableStateOf(uiState.worksheetName ?: DEFAULT_WORKSHEET_NAME)
    }

    if (showWorksheetDialog) {
        AlertDialog(
            onDismissRequest = { showWorksheetDialog = false },
            title = { Text("Change Worksheet Name") },
            text = {
                OutlinedTextField(
                    value = tempWorksheetName,
                    onValueChange = { tempWorksheetName = it },
                    label = { Text("Worksheet Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onWorksheetNameChange(tempWorksheetName)
                        showWorksheetDialog = false
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showWorksheetDialog = false },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Cancel")
                }
            }
        )
    }

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
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = { showWorksheetDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Worksheet: ${uiState.worksheetName ?: DEFAULT_WORKSHEET_NAME}")
                    }
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
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Logout,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
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
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            // In a real app, you would use a drawable resource for the Google logo
            Icon(
                Icons.AutoMirrored.Filled.Login,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
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

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview_LoggedOut() {
    MaterialTheme {
        SettingsScreenContent(
            uiState = SettingsUiState(isLoggedIn = false),
            onSignInClick = {}, onSignOutClick = {}, onSelectSheetClick = {},
            onBackClick = {}, onWorksheetNameChange = {}
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
            onBackClick = {}, onWorksheetNameChange = {}
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
                selectedSheetName = "My Fuel Log",
                worksheetName = "Entries"
            ),
            onSignInClick = {}, onSignOutClick = {}, onSelectSheetClick = {},
            onBackClick = {}, onWorksheetNameChange = {}
        )
    }
}
