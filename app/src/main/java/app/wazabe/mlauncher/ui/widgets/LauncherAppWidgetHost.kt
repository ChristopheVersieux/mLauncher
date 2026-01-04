package app.wazabe.mlauncher.ui.widgets

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import android.content.Context

/**
 * Custom AppWidgetHost based on Android Launcher3 implementation.
 * Handles widget lifecycle with proper start/stop listening management.
 */
class LauncherAppWidgetHost(
    context: Context,
    hostId: Int
) : AppWidgetHost(context.applicationContext, hostId) {

    private val context: Context = context.applicationContext
    private var isListening = false

    override fun startListening() {
        if (isListening) return
        try {
            super.startListening()
            isListening = true
        } catch (_: Exception) { }
    }

    override fun stopListening() {
        if (!isListening) return
        try {
            super.stopListening()
            isListening = false
        } catch (_: Exception) { }
    }

    fun createViewSafe(appWidgetId: Int, appWidgetProviderInfo: AppWidgetProviderInfo): AppWidgetHostView? {
        return try {
            createView(context, appWidgetId, appWidgetProviderInfo)
        } catch (_: Exception) {
            null
        }
    }

    fun deleteAppWidgetIdSafe(appWidgetId: Int) {
        try {
            deleteAppWidgetId(appWidgetId)
        } catch (_: Exception) { }
    }

    fun isHostListening(): Boolean = isListening
}
