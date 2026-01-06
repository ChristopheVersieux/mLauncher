package app.wazabe.mlauncher.ui

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import app.wazabe.mlauncher.MainActivity
import app.wazabe.mlauncher.R
import app.wazabe.mlauncher.data.Constants
import app.wazabe.mlauncher.data.Prefs
import app.wazabe.mlauncher.helper.IconCacheTarget
import app.wazabe.mlauncher.helper.emptyString
import app.wazabe.mlauncher.ui.components.DialogManager
import app.wazabe.mlauncher.ui.iconpack.CustomIconSelectionActivity
import com.github.droidworksstudio.common.AppLogger

class LookFeelSettingsFragment : PreferenceFragmentCompat() {

    private lateinit var dialogManager: DialogManager
    private lateinit var prefs: Prefs

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_lookfeel, rootKey)
        prefs = Prefs(requireContext())
        dialogManager = DialogManager(requireContext(), requireActivity())

        findPreference<Preference>("OPEN_WALLPAPER_SETTINGS")?.setOnPreferenceClickListener {
            try {
                // Try standard wallpaper picker
                val intent = Intent(Intent.ACTION_SET_WALLPAPER)
                startActivity(intent)
            } catch (e: Exception) {
                try {
                    // Fallback to system settings wallpaper entry
                    val intent = Intent("android.settings.WALLPAPER_SETTINGS")
                    startActivity(intent)
                } catch (e2: Exception) {
                    // Final fallback
                    val intent = Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_LAUNCHER)
                        component = ComponentName("com.android.settings", "com.android.settings.Settings")
                    }
                    try {
                        startActivity(intent)
                    } catch (e3: Exception) {
                        AppLogger.e("Wallpaper", "Could not open settings", e3)
                    }
                }
            }
            true
        }

        // --- Side Effects (Immediate Action) ---
        findPreference<SwitchPreferenceCompat>("showStatusBar")?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                (activity as? MainActivity)?.toggleStatusBar(newValue as Boolean)
                true
            }

        findPreference<SwitchPreferenceCompat>("showNavigationBar")?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                (activity as? MainActivity)?.toggleNavigationBar(newValue as Boolean)
                true
            }

        // --- Custom UI (Color Pickers) ---
        val colorPrefs = mapOf(
            "backgroundColor" to R.string.background_color,
            "appColor" to R.string.app_color,
            "dateColor" to R.string.date_color,
            "clockColor" to R.string.clock_color,
            "alarmClockColor" to R.string.alarm_color,
            "batteryColor" to R.string.battery_color,
            "shortcutIconsColor" to R.string.shortcuts_color
        )

        colorPrefs.forEach { (key, titleRes) ->
            findPreference<Preference>(key)?.setOnPreferenceClickListener {
                val currentColor = when (key) {
                    "backgroundColor" -> prefs.backgroundColor
                    "appColor" -> prefs.appColor
                    "dateColor" -> prefs.dateColor
                    "clockColor" -> prefs.clockColor
                    "alarmClockColor" -> prefs.alarmClockColor
                    "batteryColor" -> prefs.batteryColor
                    "shortcutIconsColor" -> prefs.shortcutIconsColor
                    else -> 0
                }
                dialogManager.showColorPickerBottomSheet(
                    requireContext(),
                    getString(titleRes),
                    currentColor
                ) { newColor ->
                    when (key) {
                        "backgroundColor" -> prefs.backgroundColor = newColor
                        "appColor" -> prefs.appColor = newColor
                        "dateColor" -> prefs.dateColor = newColor
                        "clockColor" -> prefs.clockColor = newColor
                        "alarmClockColor" -> prefs.alarmClockColor = newColor
                        "batteryColor" -> prefs.batteryColor = newColor
                        "shortcutIconsColor" -> prefs.shortcutIconsColor = newColor
                    }
                }
                true
            }
        }

        // --- Icon Packs ---
        setupIconPackPreference("iconPackHome", IconCacheTarget.HOME)
        setupIconPackPreference("iconPackAppList", IconCacheTarget.APP_LIST)
    }

    private fun setupIconPackPreference(key: String, target: IconCacheTarget) {
        findPreference<androidx.preference.ListPreference>(key)?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                val newIconPack = Constants.IconPacks.valueOf(newValue as String)
                if (newIconPack == Constants.IconPacks.Custom) {
                    openCustomIconSelectionDialog(target)
                } else {
                    if (target == IconCacheTarget.HOME) {
                        prefs.customIconPackHome = emptyString()
                        prefs.iconPackHome = newIconPack
                    } else {
                        prefs.customIconPackAppList = emptyString()
                        prefs.iconPackAppList = newIconPack
                    }
                }
                true
            }
    }

    private fun openCustomIconSelectionDialog(target: IconCacheTarget) {
        val intent = Intent(requireContext(), CustomIconSelectionActivity::class.java).apply {
            putExtra("icon_cache_target", target.name)
        }
        startActivity(intent)
    }
}
