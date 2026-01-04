package app.wazabe.mlauncher.ui

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import app.wazabe.mlauncher.R

class SettingsPreferenceFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        findPreference<Preference>("pref_key_about")?.title = getString(R.string.about_settings_title, getString(R.string.app_name))
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        return when (preference.key) {
            "pref_key_general" -> {
                findNavController().navigate(R.id.action_settingsFragment_to_generalSettingsFragment)
                true
            }
            "pref_key_widget" -> {
                findNavController().navigate(R.id.action_settingsFragment_to_widgetSettingsFragment)
                true
            }
            "pref_key_home" -> {
                findNavController().navigate(R.id.action_settingsFragment_to_homeSettingsFragment)
                true
            }
            "pref_key_drawer" -> {
                findNavController().navigate(R.id.action_settingsFragment_to_drawerSettingsFragment)
                true
            }
            "pref_key_about" -> {
                findNavController().navigate(R.id.action_settingsFragment_to_aboutSettingsFragment)
                true
            }
            else -> super.onPreferenceTreeClick(preference)
        }
    }
}
