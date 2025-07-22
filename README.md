# Lumen - Personal Automation Android App

Lumen is a personal automation Android app that integrates with Google Assistant to add gas entries to Google Sheets through voice commands.

## Features

- **Voice Command Integration**: Say "Hey Google, add gas entry with miles, gallons and cost in dollars" to automatically add entries
- **Google Sheets Integration**: Automatically syncs gas entries to your Google Sheets spreadsheet
- **Local Storage**: Stores entries locally using Room database for offline access
- **Modern UI**: Built with Jetpack Compose and Material 3 design
- **Offline Support**: Works offline and syncs when connection is restored

## Voice Command Format

The app recognizes voice commands in the following format:
- "Hey Google, add gas entry with [miles] miles, [gallons] gallons, and [cost] dollars"
- "Add gas entry [miles] [gallons] [cost]"

Example: "Hey Google, add gas entry with 250 miles, 12.5 gallons, and 45.75 dollars"

## Setup Instructions

### 1. Prerequisites

- Android Studio Arctic Fox or later
- Android device with Google Assistant
- Google Cloud Platform account
- Google Sheets API enabled

### 2. Google Cloud Setup

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select existing one
3. Enable Google Sheets API
4. Create a Service Account:
   - Go to IAM & Admin > Service Accounts
   - Create a new service account
   - Download the JSON key file
   - Place it in `app/src/main/assets/` as `service-account-key.json`

### 3. Google Sheets Setup

1. Create a new Google Sheets spreadsheet
2. Share it with your service account email (found in the JSON key file)
3. Create a sheet named "Gas Entries" with headers:
   - Column A: Timestamp
   - Column B: Miles
   - Column C: Gallons
   - Column D: Cost
   - Column E: Location

### 4. App Configuration

1. Update `GoogleSheetsService.kt`:
   ```kotlin
   const val SPREADSHEET_ID = "YOUR_SPREADSHEET_ID" // Replace with your actual spreadsheet ID
   ```

2. Add your `google-services.json` file to the `app/` directory (if using Firebase)

### 5. Build and Install

1. Open the project in Android Studio
2. Sync Gradle files
3. Build the project
4. Install on your device

## Project Structure

```
app/src/main/java/com/lumen/app/
├── LumenApplication.kt          # Main application class
├── data/
│   ├── model/
│   │   └── GasEntry.kt          # Data model for gas entries
│   ├── local/
│   │   ├── AppDatabase.kt       # Room database
│   │   ├── GasEntryDao.kt       # Data access object
│   │   └── Converters.kt        # Type converters
│   └── remote/
│       └── GoogleSheetsService.kt # Google Sheets API service
├── di/
│   ├── DatabaseModule.kt        # Database dependency injection
│   └── NetworkModule.kt         # Network dependency injection
├── repository/
│   └── GasEntryRepository.kt    # Repository pattern implementation
├── assistant/
│   ├── AssistantActivity.kt     # Google Assistant integration
│   └── VoiceCommandProcessor.kt # Voice command processing
└── ui/
    ├── MainActivity.kt          # Main UI activity
    ├── GasTrackerViewModel.kt   # ViewModel for UI logic
    └── theme/                   # UI theme and styling
```

## Dependencies

- **Jetpack Compose**: Modern UI toolkit
- **Room**: Local database
- **Hilt**: Dependency injection
- **Google Sheets API**: Cloud integration
- **Coroutines**: Asynchronous programming
- **Material 3**: Design system

## Usage

1. **Voice Commands**: Use Google Assistant to add gas entries
2. **Manual Entry**: Open the app and tap the microphone button
3. **View Entries**: All entries are displayed in the main screen
4. **Sync**: Entries are automatically synced to Google Sheets

## Troubleshooting

### Voice Commands Not Working
- Ensure Google Assistant is enabled on your device
- Check microphone permissions
- Verify the app is installed and accessible

### Google Sheets Sync Issues
- Verify service account credentials
- Check spreadsheet sharing permissions
- Ensure internet connection
- Review Google Sheets API quotas

### Build Issues
- Sync Gradle files
- Clean and rebuild project
- Check Android Studio version compatibility

## Contributing

This is a personal automation project. Feel free to fork and modify for your own needs.

## License

This project is for personal use only.

## Future Enhancements

- Support for multiple automation types
- Custom voice command patterns
- Advanced Google Sheets integration
- Widget support
- Wear OS integration 