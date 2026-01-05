package app.wazabe.mlauncher.ui

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import app.wazabe.mlauncher.R

class WidgetSettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.widget_preferences, rootKey)
        
        findPreference<androidx.preference.Preference>("add_widget")?.setOnPreferenceClickListener {
            com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.experimental_feature_title)
                .setMessage(R.string.experimental_widgets_message)
                .setPositiveButton(R.string.okay, null)
                .show()
            true
        }
    }
}
