package app.wazabe.mlauncher.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import app.wazabe.mlauncher.R
import app.wazabe.mlauncher.data.Prefs
import app.wazabe.mlauncher.MainViewModel
import app.wazabe.mlauncher.ui.components.DialogManager

class NotesSettingsFragment : GenericPref() {

    private val viewModel: MainViewModel by viewModels({ requireActivity() })
    private lateinit var dialogManager: DialogManager
    private lateinit var prefs: Prefs

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_notes, rootKey)
        prefs = Prefs(requireContext())
        dialogManager = DialogManager(requireContext(), requireActivity())

        setupColorPreference("notesBackgroundColor", prefs.notesBackgroundColor) { prefs.notesBackgroundColor = it }
        setupColorPreference("bubbleBackgroundColor", prefs.bubbleBackgroundColor) { prefs.bubbleBackgroundColor = it }
        setupColorPreference("bubbleMessageTextColor", prefs.bubbleMessageTextColor) { prefs.bubbleMessageTextColor = it }
        setupColorPreference("bubbleTimeDateColor", prefs.bubbleTimeDateColor) { prefs.bubbleTimeDateColor = it }
        setupColorPreference("bubbleCategoryColor", prefs.bubbleCategoryColor) { prefs.bubbleCategoryColor = it }
        setupColorPreference("inputMessageColor", prefs.inputMessageColor) { prefs.inputMessageColor = it }
        setupColorPreference("inputMessageHintColor", prefs.inputMessageHintColor) { prefs.inputMessageHintColor = it }
    }

    private fun setupColorPreference(key: String, currentColor: Int, onColorSelected: (Int) -> Unit) {
        findPreference<Preference>(key)?.setOnPreferenceClickListener {
            dialogManager.showColorPickerBottomSheet(
                context = requireContext(),
                color = currentColor,
                title = it.title.toString(),
                onItemSelected = { selectedColor ->
                    onColorSelected(selectedColor)
                    // We might need to refresh the summary or icon to show the new color if we want,
                    // but the original didn't show the color in the list, just in the picker.
                }
            )
            true
        }
    }
}
