package app.wazabe.mlauncher.ui.iconpack

import android.os.Bundle
import android.widget.CheckBox
import android.widget.LinearLayout

import app.wazabe.mlauncher.R
import app.wazabe.mlauncher.data.Constants
import app.wazabe.mlauncher.data.Prefs
import app.wazabe.mlauncher.helper.IconCacheTarget
import app.wazabe.mlauncher.helper.IconPackHelper
import app.wazabe.mlauncher.helper.utils.AppReloader
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.concurrent.Executors

class ApplyIconPackActivity : androidx.appcompat.app.AppCompatActivity() {
    private lateinit var prefs: Prefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)

        val packageName = intent.getStringExtra("packageName").toString()
        val packageClass = intent.getStringExtra("packageClass").toString()
        if (packageClass.isNotEmpty()) {
            // Create a vertical LinearLayout programmatically
            val layout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(50, 40, 50, 10)
            }

            // Create the CheckBoxes
            val checkBoxHome = CheckBox(this).apply {
                text = getString(R.string.apply_to_home) // e.g., "Apply to Home"
                isChecked = true // default value
            }

            val checkBoxAppList = CheckBox(this).apply {
                text = getString(R.string.apply_to_app_list) // e.g., "Apply to App List"
                isChecked = true // default value
            }

            // Add the CheckBoxes to the layout
            layout.addView(checkBoxHome)
            layout.addView(checkBoxAppList)

            MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.apply_icon_pack))
                .setMessage(getString(R.string.apply_icon_pack_are_you_sure, packageName))
                .setView(layout)
                .setPositiveButton(getString(R.string.apply)) { _, _ ->

                    val iconPackType = Constants.IconPacks.Custom
                    val customIconPackType = packageClass

                    if (checkBoxHome.isChecked) {
                        val executor = Executors.newSingleThreadExecutor()
                        executor.execute {
                            IconPackHelper.preloadIcons(this, customIconPackType, IconCacheTarget.HOME)
                        }
                        prefs.iconPackHome = iconPackType
                        prefs.customIconPackHome = customIconPackType
                    }

                    if (checkBoxAppList.isChecked) {
                        val executor = Executors.newSingleThreadExecutor()
                        executor.execute {
                            IconPackHelper.preloadIcons(this, customIconPackType, IconCacheTarget.APP_LIST)
                        }
                        prefs.iconPackAppList = iconPackType
                        prefs.customIconPackAppList = customIconPackType
                    }

                    AppReloader.restartApp(this)
                }
                .setNegativeButton(getString(R.string.cancel)) { _, _ ->
                    finish()
                }
                .setCancelable(false)
                .show()

        } else {
            finish()
        }
    }
}
