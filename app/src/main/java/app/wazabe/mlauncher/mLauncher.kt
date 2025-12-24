package app.wazabe.mlauncher

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import app.wazabe.mlauncher.data.Constants
import app.wazabe.mlauncher.data.Prefs
import app.wazabe.mlauncher.helper.FontManager
import app.wazabe.mlauncher.helper.IconCacheTarget
import app.wazabe.mlauncher.helper.IconPackHelper
import com.github.droidworksstudio.common.AppLogger
import com.github.droidworksstudio.common.CrashHandler
import java.util.concurrent.Executors

class Mlauncher : Application() {
    override fun onCreate() {
        super.onCreate()
        initialize(this)
    }

    companion object {
        private var appContext: Context? = null

        fun getContext(): Context {
            return appContext ?: throw IllegalStateException(
                "Mlauncher not initialized. Ensure Mlauncher.initialize(context) is called early."
            )
        }

        fun initialize(context: Context) {
            if (appContext != null) return // already initialized
            appContext = context.applicationContext

            val prefs = Prefs(context)

            // 🔹 Log the theme preference to understand what's being loaded
            val loadedTheme = prefs.appTheme
            val toastMessage = "Theme loaded: ${loadedTheme.name}"
            AppLogger.d("MlauncherTheme", toastMessage)
            Toast.makeText(context.applicationContext, toastMessage, Toast.LENGTH_LONG).show()


            // 🌓 Set theme mode once at app startup
            val themeMode = when (loadedTheme) {
                Constants.Theme.Light -> AppCompatDelegate.MODE_NIGHT_NO
                Constants.Theme.Dark -> AppCompatDelegate.MODE_NIGHT_YES
                Constants.Theme.System -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            AppCompatDelegate.setDefaultNightMode(themeMode)

            // Optional: preload icons, init crash handler, etc. if needed
            val executor = Executors.newSingleThreadExecutor()
            executor.execute {
                if (prefs.iconPackHome == Constants.IconPacks.Custom) {
                    IconPackHelper.preloadIcons(appContext!!, prefs.customIconPackHome, IconCacheTarget.HOME)
                }

                if (prefs.iconPackAppList == Constants.IconPacks.Custom) {
                    IconPackHelper.preloadIcons(appContext!!, prefs.customIconPackAppList, IconCacheTarget.APP_LIST)
                }
            }

            Thread.setDefaultUncaughtExceptionHandler(CrashHandler(appContext!!))

            CrashHandler.logUserAction("App Launched")

            FontManager.reloadFont(context)
        }
    }
}
