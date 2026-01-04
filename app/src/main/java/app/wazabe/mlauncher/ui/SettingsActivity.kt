package app.wazabe.mlauncher.ui

import android.os.Bundle
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupActionBarWithNavController
import app.wazabe.mlauncher.R
import com.google.android.material.appbar.MaterialToolbar

class SettingsActivity : BaseActivity() {

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
}
