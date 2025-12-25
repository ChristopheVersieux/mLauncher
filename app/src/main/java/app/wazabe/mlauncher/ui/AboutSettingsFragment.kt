package app.wazabe.mlauncher.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import app.wazabe.mlauncher.R

class AboutSettingsFragment : GenericPref() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_about, rootKey)

        findPreference<Preference>("about_header")?.title = getString(R.string.app_name)
        findPreference<Preference>("about_header")?.summary = getString(R.string.created_by)

        findPreference<Preference>("sourceCode")?.setOnPreferenceClickListener {
            openUrl(getString(R.string.github_link))
            true
        }

        // Donations and Credits could be ListPreference or multiple entries.
        // For simplicity and matching the original "links" feel, I'll handle them if I had specific keys.
        // In my XML I added "donations" and "credits" as single preferences.
        // I'll make them open the most prominent link or a chooser.
        
        findPreference<Preference>("donations")?.setOnPreferenceClickListener {
            openUrl(getString(R.string.sponsor_link))
            true
        }

        findPreference<Preference>("credits")?.setOnPreferenceClickListener {
            openUrl(getString(R.string.weather_link))
            true
        }
    }

    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            // Handle error
        }
    }
}
