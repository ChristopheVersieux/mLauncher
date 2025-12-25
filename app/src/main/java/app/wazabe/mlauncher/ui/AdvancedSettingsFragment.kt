package app.wazabe.mlauncher.ui

import android.os.Bundle
import android.os.Process
import android.view.View
import androidx.fragment.app.viewModels
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import app.wazabe.mlauncher.MainViewModel
import app.wazabe.mlauncher.R
import app.wazabe.mlauncher.helper.communitySupportButton
import app.wazabe.mlauncher.helper.helpFeedbackButton
import app.wazabe.mlauncher.helper.openAppInfo
import app.wazabe.mlauncher.helper.checkWhoInstalled
import app.wazabe.mlauncher.helper.utils.AppReloader
import app.wazabe.mlauncher.ui.components.DialogManager
import com.github.droidworksstudio.common.share.ShareUtils

class AdvancedSettingsFragment : GenericPref() {

    private val viewModel: MainViewModel by viewModels({ requireActivity() })
    private lateinit var dialogManager: DialogManager
    private lateinit var shareUtils: ShareUtils

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_advanced, rootKey)
        dialogManager = DialogManager(requireContext(), requireActivity())
        shareUtils = ShareUtils(requireContext(), requireActivity())

        findPreference<Preference>("appInfo")?.apply {
            summary = getString(R.string.advanced_settings_app_info_description, getString(R.string.app_version))
            setOnPreferenceClickListener {
                openAppInfo(requireContext(), Process.myUserHandle(), requireContext().packageName)
                true
            }
        }

        findPreference<Preference>("resetDefaultLauncher")?.setOnPreferenceClickListener {
            viewModel.resetDefaultLauncherApp(requireContext())
            true
        }

        findPreference<Preference>("restartApp")?.setOnPreferenceClickListener {
            AppReloader.restartApp(requireContext())
            true
        }

        findPreference<Preference>("exitLauncher")?.setOnPreferenceClickListener {
            requireActivity().finish()
            true
        }

        findPreference<Preference>("backupRestore")?.setOnPreferenceClickListener {
            dialogManager.showBackupRestoreBottomSheet()
            true
        }

        findPreference<Preference>("saveLoadTheme")?.setOnPreferenceClickListener {
            dialogManager.showSaveLoadThemeBottomSheet()
            true
        }

        findPreference<Preference>("wotdSettings")?.setOnPreferenceClickListener {
            dialogManager.showSaveDownloadWOTDBottomSheet()
            true
        }

        findPreference<Preference>("helpFeedback")?.setOnPreferenceClickListener {
            helpFeedbackButton(requireContext())
            true
        }

        findPreference<Preference>("communitySupport")?.setOnPreferenceClickListener {
            communitySupportButton(requireContext())
            true
        }

        findPreference<Preference>("shareApp")?.setOnPreferenceClickListener {
            shareUtils.showMaterialShareDialog(
                requireContext(),
                getString(R.string.share_application),
                checkWhoInstalled(requireContext())
            )
            true
        }
    }
}
