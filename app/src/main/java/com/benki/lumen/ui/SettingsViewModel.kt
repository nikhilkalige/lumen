package com.benki.lumen.ui

import android.app.PendingIntent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benki.lumen.datastore.SettingsDataStore
import com.benki.lumen.network.AuthorizationRequiredException
import com.benki.lumen.network.GoogleSheetsService
import com.benki.lumen.network.SelectedSpreadsheet
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val googleSheetsService: GoogleSheetsService
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        // Always check for authorization status when the ViewModel is created.
        checkUserIsAuthorized()

        // Listen for changes to the selected sheet information.
        viewModelScope.launch {
            settingsDataStore.sheetInfoFlow.collect { sheetInfo ->
                _uiState.update {
                    it.copy(
                        selectedSheetId = sheetInfo?.id,
                        selectedSheetName = sheetInfo?.name,
                        worksheetName = sheetInfo?.worksheetName
                    )
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

    fun onAuthIntentHandled() {
        _uiState.update { it.copy(authorizationIntent = null) }
    }

    /**
     * Handles the file data received from the web-based picker deep link.
     * This function should be called from your MainActivity's onNewIntent.
     */
    fun onFileSelectedFromWeb(fileId: String, fileName: String, mimeType: String?) {
        viewModelScope.launch {
            Log.d("SettingsViewModel", "Received from web: $fileName ($fileId)")
            settingsDataStore.saveSheet(id = fileId, name = fileName, uri = Uri.EMPTY)
        }
    }

    fun setWorksheetName(name: String) {
        viewModelScope.launch {
            settingsDataStore.saveWorksheetName(name)
        }
    }
}


data class SettingsUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val userEmail: String? = null,
    val selectedSheetId: String? = null,
    val selectedSheetName: String? = null,
    val worksheetName: String? = null,
    val isSheetPickerVisible: Boolean = false,
    val launchSheetPicker: Boolean = false,
    val authorizationIntent: PendingIntent? = null,
    val error: String? = null
)