package com.benki.lumen.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benki.lumen.datastore.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        // Load current sheet ID when ViewModel is created
        viewModelScope.launch {
            settingsDataStore.sheetIdFlow.collect { sheetId ->
                _uiState.value = _uiState.value.copy(
                    currentSheetId = sheetId ?: "",
                    // Preserve inputValue if it has been changed by the user,
                    // otherwise initialize with loaded sheetId
                    inputValue = if (_uiState.value.inputValue.isNotEmpty() && _uiState.value.inputValue != _uiState.value.currentSheetId) {
                        _uiState.value.inputValue
                    } else {
                        sheetId ?: ""
                    }
                )
            }
        }
    }

    fun updateInputValue(value: String) {
        _uiState.value = _uiState.value.copy(inputValue = value)
    }

    fun saveSettings(onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            try {
                settingsDataStore.saveSheetId(_uiState.value.inputValue)
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    currentSheetId = _uiState.value.inputValue,
                    // After saving, inputValue and currentSheetId are the same,
                    // so no unsaved changes.
                    // Also, clear any previous error messages on successful save.
                    errorMessage = null
                )
                onSaved()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = e.localizedMessage
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}

data class SettingsUiState(
    val currentSheetId: String = "",
    val inputValue: String = "",
    val isSaving: Boolean = false,
    val errorMessage: String? = null
) {
    val hasChanges: Boolean
        get() = inputValue != currentSheetId && !isSaving // Don't show changes if currently saving
}
