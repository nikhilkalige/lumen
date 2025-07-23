package com.benki.lumen

import androidx.lifecycle.ViewModel
import com.benki.lumen.data.GoogleSheetsService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class GoogleSheetsServiceViewModel : ViewModel() {
    private val _serviceFlow = MutableStateFlow<GoogleSheetsService?>(null)
    val serviceFlow: StateFlow<GoogleSheetsService?> = _serviceFlow

    fun setService(service: GoogleSheetsService) {
        _serviceFlow.value = service
    }
} 