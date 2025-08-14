package com.benki.lumen.ui

import android.app.PendingIntent
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.benki.lumen.datastore.SettingsDataStore
import com.benki.lumen.model.FuelEntry
import com.benki.lumen.network.AuthorizationRequiredException
import com.benki.lumen.network.GoogleSheetsService
import com.benki.lumen.network.SelectedSpreadsheet
import com.benki.lumen.network.createFilePickerIntent
import com.google.api.services.sheets.v4.model.Sheet
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject


private fun getFileNameFromUri(context: Context, uri: Uri): String? {
    // Use a ContentResolver to query the file's metadata.
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            // Get the index of the DISPLAY_NAME column.
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0) {
                // Return the file name.
                return cursor.getString(nameIndex)
            }
        }
    }
    // Return null if the query fails or the column doesn't exist.
    return null
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val googleSheetsService: GoogleSheetsService
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsDataStore.sheetInfoFlow.collect { sheetInfo ->
                _uiState.update { it.copy(selectedSheetId = sheetInfo?.id, selectedSheetName = sheetInfo?.name) }
                if (sheetInfo?.id != null) {
                    checkUserIsAuthorized()
                }
            }
        }
    }

    private fun checkUserIsAuthorized() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                if (googleSheetsService.isAuthorized()) {
                    val userEmail = googleSheetsService.getSignedInUserEmail()
                    _uiState.update { it.copy(isLoggedIn = true, userEmail = userEmail, isLoading = false) }
                } else {
                    _uiState.update { it.copy(isLoggedIn = false, isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun signIn() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val userEmail = googleSheetsService.getSignedInUserEmail()
                _uiState.update { it.copy(isLoggedIn = true, userEmail = userEmail, isLoading = false) }
            } catch (e: AuthorizationRequiredException) {
                _uiState.update { it.copy(isLoading = false, authorizationIntent = e.pendingIntent) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            // googleSheetsService.signOut()
            settingsDataStore.clearSheet()
            _uiState.value = SettingsUiState()
        }
    }

    /**
     * Signals the UI to launch the system file picker.
     */
    fun onSelectSheetClicked() {
        _uiState.update { it.copy(launchSheetPicker = true) }
    }

    /**
     * The UI calls this immediately after launching the picker to reset the event flag.
     */
    fun onSheetPickerLaunched() {
        _uiState.update { it.copy(launchSheetPicker = false) }
    }

    fun onSheetSelectedFromPicker(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val sheet = googleSheetsService.getSpreadsheetFromUri(uri)
                Log.d("SettingsViewModel", "Selected sheet: $sheet")

                if (sheet!= null) {
                    settingsDataStore.saveSheet(sheet)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            selectedSheetId = sheet.id,
                            selectedSheetName = sheet.name
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Could not identify the selected sheet.") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

//    fun showSheetPicker() {
//        viewModelScope.launch {
//            _uiState.update { it.copy(isLoading = true) }
//            try {
//                val spreadsheets = googleSheetsService.listSpreadsheets()
//                _uiState.update { it.copy(isLoading = false, spreadsheets = spreadsheets, isSheetPickerVisible = true) }
//            } catch (e: Exception) {
//                _uiState.update { it.copy(isLoading = false, error = e.message) }
//            }
//        }
//    }

//    fun onSheetSelected(sheetId: String, sheetName: String) {
//        viewModelScope.launch {
//            settingsDataStore.saveSheetId(sheetId)
//            _uiState.update { it.copy(selectedSheetId = sheetId, selectedSheetName = sheetName, isSheetPickerVisible = false) }
//        }
//    }

    fun dismissSheetPicker() {
        _uiState.update { it.copy(isSheetPickerVisible = false) }
    }

    fun onAuthIntentHandled() {
        _uiState.update { it.copy(authorizationIntent = null) }
    }

    fun onErrorDismissed() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Handles the file data received from the web-based picker deep link.
     * This function should be called from your MainActivity's onNewIntent.
     */
    fun onFileSelectedFromWeb(fileId: String, fileName: String, mimeType: String?) {
        viewModelScope.launch {
            Log.d("SettingsViewModel", "Received from web: $fileName ($fileId)")
            // Here, you need to save the sheet info, similar to onSheetSelectedFromPicker.
            // You may need to create a simple Sheet data class if you don't have one.
            // data class Sheet(val id: String, val name: String)
            val sheet = SelectedSpreadsheet(id = fileId, name = fileName, uri = Uri.EMPTY)
            settingsDataStore.saveSheet(sheet)

            // Update the UI state to reflect the new selection
            _uiState.update {
                it.copy(
                    selectedSheetId = fileId,
                    selectedSheetName = fileName
                )
            }
        }
    }
}

data class SettingsUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val userEmail: String? = null,
    val selectedSheetId: String? = null,
    val selectedSheetName: String? = null,
    // val spreadsheets: List<com.google.api.services.drive.model.File> = emptyList(),
    val isSheetPickerVisible: Boolean = false,
    val launchSheetPicker: Boolean = false,
    val authorizationIntent: PendingIntent? = null,
    val error: String? = null
)
