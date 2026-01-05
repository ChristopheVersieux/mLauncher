package app.wazabe.mlauncher.ui

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import app.wazabe.mlauncher.R

class HomeSettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.home_preferences, rootKey)
        
        findPreference<androidx.preference.Preference>("HOME_ALIGNMENT")?.setOnPreferenceChangeListener { _, _ ->
            // Delay to ensure preference is saved before recreating
            view?.postDelayed({ requireActivity().recreate() }, 100)
            true
        }
        
        findPreference<androidx.preference.Preference>("HOME_APPS_NUM")?.setOnPreferenceChangeListener { _, _ ->
            view?.postDelayed({ requireActivity().recreate() }, 100)
            true
        }

        setupTextColorPreference()
    }

    private fun setupTextColorPreference() {
        val autoColorPref = findPreference<androidx.preference.SwitchPreferenceCompat>("AUTO_TEXT_COLOR")
        val manualColorPref = findPreference<androidx.preference.ListPreference>("MANUAL_TEXT_COLOR")

        autoColorPref?.onPreferenceChangeListener = androidx.preference.Preference.OnPreferenceChangeListener { _, _ ->
            requireActivity().recreate()
            true
        }

        manualColorPref?.onPreferenceChangeListener = androidx.preference.Preference.OnPreferenceChangeListener { _, _ ->
            requireActivity().recreate()
            true
        }
    }
}
