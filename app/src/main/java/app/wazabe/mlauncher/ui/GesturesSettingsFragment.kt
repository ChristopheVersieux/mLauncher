package app.wazabe.mlauncher.ui

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import app.wazabe.mlauncher.R
import app.wazabe.mlauncher.data.Constants.Action
import app.wazabe.mlauncher.data.Constants.AppDrawerFlag
import app.wazabe.mlauncher.data.Prefs
import app.wazabe.mlauncher.MainViewModel

class GesturesSettingsFragment : GenericPref() {

    private val viewModel: MainViewModel by viewModels({ requireActivity() })
    private lateinit var prefs: Prefs

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_gestures, rootKey)
        prefs = Prefs(requireContext())

        setupGesturePreference("doubleTapAction", AppDrawerFlag.SetDoubleTap)
        setupGesturePreference("clickClockAction", AppDrawerFlag.SetClickClock)
        setupGesturePreference("clickDateAction", AppDrawerFlag.SetClickDate)
        setupGesturePreference("clickAppUsageAction", AppDrawerFlag.SetAppUsage)
        setupGesturePreference("clickFloatingAction", AppDrawerFlag.SetFloating)
        
        setupGesturePreference("swipeUpAction", AppDrawerFlag.SetShortSwipeUp)
        setupGesturePreference("swipeDownAction", AppDrawerFlag.SetShortSwipeDown)
        setupGesturePreference("swipeLeftAction", AppDrawerFlag.SetShortSwipeLeft)
        setupGesturePreference("swipeRightAction", AppDrawerFlag.SetShortSwipeRight)
        
        setupGesturePreference("longSwipeUpAction", AppDrawerFlag.SetLongSwipeUp)
        setupGesturePreference("longSwipeDownAction", AppDrawerFlag.SetLongSwipeDown)
        setupGesturePreference("longSwipeLeftAction", AppDrawerFlag.SetLongSwipeLeft)
        setupGesturePreference("longSwipeRightAction", AppDrawerFlag.SetLongSwipeRight)
    }

    private fun setupGesturePreference(key: String, flag: AppDrawerFlag) {
        findPreference<ListPreference>(key)?.setOnPreferenceChangeListener { _, newValue ->
            val actionName = newValue as String
            val action = try {
                Action.valueOf(actionName)
            } catch (e: Exception) {
                Action.Disabled
            }

            if (action == Action.OpenApp) {
                showAppList(flag)
            }
            true
        }
    }

    private fun showAppList(flag: AppDrawerFlag) {
        viewModel.getHiddenApps()
        findNavController().navigate(
            R.id.action_gesturesSettingsFragment_to_appListFragment,
            bundleOf(
                "flag" to flag.toString(),
                "includeRecentApps" to false,
                "includeHiddenApps" to true
            )
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val typedValue = android.util.TypedValue()
        requireContext().theme.resolveAttribute(com.google.android.material.R.attr.colorSurface, typedValue, true)
        view.setBackgroundColor(typedValue.data)
    }
}
