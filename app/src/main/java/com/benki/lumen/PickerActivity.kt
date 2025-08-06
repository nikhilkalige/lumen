package com.benki.lumen

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri

class PickerActivity : ComponentActivity() {

   @SuppressLint("SetJavaScriptEnabled")
   override fun onCreate(savedInstanceState: Bundle?) {
       super.onCreate(savedInstanceState)
       // Check if the activity was launched by our deep link from the browser
       if (intent?.action == Intent.ACTION_VIEW && intent.data?.host == "picker-result") {
           // If yes, handle the data that came back
           handleDeepLink(intent)
       } else {
           // If no, this is the initial launch. Open the URL in a browser.
           launchBrowserPicker()
       }

       setContent {
            PickerWebView()
       }
   }

    private fun launchBrowserPicker() {
        val url = "https://lumen.shortcircuits.dev"
        // Create an intent to open the URL in an external browser
        val browserIntent = Intent(Intent.ACTION_VIEW, url.toUri())
        startActivity(browserIntent)

        // Finish this activity immediately. The user will see the browser.
        // When they finish picking a file, the deep link will re-launch this activity.
        finish()
    }

    private fun handleDeepLink(intent: Intent?) {
        if (intent == null) {
            finish() // Can't do anything without an intent
            return
        }
        // Check if the intent is the one we're looking for (from our deep link)
        if (intent.action == Intent.ACTION_VIEW) {
            // The 'S.' prefix in the URI corresponds to a String extra.
            val fileId = intent.getStringExtra("fileId")
            val fileName = intent.getStringExtra("fileName")
            val mimeType = intent.getStringExtra("mimeType")

            // Log to confirm we received the data
            Log.d("PickerActivity", "File ID: $fileId")
            Log.d("PickerActivity", "File Name: $fileName")
            Log.d("PickerActivity", "Mime Type: $mimeType")

            // Check if we have valid data
            if (fileId != null && fileName != null) {
                // Package the result into a new intent to send back
                val resultIntent = Intent().apply {
                    putExtra("fileId", fileId)
                    putExtra("fileName", fileName)
                    putExtra("mimeType", mimeType)
                }

                // Set the result and finish this activity
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PickerWebView() {
    // Use AndroidView to embed the WebView in your Composable UI
    AndroidView(factory = { context ->
        WebView(context).apply {
            settings.javaScriptEnabled = true
            webViewClient = WebViewClient()
            loadUrl("https://lumen.shortcircuits.dev")
        }
    })
}