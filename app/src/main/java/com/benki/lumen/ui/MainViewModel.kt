package com.benki.lumen.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benki.lumen.model.FuelEntry
import com.benki.lumen.repository.FuelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: FuelRepository
) : ViewModel() {

    val entries: StateFlow<List<FuelEntry>> = repository.entries.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun handleIntentUri(intentUri: Uri?) {
        if (intentUri == null) return
        // Expecting uri format: lumen://add?date=2024-01-01&gallons=8.5&miles=220&cost=35.5
        val dateStr = intentUri.getQueryParameter("date") ?: return
        val gallonsStr = intentUri.getQueryParameter("gallons") ?: return
        val milesStr = intentUri.getQueryParameter("miles") ?: return
        val costStr = intentUri.getQueryParameter("cost") ?: return

        val entry = FuelEntry(
            date = LocalDate.parse(dateStr),
            gallons = gallonsStr.toDouble(),
            miles = milesStr.toDouble(),
            cost = costStr.toDouble()
        )
        viewModelScope.launch { repository.addEntry(entry) }
    }

    fun retry(entryId: String) {
        viewModelScope.launch { repository.retry(entryId) }
    }

    fun delete(entryId: String) {
        viewModelScope.launch { repository.delete(entryId) }
    }
}