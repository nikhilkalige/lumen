package com.benki.lumen.network

import android.app.PendingIntent
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.benki.lumen.model.FuelEntry
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.oauth2.Oauth2
import com.google.api.services.oauth2.Oauth2Scopes
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.api.services.sheets.v4.model.AppendValuesResponse
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest
import com.google.api.services.sheets.v4.model.DeleteDimensionRequest
import com.google.api.services.sheets.v4.model.DimensionRange
import com.google.api.services.sheets.v4.model.Request
import com.google.api.services.sheets.v4.model.ValueRange
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.AccessToken
import com.google.auth.oauth2.GoogleCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "GoogleSheetsService"
private const val APP_NAME = "Lumen" // Define app name as a constant

// Add this data class to represent a selected spreadsheet
data class SelectedSpreadsheet(
    val id: String,
    val name: String,
    val uri: Uri
)


/**
 * Helper method to extract file ID from content URI using content resolver
 */
private fun extractIdFromContentUri(uri: Uri): String? {
    return try {
        // Try to get the document ID
        val docId = uri.lastPathSegment?.split(":")?.lastOrNull()
        docId
    } catch (e: Exception) {
        Log.e(TAG, "Could not extract ID from content URI", e)
        null
    }
}

private fun getFileNameFromUri(context: Context, uri: Uri): String? {
    // Use a ContentResolver to query the file's metadata.
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            // Get the index of the DISPLAY_NAME column.
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0) {
                // Return the file name.
                return cursor.getString(nameIndex)
            }
        }
    }
    // Return null if the query fails or the column doesn't exist.
    return null
}


/**
 * Custom exception to signal that user authorization UI needs to be shown.
 */
class AuthorizationRequiredException(
    val pendingIntent: PendingIntent
) : Exception("User authorization required.")

/**
 * Manages all interactions with Google Sheets and Drive APIs.
 * This service handles OAuth2 authorization, token management, and API requests
 * in a centralized and efficient manner.
 */
@Singleton
class GoogleSheetsService @Inject constructor(private val context: Context) {

    private val authorizationClient by lazy { Identity.getAuthorizationClient(context) }
    private val httpTransport by lazy { NetHttpTransport() }
    private val jsonFactory by lazy { GsonFactory.getDefaultInstance() }

    // Use a Mutex to ensure thread-safe access to cached services and credentials
    private val serviceMutex = Mutex()

    // Cache the API service clients for efficiency. @Volatile ensures visibility across threads.
    @Volatile
    private var sheetsService: Sheets? = null

    @Volatile
    private var driveService: Drive? = null

    private val requiredScopes = listOf(
        Scope(SheetsScopes.DRIVE_FILE), // Needed to create/access files opened by the app
        Scope(DriveScopes.DRIVE_READONLY), // Needed to search all of the user's spreadsheets
        Scope(Oauth2Scopes.USERINFO_EMAIL) // Correct scope for fetching user's email
    )

    /**
     * Checks if the user is currently authorized without triggering a UI flow.
     * @return `true` if authorized, `false` otherwise.
     */
    suspend fun isAuthorized(): Boolean {
        return try {
            getCredentials()
            true
        } catch (e: AuthorizationRequiredException) {
            false
        } catch (e: Exception) {
            // Any other exception (network error, service issue, etc.) also means
            // we cannot proceed, so we consider the user not authorized.
            Log.e(TAG, "An unexpected error occurred during authorization check", e)
            false
        }
    }

    /**
     * Retrieves a credential, triggering authorization if needed. This is the core auth function.
     */
    private suspend fun getCredentials(): GoogleCredentials {
        val authRequest = AuthorizationRequest.builder()
            .setRequestedScopes(requiredScopes)
            .build()

        val authResult: AuthorizationResult = try {
            authorizationClient.authorize(authRequest).await()
        } catch (e: Exception) {
            Log.e(TAG, "Authorization failed", e)
            throw e
        }

        if (authResult.hasResolution()) {
            // Authorization is required. Throw exception with the intent for the UI to handle.
            throw AuthorizationRequiredException(authResult.pendingIntent!!)
        }

        // Authorization successful, create and return credentials
        return GoogleCredentials.create(AccessToken(authResult.accessToken, null))
    }

    /**
     * Lazily initializes and returns an authorized Sheets service client.
     * Subsequent calls will return the cached instance.
     */
    private suspend fun getSheetsService(): Sheets = serviceMutex.withLock {
        sheetsService ?: run {
            val credentials = getCredentials()
            Sheets.Builder(httpTransport, jsonFactory, HttpCredentialsAdapter(credentials))
                .setApplicationName(APP_NAME)
                .build().also { sheetsService = it }
        }
    }

    /**
     * Lazily initializes and returns an authorized Drive service client.
     */
    private suspend fun getDriveService(): Drive = serviceMutex.withLock {
        driveService ?: run {
            val credentials = getCredentials()
            Drive.Builder(httpTransport, jsonFactory, HttpCredentialsAdapter(credentials))
                .setApplicationName(APP_NAME)
                .build().also { driveService = it }
        }
    }

    /**
     * Fetches the email address of the signed-in user.
     * Note: Requires the 'userinfo.email' scope.
     */
    suspend fun getSignedInUserEmail(): String? = withContext(Dispatchers.IO) {
        serviceMutex.withLock {
            val credentials = getCredentials()
            val oauth2Service =
                Oauth2.Builder(httpTransport, jsonFactory, HttpCredentialsAdapter(credentials))
                    .setApplicationName(APP_NAME)
                    .build()
            try {
                oauth2Service.userinfo().get().execute()?.email
            } catch (e: Exception) {
                Log.e(TAG, "Could not fetch user email", e)
                null
            }
        }
    }

    /**
     * Appends a new row to the specified spreadsheet.
     */
    suspend fun addData(fuelEntry: FuelEntry, spreadsheetId: String, range: String): String? =
        withContext(Dispatchers.IO) {
            try {
                val rowData = listOf(
                    listOf(
                        fuelEntry.id,
                        fuelEntry.date.toString(),
                        fuelEntry.gallons,
                        fuelEntry.miles,
                        fuelEntry.cost,
                    )
                )
                val body = ValueRange().setValues(rowData)

                val response: AppendValuesResponse = getSheetsService().spreadsheets().values()
                    .append(spreadsheetId, range, body)
                    .setValueInputOption("USER_ENTERED")
                    .setIncludeValuesInResponse(false) // We don't need the data echoed back
                    .execute()

                Log.d(
                    TAG,
                    "Data added successfully to spreadsheet $spreadsheetId at range ${response.updates.updatedRange}"
                )
                response.updates.updatedRange

            } catch (e: Exception) {
                Log.e(TAG, "Error adding data to spreadsheet", e)
                throw e
            }
        }

    /**
     * Deletes a specific row from a spreadsheet.
     */
    suspend fun deleteRow(spreadSheetId: String, rowNumber: Int) =
        withContext(Dispatchers.IO) {
            try {
                val deleteRequest = DeleteDimensionRequest().setRange(
                    DimensionRange()
                        .setSheetId(0)
                        .setDimension("ROWS")
                        .setStartIndex(rowNumber - 1) // API is 0-indexed
                        .setEndIndex(rowNumber)
                )

                val batchUpdateRequest = BatchUpdateSpreadsheetRequest()
                    .setRequests(listOf(Request().setDeleteDimension(deleteRequest)))

                getSheetsService().spreadsheets()
                    .batchUpdate(spreadSheetId, batchUpdateRequest)
                    .execute()

                Log.d(TAG, "Row $rowNumber deleted successfully from sheet $spreadSheetId")
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting row", e)
                throw e
            }
        }

    suspend fun findRowNumberByUniqueId(spreadsheetId: String, uniqueId: String): Int? =
        withContext(Dispatchers.IO) {
            try {
                val idColumnRange = "Sheet1!A:A" // Ensure this is the correct column for your IDs

                val response: ValueRange = getSheetsService().spreadsheets().values()
                    .get(spreadsheetId, idColumnRange)
                    .setMajorDimension("COLUMNS")
                    .execute()

                val valuesList = response.values.toList()

                @Suppress("UNCHECKED_CAST")
                val actualData = valuesList[2] as? List<List<Any>>
                val data: List<String> =
                    actualData?.getOrNull(0)?.map { it.toString() } ?: emptyList()
                // val rowIndex = data.indexOf(uniqueId)
                val rowIndex = data.indexOfFirst { cellValue ->
                    // Explicitly convert the cell's value to a String before comparing
                    cellValue.toString() == uniqueId
                }

                if (rowIndex != -1) {
                    val rowNumber = rowIndex + 1
                    return@withContext rowNumber
                } else {
                    return@withContext null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Step ERROR: An exception occurred.", e)
                return@withContext null
            }
        }

    /**
     * Gets spreadsheet details from a selected URI
     * @param uri The URI returned from the file picker
     * @return SelectedSpreadsheet object with ID and name, or null if unable to process
     */
    suspend fun getSpreadsheetFromUri(uri: Uri): SelectedSpreadsheet? =
        withContext(Dispatchers.IO) {
            try {
                // Extract the file ID from the URI
                getFileNameFromUri(context, uri)?.let { filename ->
                    searchForSheetByExactName(filename)?.let { sheetId ->
                        SelectedSpreadsheet(sheetId, filename, uri)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting spreadsheet from URI", e)
                null
            }
        }

    /**
     * Method 3: Search for spreadsheet by exact name match
     * This is more reliable when dealing with content URIs
     */
    suspend fun searchForSheetByExactName(name: String): String? = withContext(Dispatchers.IO) {
        try {
            // Remove file extension if present
            val cleanName = name.replace(".gsheet", "").replace(".pdf", "").trim()

            // Search for the exact spreadsheet name
            val files = getDriveService().files().list()
                .setQ(
                    "name = '${
                        cleanName.replace(
                            "'",
                            "\\'"
                        )
                    }' and mimeType = 'application/vnd.google-apps.spreadsheet'"
                )
                .setSpaces("drive")
                .setFields("files(id, name)")
                .execute()

            // Return the first matching file
            files.files.firstOrNull()?.id
        } catch (e: Exception) {
            Log.e(TAG, "Error searching for sheet by name: $name", e)
            null
        }
    }

}