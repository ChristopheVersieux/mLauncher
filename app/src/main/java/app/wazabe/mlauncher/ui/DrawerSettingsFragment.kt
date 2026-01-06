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
        
        findPreference<androidx.preference.Preference>("MANAGE_HIDDEN_APPS")?.setOnPreferenceClickListener {
            val bundle = Bundle().apply {
                putString("flag", app.wazabe.mlauncher.data.Constants.AppDrawerFlag.HiddenApps.toString())
            }
            androidx.navigation.fragment.NavHostFragment.findNavController(this)
                .navigate(R.id.action_drawerSettingsFragment_to_appListFragment, bundle)
            true
        }
    }
}
