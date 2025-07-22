package com.benki.lumen.data

import androidx.compose.runtime.Immutable
import java.time.LocalDate
import java.util.UUID

// Represents a single entry from your Google Sheet.
// It's immutable, which is a best practice for Compose state.
@Immutable
data class SheetEntry(
    val id: String = UUID.randomUUID().toString(),
    val date: LocalDate,
    val gallons: Double,
    val miles: Double,
    val dollars: Double,
    val isSuccess: Boolean,
    val sheetRowUrl: String? = null,
    val errorMessage: String? = null
) 