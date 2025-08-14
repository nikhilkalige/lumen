package com.benki.lumen.ui

import android.content.res.Configuration
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.benki.lumen.model.FuelEntry
import com.benki.lumen.ui.components.EntryCard
import java.time.LocalDate


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenRoute(
    navController: NavController,
    viewModel: FuelViewModel,
) {
    val entries by viewModel.entries.collectAsState(initial = emptyList())

    MainScreen(
        entries = entries,
        onSettingsClick = { navController.navigate("settings") },
        onRetry = { id -> viewModel.retryLastOperation(id) },
        onDelete = { id -> viewModel.deleteFuelEntry(id) },
        onBackClick = { navController.popBackStack() },
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
    onBackClick: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fuel Logs", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    //IconButton(onClick = onBackClick) {
                    //    Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back")
                    //}
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
            onBackClick = {},
        )
    }
}
