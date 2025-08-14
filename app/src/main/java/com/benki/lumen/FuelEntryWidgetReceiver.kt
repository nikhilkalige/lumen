package com.benki.lumen

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class FuelEntryWidgetReceiver: GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FuelEntryWidget()
}

//class FuelEntryWidgetReciever : AppWidgetProvider() {
//
//    override fun onUpdate(
//        context: Context,
//        appWidgetManager: AppWidgetManager,
//        appWidgetIds: IntArray
//    ) {
//        for (appWidgetId in appWidgetIds) {
//            val currentWidget = FuelEntryWidget(context,
//                appWidgetManager,
//                appWidgetId,
//                R.layout.stats_widget)
//
//            currentWidget.updateAppWidget()
//        }
//    }
//
//    private fun updateAppWidget(
//        context: Context,
//        appWidgetManager: AppWidgetManager,
//        appWidgetId: Int
//    ) {
//        val views = RemoteViews(context.packageName, R.layout.fuel_entry_widget)
//
//        // **Correctly extract BII parameters from the widget options Bundle**
//        val optionsBundle = appWidgetManager.getAppWidgetOptions(appWidgetId)
//        val bii = optionsBundle.getString(AppActionsWidgetExtension.EXTRA_APP_ACTIONS_BII)
//        val params = optionsBundle.getBundle(AppActionsWidgetExtension.EXTRA_APP_ACTIONS_PARAMS)
//
//        // Example: Customize the widget UI if the "odometer" parameter was provided
//        if (bii == "actions.intent.CREATE_FUEL_ENTRY" && params != null && params.containsKey("odometer")) {
//            val odometerValue = params.getString("odometer")
//            views.setTextViewText(R.id.widget_title, "Entry for $odometerValue miles")
//        }
//
//        // Create a deep link Intent to launch a specific part of your app.
//        val deepLinkIntent = Intent(
//            Intent.ACTION_VIEW,
//            Uri.parse("lumen://gas/"), // Your deep link to the add entry screen
//            context,
//            MainActivity::class.java
//        )
//
//        val pendingIntent = PendingIntent.getActivity(
//            context,
//            appWidgetId, // Use a unique request code for each widget
//            deepLinkIntent,
//            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//        )
//
//        // Set the click listener for the button
//        views.setOnClickPendingIntent(R.id.widget_add_entry_button, pendingIntent)
//
//        // Instruct the widget manager to update the widget
//        appWidgetManager.updateAppWidget(appWidgetId, views)
//    }
//}
