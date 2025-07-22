package com.benki.lumen

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.benki.lumen.data.SheetEntry
import com.benki.lumen.data.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

// --- ViewModel and State ---

data class MainUiState(
    val sheetLink: String? = null,
    val apiKey: String? = null,
    val lastEntries: List<SheetEntry> = emptyList(),
    val feedbackMessage: String? = null
)

class MainViewModel(
    private val userRepository: UserRepository
) : ViewModel() {
    val uiState: StateFlow<MainUiState> = combine(
        userRepository.lastEntries,
        userRepository.settings
    ) { entries, settings ->
        MainUiState(
            lastEntries = entries,
            sheetLink = settings.sheetUrl,
            apiKey = settings.apiKey
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainUiState()
    )

    private val _feedbackMessage = MutableStateFlow<String?>(null)

    // TODO: Combine feedback message into the main uiState flow
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    fun addGasEntry(odometer: Double, gallons: Double, cost: Double) {
        val entry = SheetEntry(
            date = getCurrentDate(),
            miles = odometer,
            gallons = gallons,
            dollars = cost,
            isSuccess = false // Assuming this will be updated later
        )
        addEntryWithFeedback(entry)
    }

    fun deleteEntry(entryId: String) {
        viewModelScope.launch {
            userRepository.deleteEntry(entryId)
        }
    }

    fun saveSettings(sheetUrl: String, apiKey: String) {
        viewModelScope.launch {
            userRepository.saveSettings(sheetUrl, apiKey)
        }
    }

    fun openSheet(context: Context) {
        val link = uiState.value.sheetLink ?: return
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
        context.startActivity(intent)
    }

    private fun setFeedback(message: String) {
        // Update the feedback message state
        _feedbackMessage.value = message
    }

    fun clearFeedback() {
        _feedbackMessage.value = null
    }

    private fun addEntryWithFeedback(entry: SheetEntry) {
        viewModelScope.launch {
            try {
                userRepository.addEntry(entry)
                setFeedback("Gas entry added successfully.")
            } catch (e: Exception) {
                setFeedback("Failed to add gas entry: ${e.localizedMessage}")
            }
        }                  
    }

    private fun getCurrentDate(): LocalDate {
        return LocalDate.now()
    }

    companion object {
        fun Factory(
            userRepository: UserRepository
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                        @Suppress("UNCHECKED_CAST")
                        return MainViewModel(userRepository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
    }
} 