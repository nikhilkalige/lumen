package com.benki.lumen.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController

@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Show error snackbar if there's an error
    uiState.errorMessage?.let { errorMessage ->
        LaunchedEffect(errorMessage) {
            // Could show a snackbar here
            viewModel.clearError()
        }
    }

    SettingsScreenContent(
        uiState = uiState,
        onInputValueChange = viewModel::updateInputValue,
        onSaveClick = { viewModel.saveSettings { navController.popBackStack() } },
        onBackClick = { navController.popBackStack() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenContent(
    uiState: SettingsUiState,
    onInputValueChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Google Sheets Configuration",
                            style = MaterialTheme.typography.titleMedium
                        )
                        
                        OutlinedTextField(
                            value = uiState.inputValue,
                            onValueChange = onInputValueChange,
                            label = { Text("Google Sheet ID") },
                            placeholder = { Text("Enter your Google Sheet ID") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = !uiState.isSaving
                        )
                        
                        Text(
                            text = "Enter the ID of your Google Sheet where fuel entries will be stored.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                FilledTonalButton(
                    onClick = onSaveClick,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isSaving && uiState.hasChanges
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Saving...")
                    } else {
                        Text("Save Settings")
                    }
                }
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    MaterialTheme {
        SettingsScreenContent(
            uiState = SettingsUiState(
                currentSheetId = "1BvFvgqT9X8sQJ5kR3pL2mH6nK9wE7dC4sA2bZ1yU",
                inputValue = "1BvFvgqT9X8sQJ5kR3pL2mH6nK9wE7dC4sA2bZ1yU",
                isSaving = false
            ),
            onInputValueChange = {},
            onSaveClick = {},
            onBackClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Empty Sheet ID")
@Composable
fun SettingsScreenEmptyPreview() {
    MaterialTheme {
        SettingsScreenContent(
            uiState = SettingsUiState(
                currentSheetId = "",
                inputValue = "",
                isSaving = false
            ),
            onInputValueChange = {},
            onSaveClick = {},
            onBackClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Saving State")
@Composable
fun SettingsScreenSavingPreview() {
    MaterialTheme {
        SettingsScreenContent(
            uiState = SettingsUiState(
                currentSheetId = "1BvFvgqT9X8sQJ5kR3pL2mH6nK9wE7dC4sA2bZ1yU",
                inputValue = "1NewSheetId123456789",
                isSaving = true
            ),
            onInputValueChange = {},
            onSaveClick = {},
            onBackClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Has Changes")
@Composable
fun SettingsScreenHasChangesPreview() {
    MaterialTheme {
        SettingsScreenContent(
            uiState = SettingsUiState(
                currentSheetId = "1BvFvgqT9X8sQJ5kR3pL2mH6nK9wE7dC4sA2bZ1yU",
                inputValue = "1NewSheetId123456789",
                isSaving = false
            ),
            onInputValueChange = {},
            onSaveClick = {},
            onBackClick = {}
        )
    }
}