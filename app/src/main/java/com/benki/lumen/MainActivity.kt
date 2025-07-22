package com.benki.lumen

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lumen.ui.theme.LumenTheme
import java.text.SimpleDateFormat
import java.util.*

sealed class ScreenState {
    data class GasEntry(val data: GasEntryData) : ScreenState()
    object Hello : ScreenState()
    data class Exercise(val exerciseType: String?) : ScreenState()
    object None : ScreenState()
}

class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "LumenMainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate called")
        enableEdgeToEdge()
        
        // Handle the intent
        val screenState = handleIntent(intent)
        
        setContent {
            LumenTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    when (screenState) {
                        is ScreenState.GasEntry -> GasEntryScreen(
                            gasEntryData = screenState.data,
                            modifier = Modifier.padding(innerPadding)
                        )
                        is ScreenState.Hello -> HelloScreen(modifier = Modifier.padding(innerPadding))
                        is ScreenState.None -> GasEntryScreen(
                            gasEntryData = GasEntryData(),
                            modifier = Modifier.padding(innerPadding)
                        )
                        is ScreenState.Exercise -> ExerciseScreen(exerciseType = screenState.exerciseType, modifier = Modifier.padding(innerPadding))
                    }
                }
            }
        }
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d(TAG, "onNewIntent called")
        if (intent != null) {
            val screenState = handleIntent(intent)
            // Update the UI with new data
            setContent {
                LumenTheme {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        when (screenState) {
                            is ScreenState.GasEntry -> GasEntryScreen(
                                gasEntryData = screenState.data,
                                modifier = Modifier.padding(innerPadding)
                            )
                            is ScreenState.Hello -> HelloScreen(modifier = Modifier.padding(innerPadding))
                            is ScreenState.None -> GasEntryScreen(
                                gasEntryData = GasEntryData(),
                                modifier = Modifier.padding(innerPadding)
                            )
                            is ScreenState.Exercise -> ExerciseScreen(exerciseType = screenState.exerciseType, modifier = Modifier.padding(innerPadding))
                        }
                    }
                }
            }
        }
    }
    
    private fun handleIntent(intent: Intent): ScreenState {
        Log.d(TAG, "handleIntent called")
        Log.d(TAG, "Intent action: ${intent.action}")
        Log.d(TAG, "Intent data: ${intent.data}")
        Log.d(TAG, "Intent extras: ${intent.extras}")
        
        val action = intent.getStringExtra("action")
        Log.d(TAG, "Action extra: $action")
        
        if (action == "add_gas_entry") {
            Log.d(TAG, "Processing add_gas_entry action")
            val data = intent.data
            if (data != null) {
                Log.d(TAG, "Intent data URI: $data")
                return ScreenState.GasEntry(extractGasEntryFromUri(data))
            } else {
                Log.d(TAG, "Intent data is null")
            }
        } else if (action == "say_hello") {
            Log.d(TAG, "Processing say_hello action")
            return ScreenState.Hello
        } else if (intent.hasExtra("exerciseType") || intent.action == "actions.intent.START_EXERCISE") {
            Log.d(TAG, "Processing START_EXERCISE action")
            val exerciseType = intent.getStringExtra("exerciseType")
            Log.d(TAG, "Exercise type: $exerciseType")
            return ScreenState.Exercise(exerciseType)
        } else {
            Log.d(TAG, "Action is not add_gas_entry, say_hello, or START_EXERCISE, action was: $action")
        }
        
        Log.d(TAG, "Returning empty GasEntryData")
        return ScreenState.None
    }
    
    private fun extractGasEntryFromUri(uri: Uri): GasEntryData {
        Log.d(TAG, "extractGasEntryFromUri called with URI: $uri")
        
        val date = uri.getQueryParameter("date")
        val odometerStr = uri.getQueryParameter("odometer")
        val gallonsStr = uri.getQueryParameter("gallons")
        val costStr = uri.getQueryParameter("cost")
        
        Log.d(TAG, "Extracted parameters:")
        Log.d(TAG, "  date: $date")
        Log.d(TAG, "  odometer: $odometerStr")
        Log.d(TAG, "  gallons: $gallonsStr")
        Log.d(TAG, "  cost: $costStr")
        
        val odometer = odometerStr?.toDoubleOrNull()
        val gallons = gallonsStr?.toDoubleOrNull()
        val cost = costStr?.toDoubleOrNull()
        
        Log.d(TAG, "Parsed values:")
        Log.d(TAG, "  odometer: $odometer")
        Log.d(TAG, "  gallons: $gallons")
        Log.d(TAG, "  cost: $cost")
        
        // If no date provided, use current date
        val finalDate = date ?: getCurrentDate()
        Log.d(TAG, "Final date: $finalDate")
        
        val gasEntryData = GasEntryData(
            date = finalDate,
            odometer = odometer,
            gallons = gallons,
            cost = cost
        )
        
        Log.d(TAG, "Created GasEntryData: $gasEntryData")
        return gasEntryData
    }
    
    private fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val currentDate = dateFormat.format(Date())
        Log.d(TAG, "Generated current date: $currentDate")
        return currentDate
    }
}

data class GasEntryData(
    val date: String = "",
    val odometer: Double? = null,
    val gallons: Double? = null,
    val cost: Double? = null
) {
    fun hasData(): Boolean = date.isNotEmpty() || odometer != null || gallons != null || cost != null
}

@Composable
fun GasEntryScreen(gasEntryData: GasEntryData, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Lumen Gas Tracker",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        if (gasEntryData.hasData()) {
            // Display extracted gas entry data
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Gas Entry Details",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    GasEntryRow("Date", gasEntryData.date)
                    GasEntryRow("Odometer", gasEntryData.odometer?.toString() ?: "Not provided")
                    GasEntryRow("Gallons", gasEntryData.gallons?.toString() ?: "Not provided")
                    GasEntryRow("Cost", gasEntryData.cost?.toString() ?: "Not provided")
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = { /* TODO: Save gas entry to database */ },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Gas Entry")
                    }
                }
            }
        } else {
            // Default welcome screen
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Welcome to Lumen!",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "Use Google Assistant to add gas entries by saying:",
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Text(
                        text = "• \"Hey Google, add gas entry\"",
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = "• \"Hey Google, add gas 50000 15 gallons 45\"",
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = "• \"Hey Google, log gas 50000 miles 15 gallons 45 dollars\"",
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun HelloScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Hello from Lumen!",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            text = "This is a greeting from the say_hello intent.",
            fontSize = 16.sp
        )
    }
}

@Composable
fun GasEntryRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun ExerciseScreen(exerciseType: String?, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Exercise Started!",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            text = "Type: ${exerciseType ?: "Unknown"}",
            fontSize = 18.sp
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GasEntryScreenPreview() {
    LumenTheme {
        GasEntryScreen(GasEntryData())
    }
}

@Preview(showBackground = true)
@Composable
fun GasEntryScreenWithDataPreview() {
    LumenTheme {
        GasEntryScreen(
            GasEntryData(
                date = "2024-01-15",
                odometer = 50000.0,
                gallons = 15.5,
                cost = 45.75
            )
        )
    }
}