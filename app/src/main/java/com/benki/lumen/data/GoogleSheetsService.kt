package com.benki.lumen.data

import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.AppendValuesResponse
import com.google.api.services.sheets.v4.model.ValueRange
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials

class GoogleSheetsService(
    credentials: GoogleCredentials,
    private val transport: NetHttpTransport = NetHttpTransport(),
    private val jsonFactory: GsonFactory = GsonFactory.getDefaultInstance()
) {
    private val service: Sheets by lazy {
        val initializer = HttpCredentialsAdapter(credentials)
        Sheets.Builder(transport, jsonFactory, initializer)
            .setApplicationName("Lumen")
            .build()
    }

    fun appendEntry(spreadsheetId: String, entry: SheetEntry): Result<AppendValuesResponse> {
        return try {
            val values = listOf(
                listOf(
                    entry.date.toString(),
                    entry.miles,
                    entry.gallons,
                    entry.dollars
                )
            )
            val body = ValueRange().setValues(values)
            val response = service.spreadsheets().values()
                .append(spreadsheetId, "A1", body)
                .setValueInputOption("USER_ENTERED")
                .execute()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
} 