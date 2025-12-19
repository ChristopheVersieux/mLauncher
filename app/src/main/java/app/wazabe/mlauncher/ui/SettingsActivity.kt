package app.wazabe.mlauncher.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import app.wazabe.mlauncher.R

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Theme is applied from manifest
        setContentView(R.layout.activity_settings)
        
        // Set up navigation
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.settings_nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        
        // Handle back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }
    
    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.settings_nav_host_fragment)
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
