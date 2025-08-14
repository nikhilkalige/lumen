package com.benki.lumen.ui

import android.app.PendingIntent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benki.lumen.model.FuelEntry
import com.benki.lumen.network.AuthorizationRequiredException
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
    data class Delete(val entry: FuelEntry) : FuelOperation()
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

    fun deleteFuelEntry(entry: FuelEntry) {
        performOperation(FuelOperation.Delete(entry))
    }

    fun retryLastOperation(entry: FuelEntry) {
        performOperation(FuelOperation.Add(entry))
    }

    fun addFuelEntryFromIntent(gallons: String, odometer: String, cost: String, date: String?) {
        // Create the FuelEntry object here in the ViewModel
        try {
            val entryDate = date.takeIf { !it.isNullOrBlank() }
                ?.let { LocalDate.parse(it) }
                ?: LocalDate.now()

            val entry = FuelEntry(
                date = entryDate,
                gallons = gallons.toDouble(),
                miles = odometer.toDouble(),
                cost = cost.toDouble()
            )
            addFuelEntry(entry)
        } catch (_: NumberFormatException) {
            viewModelScope.launch {
                emitEvent(UiEvent.ShowMessage("Error: Invalid number format in deep link."))
            }
        }
    }

    private fun performOperationx(operation: FuelOperation) {
        if (currentJob?.isActive == true) {
            viewModelScope.launch {
                emitEvent(UiEvent.ShowMessage("Operation already in progress."))
            }
            return
        }

        currentJob = viewModelScope.launch {
            emitLoading(true)
            try {
                when (operation) {
                    is FuelOperation.Add -> fuelRepository.addEntry(operation.entry)
                    is FuelOperation.Delete -> fuelRepository.delete(
                        operation.entry.id
                    )
                }

                lastFailedOperation = null
                emitEvent(UiEvent.DataSynced)
                emitEvent(UiEvent.ShowMessage("Operation successful!"))

            } catch (e: AuthorizationRequiredException) {
                lastFailedOperation = operation
                emitEvent(UiEvent.ShowAuthorizationPrompt(e.pendingIntent))
            } catch (e: Exception) {
                lastFailedOperation = operation
                emitEvent(UiEvent.ShowMessage("Error: ${e.localizedMessage}"))
            } finally {
                emitLoading(false)
            }
        }
    }

    private fun performOperation(operation: FuelOperation) {
        if (currentJob?.isActive == true) {
            return
        }

        currentJob = viewModelScope.launch {
            emitLoading(true)
            try {
                when (operation) {
                    is FuelOperation.Add -> fuelRepository.addEntry(operation.entry)
                    is FuelOperation.Delete -> fuelRepository.delete(operation.entry.id)
                }

                // --- Granular logging for the success path ---
                lastFailedOperation = null
                emitEvent(UiEvent.DataSynced)
                emitEvent(UiEvent.ShowMessage("Operation successful!"))

            } catch (e: Exception) {
                if (e is java.util.concurrent.CancellationException) {
                    throw e
                }
                lastFailedOperation = operation
                val uniqueErrorMessage = "THIS IS THE CATCH BLOCK RUNNING - ${e.localizedMessage}"
                emitEvent(UiEvent.ShowMessage(uniqueErrorMessage))
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
}
