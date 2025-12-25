package app.wazabe.mlauncher.ui

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import app.wazabe.mlauncher.R
import app.wazabe.mlauncher.data.Constants.AppDrawerFlag
import app.wazabe.mlauncher.helper.utils.PrivateSpaceManager

class SettingsPreferenceFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = app.wazabe.mlauncher.data.Prefs(requireContext())
        val privateSpaceManager = PrivateSpaceManager(requireContext())

        findPreference<Preference>("pref_key_private_spaces")?.isVisible = privateSpaceManager.isPrivateSpaceSetUp()
        findPreference<Preference>("pref_key_expert")?.isVisible = prefs.enableExpertOptions
        
         // Set correct icon/status for private spaces if visible
        if (privateSpaceManager.isPrivateSpaceSetUp()) {
             val isLocked = privateSpaceManager.isPrivateSpaceLocked()
             val iconRes = if (isLocked) R.drawable.private_profile_on else R.drawable.private_profile_off
             val titleRes = if (isLocked) R.string.locked else R.string.unlocked
             findPreference<Preference>("pref_key_private_spaces")?.let {
                 it.setIcon(iconRes)
                 it.title = getString(R.string.private_space, getString(titleRes))
             }
        }
        
        findPreference<Preference>("pref_key_about")?.title = getString(R.string.about_settings_title, getString(R.string.app_name))
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        return when (preference.key) {
            "pref_key_features" -> {
                findNavController().navigate(R.id.action_settingsFragment_to_featuresSettingsFragment)
                true
            }
            "pref_key_look_feel" -> {
                findNavController().navigate(R.id.action_settingsFragment_to_lookFeelSettingsFragment)
                true
            }
            "pref_key_gestures" -> {
                findNavController().navigate(R.id.action_settingsFragment_to_gesturesSettingsFragment)
                true
            }
            "pref_key_notes" -> {
                findNavController().navigate(R.id.action_settingsFragment_to_notesSettingsFragment)
                true
            }
            "pref_key_private_spaces" -> {
                 if (PrivateSpaceManager(requireContext()).isPrivateSpaceSetUp(showToast = false, launchSettings = false)) {
                    PrivateSpaceManager(requireContext()).togglePrivateSpaceLock(
                        showToast = false,
                        launchSettings = false
                    )
                }
                true
            }
            "pref_key_hidden_apps" -> {
                findNavController().navigate(
                    R.id.action_settingsFragment_to_appListFragment,
                    bundleOf("flag" to AppDrawerFlag.HiddenApps.toString())
                )
                true
            }
            "pref_key_favorite_apps" -> {
                findNavController().navigate(
                    R.id.action_settingsFragment_to_appFavoriteFragment,
                    bundleOf("flag" to AppDrawerFlag.SetHomeApp.toString())
                )
                true
            }
            "pref_key_advanced" -> {
                findNavController().navigate(R.id.action_settingsFragment_to_advancedSettingsFragment)
                true
            }
            "pref_key_expert" -> {
                findNavController().navigate(R.id.action_settingsFragment_to_expertSettingsFragment)
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
