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
import kotlinx.coroutines.launch

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

    init {
        // Feedback can be its own state flow
        viewModelScope.launch {
            _feedbackMessage.collect { message ->
                // This would be combined into uiState if you want to keep it there
            }
        }
    }

    fun handleIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_VIEW && intent.data != null) {
            val uri = intent.data!!
            if (uri.path?.startsWith("/gas") == true) {
                val odometer = uri.getQueryParameter("odometer")?.toDoubleOrNull()
                val gallons = uri.getQueryParameter("gallons")?.toDoubleOrNull()
                val cost = uri.getQueryParameter("cost")?.toDoubleOrNull()
                val date = getCurrentDate()
                if (odometer != null && gallons != null && cost != null) {
                    addEntryWithFeedback(
                        SheetEntry(
                            date = date,
                            miles = odometer,
                            gallons = gallons,
                            dollars = cost,
                            isSuccess = false // Assuming this will be updated later
                        )
                    )
                } else {
                    setFeedback("Invalid or missing parameters in intent.")
                }
            } else {
                setFeedback("Unrecognized intent path.")
            }
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

    private fun getCurrentDate(): String {
        return java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
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