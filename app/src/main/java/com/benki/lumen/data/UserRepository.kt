package com.benki.lumen.data

import androidx.datastore.core.DataStore
import com.benki.lumen.Settings
import com.benki.lumen.SheetEntryList
import com.benki.lumen.SheetEntryProto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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
                    date = proto.date,
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
        sheetEntryDataStore.updateData { currentEntries ->
            currentEntries.toBuilder()
                .addEntries(
                    SheetEntryProto.newBuilder()
                        .setId(entry.id)
                        .setDate(entry.date)
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

    suspend fun saveSettings(sheetUrl: String, apiKey: String) {
        settingsDataStore.updateData {
            it.toBuilder()
                .setSheetUrl(sheetUrl)
                .setApiKey(apiKey)
                .build()
        }
    }
} 