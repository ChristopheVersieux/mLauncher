package app.wazabe.mlauncher.helper.utils

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import app.wazabe.mlauncher.data.Prefs
import app.wazabe.mlauncher.helper.hideNavigationBar
import app.wazabe.mlauncher.helper.hideStatusBar
import app.wazabe.mlauncher.helper.showNavigationBar
import app.wazabe.mlauncher.helper.showStatusBar

class SystemBarObserver(private val prefs: Prefs) : DefaultLifecycleObserver {

    override fun onResume(owner: LifecycleOwner) {
        val activity = owner as? AppCompatActivity ?: return
        val window = activity.window
        if (prefs.showStatusBar) showStatusBar(window) else hideStatusBar(window)
        if (prefs.showNavigationBar) showNavigationBar(window) else hideNavigationBar(window)
    }
}
