package app.wazabe.mlauncher

import android.app.Application
import android.content.Context
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import com.github.droidworksstudio.common.CrashHandler
import app.wazabe.mlauncher.data.Constants
import app.wazabe.mlauncher.data.Prefs
import app.wazabe.mlauncher.helper.IconCacheTarget
import app.wazabe.mlauncher.helper.IconPackHelper
import app.wazabe.mlauncher.helper.utils.AppReloader
import java.util.concurrent.Executors

class Mlauncher : Application() {

    override fun onCreate() {
        super.onCreate()
        initialize(this)
    }

    companion object {
        private var appContext: Context? = null

        // 🔹 Store the Prefs instance here so it's globally accessible
        lateinit var prefs: Prefs
            private set

        var globalTypeface: Typeface = Typeface.DEFAULT
            private set

        fun getContext(): Context {
            return appContext ?: throw IllegalStateException("Mlauncher not initialized.")
        }

        fun initialize(context: Context) {
            if (appContext != null) return
            appContext = context.applicationContext

            // 🔹 Initialize the shared Prefs instance
            prefs = Prefs(appContext!!)

            // 1. Initial Font Load (Now uses the internal 'prefs')
            reloadFont()

            // 2. Set theme mode
            val themeMode = when (prefs.appTheme) {
                Constants.Theme.Light -> AppCompatDelegate.MODE_NIGHT_NO
                Constants.Theme.Dark -> AppCompatDelegate.MODE_NIGHT_YES
                Constants.Theme.System -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            AppCompatDelegate.setDefaultNightMode(themeMode)

            // 3. Background Tasks
            val executor = Executors.newSingleThreadExecutor()
            executor.execute {
                if (prefs.iconPackHome == Constants.IconPacks.Custom) {
                    IconPackHelper.preloadIcons(
                        appContext!!,
                        prefs.customIconPackHome,
                        IconCacheTarget.HOME
                    )
                }
                if (prefs.iconPackAppList == Constants.IconPacks.Custom) {
                    IconPackHelper.preloadIcons(
                        appContext!!,
                        prefs.customIconPackAppList,
                        IconCacheTarget.APP_LIST
                    )
                }
            }

            Thread.setDefaultUncaughtExceptionHandler(CrashHandler(appContext!!))
            CrashHandler.logUserAction("App Launched")
        }

        /**
         * Loads the font from Assets using the internal 'prefs' instance.
         */
        fun reloadFont() {
            val fontPath = prefs.launcherFont
            val context = getContext()

            globalTypeface = if (fontPath == "system") {
                Typeface.DEFAULT
            } else {
                try {
                    // Attempt to create typeface
                    val tf = Typeface.createFromAsset(context.assets, fontPath)

                    // DEBUG TOAST 3: Success!
                    //Toast.makeText(context, "Font Loaded: $fontPath", Toast.LENGTH_SHORT).show()
                    tf
                } catch (e: Exception) {
                    // DEBUG TOAST 4: Error - This usually means the path is wrong
                    Toast.makeText(context, "FAILED to find: $fontPath", Toast.LENGTH_LONG).show()
                    Typeface.DEFAULT
                }
            }

        }
    }
}