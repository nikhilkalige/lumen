package com.benki.lumen.network

//import com.benki.lumen.BuildConfig // For CLIENT_ID
import android.app.PendingIntent
import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import com.benki.lumen.datastore.SettingsDataStore
import com.benki.lumen.model.FuelEntry
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest
import com.google.api.services.sheets.v4.model.DeleteDimensionRequest
import com.google.api.services.sheets.v4.model.DimensionRange
import com.google.api.services.sheets.v4.model.Request
import com.google.api.services.sheets.v4.model.ValueRange
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.AccessToken
import com.google.auth.oauth2.GoogleCredentials
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

const val TAG = "GoogleSheetsService"

/**
 * Responsible for performing network operations against Google Sheets REST API.
 * Uses [CredentialManager] to obtain OAuth2 tokens following best practices.
 */
@Singleton
class GoogleSheetsService @Inject constructor(
    private val context: Context,
    private val settings: SettingsDataStore,
) {

    private val authorizationClient by lazy { Identity.getAuthorizationClient(context) }

    private val requiredScopes = listOf(
        Scope(SheetsScopes.DRIVE_FILE)
    )

    /**
     * Initiates the Google Sheets API authorization flow and returns an authorized Sheets service instance.
     * This function should be called from a UI component (Activity/Fragment) that can handle
     * the authorization intent.
     *
     * @param onAuthorizationNeeded A callback function that will be invoked if authorization
     * is required. It provides an [PendingIntent] which
     * your UI component should launch using `ActivityResultLauncher`.
     * @return An authorized [Sheets] service instance.
     * @throws Exception if authorization fails or is cancelled.
     */
    suspend fun getAuthorizedSheetsService(): Sheets {
        return withContext(Dispatchers.IO) {
            val authorizationRequest = AuthorizationRequest.builder()
                .setRequestedScopes(requiredScopes)
                .build()

            try {
                // Attempt to authorize silently (e.g., if user already granted permissions)
                val authorizationResult =
                    authorizationClient.authorize(authorizationRequest).await()

                // Use hasResolution() to determine if user interaction is needed.
                if (!authorizationResult.hasResolution()) {
                    Log.d(TAG, "Authorization already granted (silent authorization).")
                    buildSheetsService(authorizationResult)
                } else {

                    Log.d(TAG, "Authorization needed, launching UI.")
                    val pendingIntent = authorizationResult.pendingIntent
                    Log.d(TAG, "Authorization needed; throwing exception with PendingIntent.")
                    throw AuthorizationRequiredException(pendingIntent = pendingIntent)
                    // If authorization is needed, trigger the callback to launch the UI
                    // The actual UI launch happens in the Activity/Fragment
//                    authorizationResult.pendingIntent?.let {
//                        onAuthorizationNeeded(it) // Pass the PendingIntent directly
//                    } ?: run {
//                        // This case should ideally not happen if hasResolution() is true,
//                        // but it's good for robustness.
//                        Log.e(TAG, "hasResolution() is true but pendingIntent is null.")
//                        throw Exception("Authorization UI intent is missing.")
//                    }
                    // We expect the UI to call setAuthorizationResult() later
                    // This part needs to be handled by the calling UI component.
                    // For simplicity, we'll throw an exception here, but in a real app,
                    // you might use a shared flow/callback to resume this coroutine
                    // after the UI handles the result.
                    // throw AuthorizationRequiredException("User authorization required.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during authorization check: ${e.message}", e)
                throw e
            }
        }
    }

    /**
     * Builds the Google Sheets service client using the provided authorization result.
     */
    private fun buildSheetsService(authorizationResult: AuthorizationResult): Sheets {
        val accessToken = authorizationResult.accessToken
        // Note: AuthorizationClient handles refresh tokens internally.
        // You generally don't need to manually manage refresh tokens with this API.
        val credentials = GoogleCredentials.create(
            AccessToken(
                accessToken,
                null
            )
        ) // Expiration time can be null if managed by client library

        return Sheets.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            HttpCredentialsAdapter(credentials)
        )
            .setApplicationName("Lumen")
            .build()
    }


    /**
     * Adds a new row of data to the specified Google Sheet.
     *
     * @param fuelEntry The [FuelEntry] object containing the data to add.
     * @param spreadsheetId The ID of the spreadsheet.
     * @param range The A1 notation or R1C1 notation of the range where data should be appended.
     * E.g., "Sheet1!A1" will append to the first available row in Sheet1.
     * @param onAuthorizationNeeded Callback for when authorization UI needs to be launched.
     * @throws Exception if an API error occurs or authorization fails.
     */
    suspend fun addData(
        fuelEntry: FuelEntry,
        spreadsheetId: String,
        range: String,
    ) {
        return withContext(Dispatchers.IO) {
            try {
                val sheetsService = getAuthorizedSheetsService()

                // Create a list of lists for the row data
                val rowData = listOf(
                    listOf(
                        fuelEntry.date,
                        fuelEntry.gallons,
                        fuelEntry.miles,
                        fuelEntry.cost,
                    )
                )

                val body = ValueRange().setValues(rowData)

                sheetsService.spreadsheets().values()
                    .append(spreadsheetId, range, body)
                    .setValueInputOption("USER_ENTERED") // How input data is interpreted
                    .execute()

                Log.d(TAG, "Data added successfully to spreadsheet $spreadsheetId in range $range")
            } catch (e: AuthorizationRequiredException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error adding data to spreadsheet: ${e.message}", e)
                throw e
            }
        }
    }

    /**
     * Deletes a specific row from a Google Sheet.
     *
     * @param spreadsheetId The ID of the spreadsheet.
     * @param sheetId The GID (Grid ID) of the specific sheet within the spreadsheet.
     * You can find this in the URL of your Google Sheet (e.g., .../edit#gid=123456789).
     * @param rowNumber The 1-based index of the row to delete. For example, 1 for the first row.
     * @param onAuthorizationNeeded Callback for when authorization UI needs to be launched.
     * @throws Exception if an API error occurs or authorization fails.
     */
    suspend fun deleteRow(
        spreadsheetId: String,
        sheetId: Int,
        rowNumber: Int,
    ) {
        return withContext(Dispatchers.IO) {
            try {
                val sheetsService = getAuthorizedSheetsService()

                // Rows are 0-indexed in the API, so adjust the 1-based rowNumber
                val startIndex = rowNumber - 1
                val endIndex = rowNumber // Delete only one row

                val deleteRequest = DeleteDimensionRequest()
                    .setRange(
                        DimensionRange()
                            .setSheetId(sheetId)
                            .setDimension("ROWS")
                            .setStartIndex(startIndex)
                            .setEndIndex(endIndex)
                    )

                val request = Request().setDeleteDimension(deleteRequest)
                val batchUpdateRequest = BatchUpdateSpreadsheetRequest()
                    .setRequests(listOf(request))

                sheetsService.spreadsheets()
                    .batchUpdate(spreadsheetId, batchUpdateRequest)
                    .execute()

                Log.d(
                    TAG,
                    "Row $rowNumber deleted successfully from sheet $sheetId in spreadsheet $spreadsheetId"
                )
            } catch (e: AuthorizationRequiredException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting row: ${e.message}", e)
                throw e
            }
        }
    }


    companion object {
        // CLIENT_ID is now accessed via BuildConfig.GOOGLE_SHEETS_CLIENT_ID
        // Ensure it's set up in local.properties and build.gradle.kts
    }

    /**
     * Custom exception to signal that user authorization UI needs to be shown.
     */
    class AuthorizationRequiredException(
        message: String = "User authorization required.",
        val pendingIntent: PendingIntent? = null // Now includes the PendingIntent
    ) : Exception(message)
}
