package app.wazabe.mlauncher.ui

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import app.wazabe.mlauncher.R

class WidgetSettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.widget_preferences, rootKey)
    }
}
