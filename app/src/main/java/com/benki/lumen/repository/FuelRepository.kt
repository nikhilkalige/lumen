package com.benki.lumen.repository

import com.benki.lumen.datastore.FuelEntriesDataStore
import com.benki.lumen.model.FuelEntry
import com.benki.lumen.network.GoogleSheetsService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

interface SheetIdProvider {
    suspend operator fun invoke(): String?
}

class FuelRepository @Inject constructor(
    private val service: GoogleSheetsService,
    private val localDataStore: FuelEntriesDataStore,
    private val sheetIdProvider: SheetIdProvider
) {
    val entries: Flow<List<FuelEntry>> = localDataStore.entriesFlow

    suspend fun addEntry(entry: FuelEntry) {
        // Save to local backup first
        localDataStore.addEntry(entry)
        
        // Try to sync with Google Sheets
        val sheetId = sheetIdProvider()
        if (sheetId != null) {
            val updatedEntry = try {
                service.addData(entry, sheetId, "Sheet1")
                entry.copy(status = FuelEntry.Status.SYNCED)
            } catch (e: Exception) {
                entry.copy(status = FuelEntry.Status.ERROR, errorMessage = e.localizedMessage)
                throw e
            }
            localDataStore.updateEntry(entry.id, updatedEntry)
        }
    }

    suspend fun retry(entryId: String) {
        // Get current entries from local store
        val currentEntries = localDataStore.entriesFlow.first()
        val entry = currentEntries.find { it.id == entryId } ?: return
        var retryEntry = entry.copy(status = FuelEntry.Status.PENDING, errorMessage = null)
        
        // Update local store first
        localDataStore.updateEntry(entryId, retryEntry)
        
        // Try to sync with Google Sheets
        val sheetId = sheetIdProvider()
        if (sheetId != null) {
            retryEntry = try {
                service.addData(retryEntry, sheetId, "Sheet1")
                retryEntry.copy(status = FuelEntry.Status.SYNCED)
            } catch (e: Exception) {
                retryEntry.copy(status = FuelEntry.Status.ERROR, errorMessage = e.localizedMessage)
                throw e
            }
            localDataStore.updateEntry(entryId, retryEntry)
        }
    }

    suspend fun delete(entryId: String) {
        // Get current entries from local store
        val currentEntries = localDataStore.entriesFlow.first()
        val entry = currentEntries.find { it.id == entryId } ?: return
        
        // Delete from Google Sheets if it exists there
        val sheetId = sheetIdProvider()
        if (sheetId != null && entry.sheetRowId != null) {
            service.deleteRow(sheetId, 1,  1)
        }
        
        // Delete from local backup
        localDataStore.deleteEntry(entryId)
    }
}