package com.benki.lumen.datastore

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.benki.lumen.model.FuelEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// Create DataStore instance at top level as recommended
private val Context.fuelEntriesDataStore by preferencesDataStore(name = "fuel_entries")

/**
 * DataStore for persisting FuelEntry data locally as JSON backup
 */
class FuelEntriesDataStore(private val context: Context) {
    
    private val json = Json { ignoreUnknownKeys = true }
    
    companion object Keys {
        val ENTRIES = stringPreferencesKey("entries")
    }

    val entriesFlow: Flow<List<FuelEntry>> = context.fuelEntriesDataStore.data.map { preferences ->
        val entriesJson = preferences[Keys.ENTRIES] ?: return@map emptyList()
        try {
            json.decodeFromString<List<FuelEntry>>(entriesJson)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun saveEntries(entries: List<FuelEntry>) {
        context.fuelEntriesDataStore.edit { preferences ->
            preferences[Keys.ENTRIES] = json.encodeToString(entries)
        }
    }

    suspend fun addEntry(entry: FuelEntry) {
        context.fuelEntriesDataStore.edit { preferences ->
            val currentEntriesJson = preferences[Keys.ENTRIES] ?: "[]"
            val currentEntries = try {
                json.decodeFromString<List<FuelEntry>>(currentEntriesJson)
            } catch (e: Exception) {
                emptyList()
            }
            val updatedEntries = currentEntries + entry
            preferences[Keys.ENTRIES] = json.encodeToString(updatedEntries)
        }
    }

    suspend fun updateEntry(entryId: String, updatedEntry: FuelEntry) {
        context.fuelEntriesDataStore.edit { preferences ->
            val currentEntriesJson = preferences[Keys.ENTRIES] ?: "[]"
            val currentEntries = try {
                json.decodeFromString<List<FuelEntry>>(currentEntriesJson)
            } catch (e: Exception) {
                emptyList()
            }
            val updatedEntries = currentEntries.map { entry ->
                if (entry.id == entryId) updatedEntry else entry
            }
            preferences[Keys.ENTRIES] = json.encodeToString(updatedEntries)
        }
    }

    suspend fun deleteEntry(entryId: String) {
        context.fuelEntriesDataStore.edit { preferences ->
            val currentEntriesJson = preferences[Keys.ENTRIES] ?: "[]"
            val currentEntries = try {
                json.decodeFromString<List<FuelEntry>>(currentEntriesJson)
            } catch (e: Exception) {
                emptyList()
            }
            val updatedEntries = currentEntries.filterNot { it.id == entryId }
            preferences[Keys.ENTRIES] = json.encodeToString(updatedEntries)
        }
    }

    suspend fun upsertEntry(entry: FuelEntry) {
        context.fuelEntriesDataStore.edit { preferences ->
            val currentEntriesJson = preferences[Keys.ENTRIES] ?: "[]"
            val currentEntries = try {
                json.decodeFromString<MutableList<FuelEntry>>(currentEntriesJson)
            } catch (e: Exception) {
                mutableListOf()
            }

            // Find the index of an existing entry with the same ID
            val existingEntryIndex = currentEntries.indexOfFirst { it.id == entry.id }

            if (existingEntryIndex != -1) {
                // If it exists, replace it at the same position
                currentEntries[existingEntryIndex] = entry
            } else {
                // If it doesn't exist, add it to the end
                currentEntries.add(entry)
            }

            preferences[Keys.ENTRIES] = json.encodeToString(currentEntries)
        }
    }
}