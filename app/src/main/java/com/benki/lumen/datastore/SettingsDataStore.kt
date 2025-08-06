package com.benki.lumen.datastore

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.benki.lumen.network.SelectedSpreadsheet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


const val DEFAULT_WORKSHEET_NAME: String = "Sheet1"

// Create DataStore instance at top level as recommended
private val Context.lumenDataStore by preferencesDataStore(name = "lumen_settings")

/**
 * A simple wrapper around Jetpack DataStore to persist user settings such as the Google Sheet ID
 * and the latest OAuth access & refresh tokens.
 */
class SettingsDataStore(private val context: Context) {

    companion object Keys {
        val SHEET_ID = stringPreferencesKey("sheet_id")
        val SHEET_NAME = stringPreferencesKey("sheet_name")
        val SHEET_URI = stringPreferencesKey("sheet_uri")
        val WORKSHEET_NAME = stringPreferencesKey("worksheet_name")
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
    }

    val sheetInfoFlow: Flow<SelectedSpreadsheet?> = context.lumenDataStore.data.map {
        val sheetId = it[Keys.SHEET_ID]
        val sheetName = it[Keys.SHEET_NAME]
        val sheetUri = it[Keys.SHEET_URI]
        val worksheetName = it[Keys.WORKSHEET_NAME] ?: DEFAULT_WORKSHEET_NAME
        if (sheetId != null && sheetName != null) {
            SelectedSpreadsheet(
                id = sheetId,
                name = sheetName,
                uri = sheetUri?.toUri() ?: Uri.EMPTY,
                worksheetName
            )
        } else {
            null
        }
    }

    val accessTokenFlow: Flow<String?> = context.lumenDataStore.data.map { it[Keys.ACCESS_TOKEN] }

    val refreshTokenFlow: Flow<String?> = context.lumenDataStore.data.map { it[Keys.REFRESH_TOKEN] }

    suspend fun saveSheet(id: String, name: String, uri: Uri) {
        context.lumenDataStore.edit {
            it[Keys.SHEET_ID] = id
            it[Keys.SHEET_NAME] = name
            it[Keys.SHEET_URI] = uri.toString()
        }
    }

    suspend fun clearSheet() {
        context.lumenDataStore.edit {
            it.remove(Keys.SHEET_ID)
            it.remove(Keys.SHEET_NAME)
            it.remove(Keys.SHEET_URI)
        }
    }

    suspend fun saveWorksheetName(name: String) {
       context.lumenDataStore.edit {
           it[Keys.WORKSHEET_NAME] = name;
       }
    }

    suspend fun saveAccessToken(token: String) {
        context.lumenDataStore.edit { it[Keys.ACCESS_TOKEN] = token }
    }

    suspend fun saveRefreshToken(token: String) {
        context.lumenDataStore.edit { it[Keys.REFRESH_TOKEN] = token }
    }
}