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
}