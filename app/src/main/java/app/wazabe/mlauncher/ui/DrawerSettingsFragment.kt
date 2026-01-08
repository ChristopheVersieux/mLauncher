package app.wazabe.mlauncher.ui

import android.os.Bundle
import android.util.Log
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

        findPreference<androidx.preference.Preference>("AUTO_TAG")?.setOnPreferenceClickListener {
            val viewModel = androidx.lifecycle.ViewModelProvider(requireActivity())[app.wazabe.mlauncher.MainViewModel::class.java]
            
            fun showAnalysisDialog(apps: List<app.wazabe.mlauncher.data.AppListItem>) {
                // Group by profile type
                val categories = apps.groupBy { it.profileType }
                
                com.github.droidworksstudio.common.AppLogger.d("AutoTagDebug", "--- Auto Tag Analysis ---")
                com.github.droidworksstudio.common.AppLogger.d("AutoTagDebug", "Total apps: ${apps.size}")
                categories.forEach { (type, list) ->
                    com.github.droidworksstudio.common.AppLogger.d("AutoTagDebug", "Profile '$type': ${list.size} apps")
                }
                
                val context = requireContext()
                val message = StringBuilder()
                
                if (categories.size <= 1) {
                    message.append("Only one category found. No tagging needed.")
                } else {
                    categories.forEach { (category, categoryApps) ->
                        val friendlyName = when(category) {
                            "SYSTEM" -> "Personal"
                            "WORK" -> "Work"
                            "PRIVATE" -> "Private"
                            else -> category
                        }
                        val examples = categoryApps.take(3).joinToString(", ") { it.label }
                        message.append("$friendlyName: ${categoryApps.size} apps\n")
                        message.append("(e.g., $examples)\n\n")
                    }
                }
    
                val builder = com.google.android.material.dialog.MaterialAlertDialogBuilder(context)
                    .setTitle("Auto Tag Analysis")
                    .setMessage(message.toString().trim())
                    .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                        dialog.dismiss()
                    }
    
                if (categories.size > 1) {
                    builder.setPositiveButton("Validate Auto Tag") { dialog, _ ->
                         // TODO: Implement actual tagging logic
                         android.widget.Toast.makeText(context, "Auto Tagging initiated... (Logic pending)", android.widget.Toast.LENGTH_SHORT).show()
                         dialog.dismiss()
                    }
                }
                
                builder.show()
            }

            val currentList = viewModel.appList.value
            if (currentList.isNullOrEmpty()) {
                android.widget.Toast.makeText(requireContext(), "Analyzing apps...", android.widget.Toast.LENGTH_SHORT).show()
                // Wait for data
                val observer = object : androidx.lifecycle.Observer<List<app.wazabe.mlauncher.data.AppListItem>?> {
                    override fun onChanged(list: List<app.wazabe.mlauncher.data.AppListItem>?) {
                        if (!list.isNullOrEmpty()) {
                            viewModel.appList.removeObserver(this)
                            showAnalysisDialog(list)
                        }
                    }
                }
                viewModel.appList.observe(this, observer)
            } else {
                showAnalysisDialog(currentList)
            }
            true
        }
    }
}
