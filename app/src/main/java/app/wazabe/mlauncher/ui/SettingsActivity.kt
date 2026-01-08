package app.wazabe.mlauncher.ui

import android.os.Bundle
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupActionBarWithNavController
import app.wazabe.mlauncher.R
import com.google.android.material.appbar.MaterialToolbar

class SettingsActivity : BaseActivity(), android.content.SharedPreferences.OnSharedPreferenceChangeListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val toolbar = findViewById<MaterialToolbar>(R.id.settings_toolbar)
        setSupportActionBar(toolbar)
        
        // Always show back arrow
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // Set up navigation
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.settings_nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        setupActionBarWithNavController(navController)
    }

    override fun onResume() {
        super.onResume()
        androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
            .registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
            .unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: android.content.SharedPreferences?, key: String?) {
        if (key == "APP_THEME_COLOR") {
            recreate()
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.settings_nav_host_fragment)
        // If at start destination, finish activity to go back to launcher
        return if (!navController.navigateUp()) {
            finish()
            true
        } else {
            true
        }
    }

    override fun getThemeForColor(color: String): Int {
        return when (color) {
            "Red" -> R.style.Theme_mLauncher_Settings_Red
            "Blue" -> R.style.Theme_mLauncher_Settings_Blue
            "Orange" -> R.style.Theme_mLauncher_Settings_Orange
            "Purple" -> R.style.Theme_mLauncher_Settings_Purple
            "Pink" -> R.style.Theme_mLauncher_Settings_Pink
            "Lime" -> R.style.Theme_mLauncher_Settings_Lime
            "Cyan" -> R.style.Theme_mLauncher_Settings_Cyan
            "Yellow" -> R.style.Theme_mLauncher_Settings_Yellow
            else -> R.style.Theme_mLauncher_Settings // Green Default
        }
    }
}
