package app.wazabe.mlauncher.ui

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import app.wazabe.mlauncher.R

class DrawerSettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.drawer_preferences, rootKey)
        
        findPreference<androidx.preference.Preference>("DRAWER_ALIGNMENT")?.setOnPreferenceChangeListener { _, _ ->
            // Delay to ensure preference is saved before recreating
            view?.postDelayed({ requireActivity().recreate() }, 100)
            true
        }
    }
}
