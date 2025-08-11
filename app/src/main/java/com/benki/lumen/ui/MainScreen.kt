package com.benki.lumen.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.benki.lumen.model.FuelEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavController, viewModel: FuelViewModel) {
    val entriesState = viewModel.entries.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Fuel Logs") }, actions = {
            Button(onClick = { navController.navigate("settings") }) { Text("Settings") }
        })

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(entriesState.value) { entry ->
                EntryRow(entry, viewModel)
            }
        }
    }
}

@Composable
fun EntryRow(entry: FuelEntry, viewModel: FuelViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Date: ${entry.date}")
            Text(text = "Gallons: ${entry.gallons}")
            Text(text = "Miles: ${entry.miles}")
            Text(text = "Cost: ${entry.cost}")
            if (entry.status == FuelEntry.Status.ERROR) {
                Text(text = entry.errorMessage ?: "Unknown error", color = MaterialTheme.colorScheme.error)
            }
        }
        if (entry.status == FuelEntry.Status.ERROR) {
            IconButton(onClick = { viewModel.retryLastOperation() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Retry")
            }
        }
        IconButton(onClick = { viewModel.deleteFuelEntry(entry.id, 0, 0) }) {
            Icon(Icons.Default.Delete, contentDescription = "Delete")
        }
    }
} 