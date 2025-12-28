package com.github.droidworksstudio.common

import android.os.Bundle
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics

object AnalyticsHelper {
    private val analytics: FirebaseAnalytics by lazy { Firebase.analytics }

    fun logEvent(eventName: String, params: Bundle? = null) {
        analytics.logEvent(eventName, params)
    }

    /**
     * Legacy method to match the old CrashHandler.logUserAction interface
     */
    fun logUserAction(action: String) {
        val bundle = Bundle().apply {
            putString("action_description", action)
        }
        logEvent("user_action", bundle)
    }
}
