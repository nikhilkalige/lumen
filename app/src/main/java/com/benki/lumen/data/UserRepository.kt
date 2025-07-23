package com.benki.lumen.data

import com.benki.lumen.Settings
import com.benki.lumen.SheetEntryList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import androidx.datastore.core.DataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import android.util.Log

class UserRepository(
    private val entryDataStore: DataStore<SheetEntryList>,
    private val settingsDataStore: DataStore<Settings>,
    private val sheetsServiceFlow: StateFlow<GoogleSheetsService?>
) {
    init {
        android.util.Log.d("UserRepository", "sheetsServiceFlow instance: ${sheetsServiceFlow.hashCode()}")
    }
    val lastEntries: Flow<List<SheetEntry>> = entryDataStore.data.map { sheetEntryList ->
        sheetEntryList.entriesList
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
            }
            .sortedByDescending { it.date }
    }
    val settings: Flow<Settings> = settingsDataStore.data

    private fun extractSheetId(url: String?): String? {
        if (url == null) return null
        val regex = "/spreadsheets/u/0/d/([a-zA-Z0-9-_]+)".toRegex()
        return regex.find(url)?.groupValues?.get(1)
    }

    private suspend fun persistEntry(entry: SheetEntry) {
        Log.d("UserRepository", "Persisting entry: $entry")
        entryDataStore.updateData { currentEntries ->
            val protoEntry = com.benki.lumen.SheetEntryProto.newBuilder()
                .setId(entry.id)
                .setDate(entry.date.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE))
                .setGallons(entry.gallons)
                .setMiles(entry.miles)
                .setDollars(entry.dollars)
                .setIsSuccess(entry.isSuccess)
                .setSheetRowUrl(entry.sheetRowUrl ?: "")
                .setErrorMessage(entry.errorMessage ?: "")
                .build()
            currentEntries.toBuilder()
                .addEntries(protoEntry)
                .build()
        }
    }

    suspend fun addEntry(entry: SheetEntry) {
        val sheetsService = sheetsServiceFlow.value
        Log.d("UserRepository", "addEntry called. sheetsServiceFlow.value = $sheetsService")
        val currentSettings = settings.first()
        val sheetId = extractSheetId(currentSettings.sheetUrl)

        if (sheetsService == null || sheetId == null) {
            Log.w("UserRepository", "sheetsService or sheetId is null. Persisting entry locally.")
            // Persist the entry locally with a failure status if not signed in or configured
            persistEntry(entry.copy(isSuccess = false, errorMessage = "Please sign in and configure Google Sheets URL in settings."))
            return
        }

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
        entryDataStore.updateData { currentEntries ->
            val indexToRemove = currentEntries.entriesList.indexOfFirst { it.id == entryId }
            if (indexToRemove != -1) {
                currentEntries.toBuilder().removeEntries(indexToRemove).build()
            } else {
                currentEntries
            }
        }
    }

    suspend fun saveSettings(sheetUrl: String, apiKey: String) {
        settingsDataStore.updateData { currentSettings ->
            currentSettings.toBuilder()
                .setSheetUrl(sheetUrl)
                .build()
        }
    }
} 