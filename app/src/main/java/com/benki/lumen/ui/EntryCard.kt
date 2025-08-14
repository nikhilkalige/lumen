package com.benki.lumen.ui

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.benki.lumen.model.FuelEntry
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun EntryCard(
    entry: FuelEntry,
    onDelete: (FuelEntry) -> Unit,
    onRetry: (FuelEntry) -> Unit
) {
    val context = LocalContext.current
    val locale = Locale.getDefault()
    val currencyFormat = NumberFormat.getCurrencyInstance(locale)
    val dataFormat = NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 2 }

    // --- Calculated Values ---
    val costPerGallon = if (entry.gallons > 0) entry.cost / entry.gallons else 0.0
    val mileage = if (entry.gallons > 0) entry.miles / entry.gallons else 0.0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // --- Date (Delete button removed from here) ---
            Text(
                text = entry.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // --- Data Rows (Now includes calculated values) ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                InfoColumn("Miles", dataFormat.format(entry.miles))
                InfoColumn("Gallons", dataFormat.format(entry.gallons))
                InfoColumn("Cost", currencyFormat.format(entry.cost))
                InfoColumn("Cost/Gal", currencyFormat.format(costPerGallon))
                InfoColumn("MPG", dataFormat.format(mileage))
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // --- Action Row (Bottom of the card) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                    // .height(40.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // This part conditionally shows either a success link or an error message.
                if (entry.status == FuelEntry.Status.SYNCED) {
                    // --- SUCCESS STATE ---
                    Text(
                        text = "Synced",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    // Link to sheet is now just an icon
                    IconButton(onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, entry.getSheetUrl()?.toUri())
                        context.startActivity(intent)
                    }) {
                        Icon(
                            imageVector = Icons.Outlined.Link,
                            contentDescription = "View on Google Sheets"
                        )
                    }
                } else {
                    // --- ERROR STATE ---
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = entry.errorMessage ?: "Failed to sync.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f, fill = false) // Prevents text from pushing icons
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { onRetry(entry) }) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Retry entry"
                        )
                    }
                }

                // Delete button is now here for all states
                IconButton(onClick = { onDelete(entry) }) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete entry",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}


@Composable
fun InfoColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Preview
@Composable
fun EntryCardPreview() {
    EntryCard(
        entry = FuelEntry(
            date = LocalDate.of(2024, 7, 21), gallons = 10.523, miles = 320.1, cost = 45.50,
            status = FuelEntry.Status.SYNCED,
            sheetRange = "xyz"
        ),
        onDelete = {},
        onRetry = {}
    )
}

@Preview
@Composable
fun EntryCardErrorPreview() {
    EntryCard(
        entry = FuelEntry(
            date = LocalDate.of(2024, 7, 21), gallons = 10.523, miles = 320.1, cost = 45.50,
            status = FuelEntry.Status.ERROR,
            sheetRange = "xyz"
        ),
        onDelete = {},
        onRetry = {}
    )
}