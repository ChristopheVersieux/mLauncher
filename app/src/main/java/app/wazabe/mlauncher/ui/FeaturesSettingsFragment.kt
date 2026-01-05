package app.wazabe.mlauncher.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import app.wazabe.mlauncher.Mlauncher
import app.wazabe.mlauncher.R
import app.wazabe.mlauncher.helper.utils.AppReloader
import com.github.droidworksstudio.common.AppLogger

class FeaturesSettingsFragment : GenericPref() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.features_preferences, rootKey)

        val gpsPref = findPreference<SwitchPreferenceCompat>("GPS_LOCATION")
        val manualPref = findPreference<Preference>("pref_key_manual_location")
        val weatherPref = findPreference<SwitchPreferenceCompat>("SHOW_WEATHER")

        val themePref = findPreference<Preference>("APP_THEME")

        themePref?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                val themeString = newValue.toString()

                // 1. Map the value (Check your R.xml for the actual entryValues!)
                val mode = when (themeString) {
                    "Light", "0" -> AppCompatDelegate.MODE_NIGHT_NO
                    "Dark", "1" -> AppCompatDelegate.MODE_NIGHT_YES
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
                // 3. Apply Change
                // This static call tells all Activities to recreate with the new configuration
                AppCompatDelegate.setDefaultNightMode(mode)

                true // Return true so the preference actually saves to SharedPreferences
            }




        val fontPref = findPreference<ListPreference>("LAUNCHER_FONT")

        fontPref?.let { pref ->
            val fontEntries = mutableListOf<String>()
            val fontValues = mutableListOf<String>()

            fontEntries.add("System Default")
            fontValues.add("system")

            try {
                // DIAGNOSTIC TOAST 1: Checking if folder exists
                val fontFiles = requireContext().assets.list("fonts")

                if (fontFiles.isNullOrEmpty()) {
                    Toast.makeText(requireContext(), "Error: 'fonts' folder is empty or not found in assets", Toast.LENGTH_LONG).show()
                } else {
                    // DIAGNOSTIC TOAST 2: Showing how many files were found
                    for (fileName in fontFiles) {
                        if (fileName.endsWith(".ttf") || fileName.endsWith(".otf")) {
                            val displayName = fileName.substringBeforeLast(".")
                                .replace("_", " ")
                                .replaceFirstChar { it.uppercase() }

                            fontEntries.add(displayName)
                            fontValues.add("fonts/$fileName")
                        }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Assets Error: ${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }

            pref.entries = fontEntries.toTypedArray()
            pref.entryValues = fontValues.toTypedArray()
        }

        fontPref?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, _ ->
            Mlauncher.reloadFont()

            AppReloader.restartApp(this.context!!)

            true
        }

            // Initial state logic
        fun updateState() {
            AppLogger.e(message= "here")

            val isWeatherOn = weatherPref?.isChecked == true
            val isGpsOn = gpsPref?.isChecked == true

            gpsPref?.isEnabled = isWeatherOn
            // Manual location enabled if Weather is ON AND GPS is OFF

                AppLogger.e(message= "isWeatherOn"+ isWeatherOn)
                AppLogger.e(message= "isGpsOn"+ isGpsOn)

                AppLogger.e(message= "ENABLED"+ ( isWeatherOn && !isGpsOn))
            manualPref?.isEnabled = isWeatherOn && !isGpsOn
        }

        gpsPref?.setOnPreferenceChangeListener { _, newValue ->
            // post to let the state update happen or just use the new value
            updateState()
            true
        }

        weatherPref?.setOnPreferenceChangeListener { _, newValue ->
            updateState()
            true
        }

        updateState()

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val typedValue = android.util.TypedValue()
        requireContext().theme.resolveAttribute(
            com.google.android.material.R.attr.colorSurface,
            typedValue,
            true
        )
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
