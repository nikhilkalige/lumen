package com.benki.lumen.ui

import android.app.PendingIntent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benki.lumen.model.FuelEntry
import com.benki.lumen.network.AuthorizationRequiredException
import com.benki.lumen.network.GoogleSheetsService
import com.benki.lumen.repository.FuelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

// Sealed class to represent UI events, including authorization requests
sealed class UiEvent {
    data class ShowAuthorizationPrompt(val pendingIntent: PendingIntent) : UiEvent()
    data class ShowMessage(val message: String) : UiEvent()
    object Loading : UiEvent()
    object DataSynced : UiEvent()
    // Add other UI states/events as needed (e.g., ShowError(message))
}

sealed class FuelOperation {
    data class Add(val entry: FuelEntry) : FuelOperation()
    data class Delete(val entryId: String, val sheetId: Int, val rowNumber: Int) : FuelOperation()
}

data class UiState(
    val isLoading: Boolean = false,
    val lastMessage: String? = null
)

@HiltViewModel
class FuelViewModel @Inject constructor(
    private val fuelRepository: FuelRepository
) : ViewModel() {

    // Expose the list of fuel entries from the repository
    val entries: StateFlow<List<FuelEntry>> = fuelRepository.entries.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // SharedFlow for UI events that the Activity/Fragment will observe
    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent: SharedFlow<UiEvent> = _uiEvent.asSharedFlow()

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var lastFailedOperation: FuelOperation? = null
    private var currentJob: Job? = null

    fun addFuelEntry(entry: FuelEntry) {
        performOperation(FuelOperation.Add(entry))
    }

    fun deleteFuelEntry(entryId: String, sheetId: Int, rowNumber: Int) {
        performOperation(FuelOperation.Delete(entryId, sheetId, rowNumber))
    }

    fun retryLastOperation() {
        lastFailedOperation?.let { performOperation(it) }
    }

    

    fun handleIntentUri(intentUri: Uri?) {
        if (intentUri == null) return

        // Expecting uri format: lumen://add?date=2024-01-01&gallons=8.5&miles=220&cost=35.5
        val gallonsStr = intentUri.getQueryParameter("gallons") ?: return
        val milesStr = intentUri.getQueryParameter("miles") ?: return
        val costStr = intentUri.getQueryParameter("cost") ?: return

        val dateParam = intentUri.getQueryParameter("date")
        val date = dateParam.takeIf { !it.isNullOrBlank() }
            ?.let { LocalDate.parse(it) }
            ?:LocalDate.now()

        val entry = FuelEntry(
            date = date,
            gallons = gallonsStr.toDouble(),
            miles = milesStr.toDouble(),
            cost = costStr.toDouble()
        )
        addFuelEntry(entry)
    }

    private fun performOperation(operation: FuelOperation) {
        currentJob?.cancel()
        currentJob = viewModelScope.launch {
            emitLoading(true)
            try {
                when (operation) {
                    is FuelOperation.Add -> fuelRepository.addEntry(operation.entry)
                    is FuelOperation.Delete -> fuelRepository.delete(
                        operation.entryId,
                    )
                }
                lastFailedOperation = null
                emitEvent(UiEvent.DataSynced)
                emitEvent(UiEvent.ShowMessage("Operation successful!"))
            } catch (e: AuthorizationRequiredException) {
                lastFailedOperation = operation
                e.pendingIntent?.let {
                    emitEvent(UiEvent.ShowAuthorizationPrompt(it))
                } ?: emitEvent(UiEvent.ShowMessage("Authorization required but intent missing."))
            } catch (e: Exception) {
                lastFailedOperation = operation
                emitEvent(UiEvent.ShowMessage("Error: ${e.localizedMessage}"))
            } finally {
                emitLoading(false)
            }
        }
    }

    private suspend fun emitEvent(event: UiEvent) {
        _uiEvent.emit(event)
        if (event is UiEvent.ShowMessage) {
            _uiState.update { it.copy(lastMessage = event.message) }
        }
    }

    private suspend fun emitLoading(loading: Boolean) {
        if (loading) {
            _uiEvent.emit(UiEvent.Loading)
        }
        _uiState.update { it.copy(isLoading = loading) }
    }

    override fun onCleared() {
        super.onCleared()
        currentJob?.cancel()
    }

    // You might also want a function to retry the last failed operation after authorization
    // This would require storing the last requested operation in the ViewModel.
    // For simplicity, we'll assume the UI re-triggers the original action.
}
