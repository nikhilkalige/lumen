package com.benki.lumen.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.benki.lumen.MainViewModel
import com.benki.lumen.data.SheetEntry
import com.benki.lumen.ui.components.EntryCard
import java.time.LocalDate
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember

@Composable
fun MainRoute(
    viewModel: MainViewModel,
    onNavigateToSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val feedbackMessage by viewModel.feedbackMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(feedbackMessage) {
        feedbackMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearFeedback()
        }
    }

    MainScreen(
        entries = uiState.lastEntries,
        onNavigateToSettings = onNavigateToSettings,
        snackbarHostState = snackbarHostState,
        onDeleteEntry = { viewModel.deleteEntry(it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    entries: List<SheetEntry>,
    onNavigateToSettings: () -> Unit,
    snackbarHostState: SnackbarHostState,
    onDeleteEntry: (String) -> Unit
) {
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Fuel Log") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Open Settings"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(entries, key = { it.id }) { entry ->
                EntryCard(
                    entry = entry,
                    onDelete = onDeleteEntry
                )
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    val sampleEntries = listOf(
        SheetEntry(date = LocalDate.of(2024, 7, 21), gallons = 10.523, miles = 320.1, dollars = 45.50, isSuccess = true, sheetRowUrl = "https://docs.google.com/spreadsheets/d/example/edit#gid=0&range=A2"),
        SheetEntry(date = LocalDate.of(2024, 7, 10), gallons = 9.876, miles = 295.8, dollars = 42.10, isSuccess = false, errorMessage = "API key invalid."),
        SheetEntry(date = LocalDate.of(2024, 6, 28), gallons = 11.100, miles = 340.5, dollars = 48.05, isSuccess = true, sheetRowUrl = "https://docs.google.com/spreadsheets/d/example/edit#gid=0&range=A4")
    )
    MaterialTheme {
        MainScreen(
            entries = sampleEntries,
            onNavigateToSettings = {},
            snackbarHostState = remember { SnackbarHostState() },
            onDeleteEntry = {}
        )
    }
} 