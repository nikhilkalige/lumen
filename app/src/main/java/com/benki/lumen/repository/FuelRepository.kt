package com.benki.lumen.repository

import android.util.Log
import com.benki.lumen.datastore.FuelEntriesDataStore
import com.benki.lumen.model.FuelEntry
import com.benki.lumen.network.GoogleSheetsService
import com.benki.lumen.network.SelectedSpreadsheet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

interface SheetIdProvider {
    suspend operator fun invoke(): SelectedSpreadsheet?
}

class FuelRepository @Inject constructor(
    private val service: GoogleSheetsService,
    private val localDataStore: FuelEntriesDataStore,
    private val sheetIdProvider: SheetIdProvider
) {
    val entries: Flow<List<FuelEntry>> = localDataStore.entriesFlow

    suspend fun addEntry(entry: FuelEntry) {
        // Save to local backup first
        localDataStore.upsertEntry(entry)

        val sheetInfo = sheetIdProvider()
        if (sheetInfo != null) {
            val updatedEntry = try {
                val range = service.addData(entry, sheetInfo.id, sheetInfo.worksheetName)
                entry.copy(status = FuelEntry.Status.SYNCED, sheetRange = range, spreadsheetId = sheetInfo.id, errorMessage = null)
            } catch (e: Exception) {
                entry.copy(status = FuelEntry.Status.ERROR, errorMessage = e.localizedMessage)
            }
            localDataStore.updateEntry(entry.id, updatedEntry)
        }
    }

    suspend fun delete(entryId: String) {
        // Delete from Google Sheets if it exists there
        val sheetId = sheetIdProvider()
        if (sheetId != null) {
            val rowNumberToDelete = service.findRowNumberByUniqueId(sheetId.id, entryId, sheetId.worksheetName);
            Log.d("FuelRepository", "Row number to delete: $rowNumberToDelete")
            if (rowNumberToDelete != null)
                service.deleteRow(sheetId.id, rowNumberToDelete, sheetId.worksheetName)
        }

        // Delete from local backup
        localDataStore.deleteEntry(entryId)
    }
}