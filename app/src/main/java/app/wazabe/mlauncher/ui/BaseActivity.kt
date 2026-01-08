package app.wazabe.mlauncher.ui

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.LayoutInflaterCompat
import app.wazabe.mlauncher.Mlauncher

open class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply Dynamic Theme Color
        // Must be done BEFORE super.onCreate and BEFORE setFactory
        val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
        val themeColor = prefs.getString("APP_THEME_COLOR", "Green") ?: "Green"
        val themeResId = getThemeForColor(themeColor)
        setTheme(themeResId)

        val typedValue = android.util.TypedValue()
        theme.resolveAttribute(android.R.attr.windowLightStatusBar, typedValue, true)
        val isLightStatusBar = typedValue.data != 0
        val iconColor = if (isLightStatusBar) "BLACK (for light background)" else "WHITE (for dark background)"
        android.util.Log.d("ThemeDebug", "Applying theme for color: $themeColor, ThemeResId: $themeResId")
        android.util.Log.d("ThemeDebug", "System thinks: Status Bar should be $iconColor [windowLightStatusBar=$isLightStatusBar]")

        // IMPORTANT: The Factory must be set BEFORE super.onCreate()
        // This intercepts the 'inflation' process as the XML is read.
        if (Mlauncher.prefs.launcherFont != "system") {
            LayoutInflaterCompat.setFactory2(layoutInflater, object : LayoutInflater.Factory2 {
                override fun onCreateView(
                    parent: View?,
                    name: String,
                    context: Context,
                    attrs: AttributeSet
                ): View? {
                    // 1. Let the AppCompatDelegate create the view (handles backward compatibility)
                    val view = delegate.createView(parent, name, context, attrs)

                    // 2. If it's any type of TextView, inject our global font
                    if (view is TextView) {
                        view.typeface = Mlauncher.globalTypeface
                    }
                    return view
                }

                override fun onCreateView(name: String, context: Context, attrs: AttributeSet): View? {
                    return onCreateView(null, name, context, attrs)
                }
            })
        }

        super.onCreate(savedInstanceState)
    }

    open fun getThemeForColor(color: String): Int {
        return when (color) {
            "Red" -> app.wazabe.mlauncher.R.style.Theme_mLauncher_Red
            "Blue" -> app.wazabe.mlauncher.R.style.Theme_mLauncher_Blue
            "Orange" -> app.wazabe.mlauncher.R.style.Theme_mLauncher_Orange
            "Purple" -> app.wazabe.mlauncher.R.style.Theme_mLauncher_Purple
            "Pink" -> app.wazabe.mlauncher.R.style.Theme_mLauncher_Pink
            "Lime" -> app.wazabe.mlauncher.R.style.Theme_mLauncher_Lime
            "Cyan" -> app.wazabe.mlauncher.R.style.Theme_mLauncher_Cyan
            "Yellow" -> app.wazabe.mlauncher.R.style.Theme_mLauncher_Yellow
            else -> app.wazabe.mlauncher.R.style.Theme_mLauncher // Green (Default)
        }
    }
}