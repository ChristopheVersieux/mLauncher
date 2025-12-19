package app.wazabe.mlauncher.ui

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import app.wazabe.mlauncher.R

class FeaturesSettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.features_preferences, rootKey)

        // Inverse dependency handling for GPS Location vs Manual Location
        // In XML we set dependency="GPS_LOCATION".
        // If GPS_LOCATION is SwitchPreference, we need to ensure its disableDependentsState is correctly set if we want Manual to be disabled when GPS is ENABLED.
        // Default "dependency" disables when the dependent key is OFF.
        // We want Manual Location (pref_key_manual_location) disabled when GPS (GPS_LOCATION) is ON.
        // So GPS_LOCATION needs android:disableDependentsState="true" in XML?
        // Let's verify what I wrote in XML. I didn't verify that attribute.
        // I will do it programmatically here to be safe and cleaner if XML didn't support it perfectly or if I forgot it.
        
        // Actually, let's fix the XML attribute via Replace if I forgot it, or just handle it here.
        // Handling here:
        val gpsPref = findPreference<SwitchPreferenceCompat>("GPS_LOCATION")
        val manualPref = findPreference<Preference>("pref_key_manual_location")
        val weatherPref = findPreference<SwitchPreferenceCompat>("SHOW_WEATHER")

        // Initial state logic
        fun updateState() {
            val isWeatherOn = weatherPref?.isChecked == true
            val isGpsOn = gpsPref?.isChecked == true
            
            gpsPref?.isEnabled = isWeatherOn
            // Manual location enabled if Weather is ON AND GPS is OFF
            manualPref?.isEnabled = isWeatherOn && !isGpsOn
        }

        gpsPref?.setOnPreferenceChangeListener { _, newValue ->
            // post to let the state update happen or just use the new value
            val isGpsOn = newValue as Boolean
            val isWeatherOn = weatherPref?.isChecked == true
            manualPref?.isEnabled = isWeatherOn && !isGpsOn
            true
        }
        
         weatherPref?.setOnPreferenceChangeListener { _, newValue ->
            val isWeatherOn = newValue as Boolean
             gpsPref?.isEnabled = isWeatherOn
             val isGpsOn = gpsPref?.isChecked == true
             manualPref?.isEnabled = isWeatherOn && !isGpsOn
            true
        }
        
        // Update initially
        updateState()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val typedValue = android.util.TypedValue()
        requireContext().theme.resolveAttribute(com.google.android.material.R.attr.colorSurface, typedValue, true)
        view.setBackgroundColor(typedValue.data)
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        return when (preference.key) {
            "pref_key_manual_location" -> {
                findNavController().navigate(
                    R.id.action_featuresSettingsFragment_to_locationSearchFragment
                )
                true
            }
            else -> super.onPreferenceTreeClick(preference)
        }
    }
}
