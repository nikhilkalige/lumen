package com.benki.lumen.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.benki.lumen.model.FuelEntry
import java.time.LocalDate
import kotlin.collections.List


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenRoute(
    navController: NavController,
    viewModel: FuelViewModel,
) {
    val entries by viewModel.entries.collectAsState(initial = emptyList())
    val dialogState by viewModel.entryDialogState.collectAsState()

    MainScreen(
        entries = entries,
        onSettingsClick = { navController.navigate("settings") },
        onRetry = { id -> viewModel.retryLastOperation(id) },
        onDelete = { id -> viewModel.deleteFuelEntry(id) },
        dialogState = dialogState,
        onDismissDialog = { viewModel.dismissEntryDialog() },
        onSaveEditedEntry = { gallons, odometer, cost, date ->
            viewModel.saveIncompleteEntry(gallons, odometer, cost, date)
        }
    )
}

/**
 * Pure, previewable UI.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    entries: List<FuelEntry>,
    onSettingsClick: () -> Unit,
    onRetry: (FuelEntry) -> Unit,
    onDelete: (FuelEntry) -> Unit,
    dialogState: EntryDialogState, // New state to control dialogs
    onDismissDialog: () -> Unit, // New callback to dismiss dialogs
    onSaveEditedEntry: (gallons: String, odometer: String, cost: String, date: String?) -> Unit // New callback to save from dialog
) {
    // --- Dialog Management ---
    // Use a 'when' expression to display the correct dialog based on the state.
    when (dialogState) {
        is EntryDialogState.ShowTooMuchMissingError -> {
            MissingInfoDialog(
                state = dialogState,
                onDismiss = onDismissDialog
            )
        }
        is EntryDialogState.ShowEditEntry -> {
            EditEntryDialog(
                state = dialogState,
                onDismiss = onDismissDialog,
                onSave = onSaveEditedEntry
            )
        }
        EntryDialogState.Hidden -> {
            // Do nothing when the state is Hidden
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fuel Logs", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { inner ->
        if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No entries yet",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(4.dp)
            ) {
                items(entries, key = { it.id }) { entry ->
                    EntryCard(
                        entry = entry,
                        onDelete = onDelete,
                        onRetry = onRetry,
                    )
                }
            }
        }
    }
}



// ---- PREVIEWS ----

private fun demoEntries(): List<FuelEntry> = listOf(
    FuelEntry(
        id = "1",
        date = LocalDate.of(2024, 7, 21),
        gallons = 10.5,
        miles = 220.0,
        cost = 47.20,
        status = FuelEntry.Status.SYNCED,
        errorMessage = null,
        sheetRange = "12"
    ),
    FuelEntry(
        id = "2",
        date = LocalDate.of(2024, 7, 21),
        gallons = 9.1,
        miles = 198.0,
        cost = 42.00,
        status = FuelEntry.Status.PENDING,
        errorMessage = null,
        sheetRange = null
    ),
    FuelEntry(
        id = "3",
        date = LocalDate.of(2024, 7, 21),
        gallons = 8.4,
        miles = 175.2,
        cost = 39.75,
        status = FuelEntry.Status.ERROR,
        errorMessage = "Network error",
        sheetRange = null
    ),
)

@Preview(name = "MainScreen – Light", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "MainScreen – Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MainScreenPreview() {
    MaterialTheme {
        MainScreen(
            entries = demoEntries(),
            onSettingsClick = {},
            onRetry = {},
            onDelete = {},
            dialogState = EntryDialogState.Hidden,
            onDismissDialog = {},
            onSaveEditedEntry = { _, _, _, _ -> }
        )
    }
}

@Preview(name = "Edit Dialog", showBackground = true)
@Composable
private fun EditDialogPreview() {
    MaterialTheme {
        MainScreen(
            entries = demoEntries(),
            dialogState = EntryDialogState.ShowEditEntry(
                gallons = "12.5",
                odometer = "12345",
                cost = null, // The cost is the missing field in this preview
                date = "2023-10-27"
            ),
            onSettingsClick = {},
            onRetry = {},
            onDelete = {},
            onDismissDialog = {},
            onSaveEditedEntry = { _, _, _, _ -> }
        )
    }
}

@Preview(name = "Missing Dialog", showBackground = true)
@Composable
private fun MissingDialogPreview() {
    MaterialTheme {
        MainScreen(
            entries = demoEntries(),
            dialogState = EntryDialogState.ShowTooMuchMissingError(
                gallons = "12.5",
                odometer = null,
                cost = null
            ),
            onSettingsClick = {},
            onRetry = {},
            onDelete = {},
            onDismissDialog = {},
            onSaveEditedEntry = { _, _, _, _ -> }
        )
    }
}



/**
 * A dialog that informs the user that too much information is missing.
 */
@Composable
fun MissingInfoDialog(state: EntryDialogState.ShowTooMuchMissingError, onDismiss: () -> Unit) {
    val capturedInfo = buildString {
        appendLine("Missing information")
        appendLine("- Gallons: ${state.gallons ?: "Missing"}")
        appendLine("- Odometer: ${state.odometer ?: "Missing"}")
        appendLine("- Cost: ${state.cost ?: "Missing"}")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Incomplete data") },
        text = { Text(capturedInfo) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}

/**
 * A dialog to input a single missing fuel entry field.
 */
@Composable
fun EditEntryDialog(
    state: EntryDialogState.ShowEditEntry,
    onDismiss: () -> Unit,
    onSave: (gallons: String, odometer: String, cost: String, date: String?) -> Unit
) {
    // Create mutable state for each text field, initialized with values from the dialog state
    var gallons by remember { mutableStateOf(state.gallons ?: "") }
    var odometer by remember { mutableStateOf(state.odometer ?: "") }
    var cost by remember { mutableStateOf(state.cost ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Complete Your Entry") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Confirm received data")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = gallons,
                    onValueChange = { gallons = it },
                    label = { Text("Gallons") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = odometer,
                    onValueChange = { odometer = it },
                    label = { Text("Odometer") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = cost,
                    onValueChange = { cost = it },
                    label = { Text("Total Cost") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(gallons, odometer, cost, state.date)
                },
                // Enable the save button only if all fields have text
                enabled = gallons.isNotBlank() && odometer.isNotBlank() && cost.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
