package com.benki.lumen.data

import androidx.datastore.core.DataStore
import com.benki.lumen.Settings
import com.benki.lumen.SheetEntryList
import com.benki.lumen.SheetEntryProto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class UserRepository(
    private val sheetEntryDataStore: DataStore<SheetEntryList>,
    private val settingsDataStore: DataStore<Settings>
) {
    val lastEntries: Flow<List<SheetEntry>> = sheetEntryDataStore.data
        .map { sheetEntryList ->
            sheetEntryList.entriesList
                .sortedByDescending { it.date } // Assuming date is a string that can be sorted
                .map { proto ->
                SheetEntry(
                    id = proto.id,
                    date = LocalDate.parse(proto.date),
                    gallons = proto.gallons,
                    miles = proto.miles,
                    dollars = proto.dollars,
                    isSuccess = proto.isSuccess,
                    sheetRowUrl = proto.sheetRowUrl.takeIf { it.isNotEmpty() },
                    errorMessage = proto.errorMessage.takeIf { it.isNotEmpty() }
                )
            }.take(5)
        }

    val settings: Flow<Settings> = settingsDataStore.data

    suspend fun addEntry(entry: SheetEntry) {
        val currentSettings = settings.first()
        val sheetId = extractSheetId(currentSettings.sheetUrl)
        val apiKey = currentSettings.apiKey

        if (sheetId == null || apiKey.isNullOrEmpty()) {
            // Persist the entry locally with a failure status
            persistEntry(entry.copy(isSuccess = false, errorMessage = "Sheet ID or API key not set"))
            return
        }

        val sheetsService = GoogleSheetsService(apiKey)
        val result = withContext(Dispatchers.IO) {
            sheetsService.appendEntry(sheetId, entry)
        }

        val updatedEntry = if (result.isSuccess) {
            entry.copy(isSuccess = true, sheetRowUrl = result.getOrNull()?.updates?.updatedRange)
        } else {
            entry.copy(isSuccess = false, errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Unknown error")
        }
        persistEntry(updatedEntry)
    }

    suspend fun deleteEntry(entryId: String) {
        sheetEntryDataStore.updateData { currentEntries ->
            val updatedEntries = currentEntries.entriesList.filterNot { it.id == entryId }
            currentEntries.toBuilder()
                .clearEntries()
                .addAllEntries(updatedEntries)
                .build()
        }
    }

    private suspend fun persistEntry(entry: SheetEntry) {
        sheetEntryDataStore.updateData { currentEntries ->
            currentEntries.toBuilder()
                .addEntries(
                    SheetEntryProto.newBuilder()
                        .setId(entry.id)
                        .setDate(entry.date.format(DateTimeFormatter.ISO_LOCAL_DATE))
                        .setGallons(entry.gallons)
                        .setMiles(entry.miles)
                        .setDollars(entry.dollars)
                        .setIsSuccess(entry.isSuccess)
                        .setSheetRowUrl(entry.sheetRowUrl ?: "")
                        .setErrorMessage(entry.errorMessage ?: "")
                        .build()
                )
                .build()
        }
    }

    private fun extractSheetId(url: String): String? {
        return url.split("/d/")[1].split("/")[0]
    }

    suspend fun saveSettings(sheetUrl: String, apiKey: String) {
        settingsDataStore.updateData {
            it.toBuilder()
                .setSheetUrl(sheetUrl)
                .setApiKey(apiKey)
                .build()
        }
    }
} 