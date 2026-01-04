package app.wazabe.mlauncher.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.ListPreference
import androidx.preference.Preference
import app.wazabe.mlauncher.Mlauncher
import app.wazabe.mlauncher.R
import app.wazabe.mlauncher.helper.utils.AppReloader

class GeneralSettingsFragment : GenericPref() {
    
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.general_preferences, rootKey)
        
        setupThemePreference()
        setupLanguagePreference()
        setupFontPreference()
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
    
    private fun setupThemePreference() {
        val themePref = findPreference<ListPreference>("APP_THEME")
        
        themePref?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            val themeString = newValue.toString()
            val mode = when (themeString) {
                "Light", "0" -> AppCompatDelegate.MODE_NIGHT_NO
                "Dark", "1" -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            AppCompatDelegate.setDefaultNightMode(mode)
            true
        }
    }
    
    private fun setupLanguagePreference() {
        val langPref = findPreference<Preference>("pref_app_language")
        
        langPref?.setOnPreferenceClickListener {
            // Open system per-app language settings (Android 13+)
            val intent = Intent(Settings.ACTION_APP_LOCALE_SETTINGS).apply {
                data = Uri.fromParts("package", requireContext().packageName, null)
            }
            startActivity(intent)
            true
        }
    }
    
    private fun setupFontPreference() {
        val fontPref = findPreference<ListPreference>("LAUNCHER_FONT")
        
        fontPref?.let { pref ->
            val fontEntries = mutableListOf<String>()
            val fontValues = mutableListOf<String>()
            
            fontEntries.add("System Default")
            fontValues.add("system")
            
            try {
                val fontFiles = requireContext().assets.list("fonts")
                if (!fontFiles.isNullOrEmpty()) {
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
                e.printStackTrace()
            }
            
            pref.entries = fontEntries.toTypedArray()
            pref.entryValues = fontValues.toTypedArray()
        }
        
        fontPref?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, _ ->
            Mlauncher.reloadFont()
            AppReloader.restartApp(requireContext())
            true
        }
    }
}
