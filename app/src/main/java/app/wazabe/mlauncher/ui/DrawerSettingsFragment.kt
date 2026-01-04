package app.wazabe.mlauncher.ui

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import app.wazabe.mlauncher.R

class DrawerSettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.drawer_preferences, rootKey)
    }
}
