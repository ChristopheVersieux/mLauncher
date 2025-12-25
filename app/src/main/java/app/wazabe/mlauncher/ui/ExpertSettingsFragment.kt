package app.wazabe.mlauncher.ui

import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import app.wazabe.mlauncher.R
import app.wazabe.mlauncher.data.Prefs
import app.wazabe.mlauncher.helper.utils.AppReloader
import com.github.droidworksstudio.common.isBiometricEnabled

class ExpertSettingsFragment : GenericPref() {

    private lateinit var prefs: Prefs

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_expert, rootKey)
        prefs = Prefs(requireContext())

        // Orientation Lock
        findPreference<SwitchPreferenceCompat>("lockOrientation")?.setOnPreferenceChangeListener { _, newValue ->
            val locked = newValue as Boolean
            if (locked) {
                val currentOrientation = resources.configuration.orientation
                prefs.lockOrientationPortrait = currentOrientation == Configuration.ORIENTATION_PORTRAIT
            }
            AppReloader.restartApp(requireContext())
            true
        }

        // Biometric Lock
        findPreference<SwitchPreferenceCompat>("settingsLocked")?.isVisible = requireContext().isBiometricEnabled()

        // Expert Options (if disabled, navigate back)
        findPreference<SwitchPreferenceCompat>("enableExpertOptions")?.setOnPreferenceChangeListener { _, newValue ->
            val enabled = newValue as Boolean
            if (!enabled) {
                parentFragmentManager.popBackStack()
            }
            true
        }
    }
}
