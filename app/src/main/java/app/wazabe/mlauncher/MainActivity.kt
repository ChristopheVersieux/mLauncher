package app.wazabe.mlauncher

import android.app.Activity
import android.app.role.RoleManager
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.provider.Settings
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.findNavController
import app.wazabe.mlauncher.data.Constants
import app.wazabe.mlauncher.data.Migration
import app.wazabe.mlauncher.data.Prefs
import app.wazabe.mlauncher.databinding.ActivityMainBinding
import app.wazabe.mlauncher.helper.IconCacheTarget
import app.wazabe.mlauncher.helper.IconPackHelper
import app.wazabe.mlauncher.helper.emptyString
import app.wazabe.mlauncher.helper.hideNavigationBar
import app.wazabe.mlauncher.helper.hideStatusBar
import app.wazabe.mlauncher.helper.ismlauncherDefault
import app.wazabe.mlauncher.helper.showNavigationBar
import app.wazabe.mlauncher.helper.showStatusBar
import app.wazabe.mlauncher.helper.utils.AppReloader
import app.wazabe.mlauncher.helper.utils.SystemBarObserver
import app.wazabe.mlauncher.ui.BaseActivity
import app.wazabe.mlauncher.ui.HomeFragment
import app.wazabe.mlauncher.ui.onboarding.OnboardingActivity
import com.github.droidworksstudio.common.AppLogger
import com.github.droidworksstudio.common.showLongToast
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import org.xmlpull.v1.XmlPullParser
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : BaseActivity() {

    private lateinit var prefs: Prefs
    private lateinit var migration: Migration
    private lateinit var navController: NavController
    private lateinit var viewModel: MainViewModel
    private lateinit var binding: ActivityMainBinding

    private lateinit var performFullBackup: ActivityResultLauncher<Intent>
    private lateinit var performFullRestore: ActivityResultLauncher<Intent>

    private lateinit var performThemeBackup: ActivityResultLauncher<Intent>
    private lateinit var performThemeRestore: ActivityResultLauncher<Intent>

    private lateinit var pickCustomFont: ActivityResultLauncher<Array<String>>

    private lateinit var setDefaultHomeScreenLauncher: ActivityResultLauncher<Intent>

    private lateinit var widgetPermissionLauncher: ActivityResultLauncher<Intent>
    private var widgetResultCallback: ((Int, Int, Intent?) -> Unit)? = null
    private var widgetConfigCallback: ((Int, Int) -> Unit)? = null
    private var widgetHost: AppWidgetHost? = null

    private var currentThemeColor: String? = null

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_MENU -> {
                when (navController.currentDestination?.id) {
                    R.id.mainFragment -> {
                        this.findNavController(R.id.nav_host_fragment)
                            .navigate(R.id.action_mainFragment_to_appListFragment)
                        true
                    }

                    else -> {
                        false
                    }
                }
            }

            KeyEvent.KEYCODE_A, KeyEvent.KEYCODE_B, KeyEvent.KEYCODE_C, KeyEvent.KEYCODE_D,
            KeyEvent.KEYCODE_E, KeyEvent.KEYCODE_F, KeyEvent.KEYCODE_G, KeyEvent.KEYCODE_H,
            KeyEvent.KEYCODE_I, KeyEvent.KEYCODE_J, KeyEvent.KEYCODE_K, KeyEvent.KEYCODE_L,
            KeyEvent.KEYCODE_M, KeyEvent.KEYCODE_N, KeyEvent.KEYCODE_O, KeyEvent.KEYCODE_P,
            KeyEvent.KEYCODE_Q, KeyEvent.KEYCODE_R, KeyEvent.KEYCODE_S, KeyEvent.KEYCODE_T,
            KeyEvent.KEYCODE_U, KeyEvent.KEYCODE_V, KeyEvent.KEYCODE_W, KeyEvent.KEYCODE_X,
            KeyEvent.KEYCODE_Y, KeyEvent.KEYCODE_Z -> {
                when (navController.currentDestination?.id) {
                    R.id.mainFragment -> {
                        val bundle = Bundle()
                        bundle.putInt("letterKeyCode", keyCode) // Pass the letter key code
                        this.findNavController(R.id.nav_host_fragment)
                            .navigate(R.id.action_mainFragment_to_appListFragment, bundle)
                        true
                    }

                    else -> {
                        false
                    }
                }
            }

            KeyEvent.KEYCODE_ESCAPE -> {
                backToHomeScreen()
                true
            }

            else -> {
                super.onKeyDown(keyCode, event)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        AppLogger.d("BottomSheetDebug", "MainActivity.onNewIntent called - Home button pressed while on home screen")
        AppLogger.d("BottomSheetDebug", "Intent action: ${intent.action}, flags: ${intent.flags}")
        
        // Notify fragments that we need to reset bottom sheet state
        // This prevents the bottom sheet from going to HIDDEN state
        supportFragmentManager.fragments.forEach { fragment ->
            if (fragment is HomeFragment) {
                fragment.resetBottomSheetOnHomePress()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // --- THEME CHECK INIT ---
        // Initialize currentThemeColor using standard PreferenceManager to be safe
        val defaultPrefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
        currentThemeColor = defaultPrefs.getString("APP_THEME_COLOR", "Green")

        // Enables edge-to-edge mode
        enableEdgeToEdge()
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (navController.currentDestination?.id != R.id.mainFragment) {
                    isEnabled = false // Temporarily disable callback
                    onBackPressedDispatcher.onBackPressed() // Perform default back action
                }
            }
        }

        onBackPressedDispatcher.addCallback(this, callback)

        prefs = Prefs(this)
        migration = Migration(this)

        requestedOrientation = if (prefs.lockOrientation) {
            if (prefs.lockOrientationPortrait) {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            } else {
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
        } else {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }

        if (prefs.iconPackHome == Constants.IconPacks.Custom) {
            val executor = Executors.newSingleThreadExecutor()
            executor.execute {
                IconPackHelper.preloadIcons(applicationContext, prefs.customIconPackHome, IconCacheTarget.HOME)
            }
        }

        if (prefs.iconPackAppList == Constants.IconPacks.Custom) {
            val executor = Executors.newSingleThreadExecutor()
            executor.execute {
                IconPackHelper.preloadIcons(applicationContext, prefs.customIconPackAppList, IconCacheTarget.APP_LIST)
            }
        }

        if (!prefs.isOnboardingCompleted()) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish() // Finish MainActivity so that user can't return to it until onboarding is completed
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        if (prefs.launcherFont != "system") {
            binding.root.post {
                binding.root.applyCustomFont()
            }
        }



        migration()

        navController = this.findNavController(R.id.nav_host_fragment)
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        // Disabled: auto-open app drawer on first launch
        // if (prefs.firstOpen) {
        //     viewModel.firstOpen(true)
        //     prefs.firstOpen = false
        // }

        viewModel.getAppList()

        window.addFlags(FLAG_LAYOUT_NO_LIMITS)

        performFullBackup = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.let { uri ->
                    applicationContext.contentResolver.openFileDescriptor(uri, "w")?.use { file ->
                        FileOutputStream(file.fileDescriptor).use { stream ->
                            val prefs = Prefs(applicationContext).saveToString()
                            stream.channel.truncate(0)
                            stream.write(prefs.toByteArray())
                        }
                    }
                }
            }
        }

        performFullRestore = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.let { uri ->
                    applicationContext.contentResolver.openInputStream(uri).use { inputStream ->
                        val stringBuilder = StringBuilder()
                        BufferedReader(InputStreamReader(inputStream)).use { reader ->
                            var line: String? = reader.readLine()
                            while (line != null) {
                                stringBuilder.append(line)
                                line = reader.readLine()
                            }
                        }

                        val string = stringBuilder.toString()
                        val prefs = Prefs(applicationContext)
                        prefs.clear()
                        prefs.loadFromString(string)
                        AppReloader.restartApp(applicationContext)
                    }
                }
            }
        }

        performThemeBackup = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                // Step 1: Read the color names from theme.xml
                val colorNames = mutableListOf<String>()

                // Obtain an XmlPullParser for the theme.xml file
                applicationContext.resources.getXml(R.xml.theme).use { parser ->
                    while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                        if (parser.eventType == XmlPullParser.START_TAG && parser.name == "color") {
                            val colorName = parser.getAttributeValue(null, "colorName")
                            colorNames.add(colorName)
                        }
                        parser.next()
                    }
                }

                result.data?.data?.let { uri ->
                    applicationContext.contentResolver.openFileDescriptor(uri, "w")?.use { file ->
                        FileOutputStream(file.fileDescriptor).use { stream ->
                            // Get the filtered preferences (only those in the colorNames list)
                            val prefs = Prefs(applicationContext).saveToTheme(colorNames)
                            stream.channel.truncate(0)
                            stream.write(prefs.toByteArray())
                        }
                    }
                }
            }
        }

        // Correct usage: Register in onAttach() to ensure it's done before the fragment's view is created
        pickCustomFont = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { handleFontSelected(it) }
        }

        performThemeRestore = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                // Step 1: Read the color names from theme.xml
                val colorNames = mutableListOf<String>()

                // Obtain an XmlPullParser for the theme.xml file
                applicationContext.resources.getXml(R.xml.theme).use { parser ->
                    while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                        if (parser.eventType == XmlPullParser.START_TAG && parser.name == "color") {
                            val colorName = parser.getAttributeValue(null, "colorName")
                            colorNames.add(colorName)
                        }
                        parser.next()
                    }
                }

                result.data?.data?.let { uri ->
                    applicationContext.contentResolver.openInputStream(uri).use { inputStream ->
                        val stringBuilder = StringBuilder()
                        BufferedReader(InputStreamReader(inputStream)).use { reader ->
                            var line: String? = reader.readLine()
                            while (line != null) {
                                stringBuilder.append(line)
                                line = reader.readLine()
                            }
                        }

                        val string = stringBuilder.toString()
                        val prefs = Prefs(applicationContext)
                        prefs.loadFromTheme(string)
                    }
                }
            }
        }

        setDefaultHomeScreenLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
                val isDefault = ismlauncherDefault(this) // Check again if the app is now default

                if (isDefault) {
                    viewModel.setDefaultLauncher(true)
                } else {
                    viewModel.setDefaultLauncher(false)
                }
            }

        widgetPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                widgetResultCallback?.invoke(
                    result.resultCode,
                    result.data?.getIntExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1,
                    result.data
                )
            }

        val systemBarObserver = SystemBarObserver(prefs)
        lifecycle.addObserver(systemBarObserver)
    }


    private fun handleFontSelected(uri: Uri?) {
        if (uri == null) return

        val fileName = getFileNameFromUri(this, uri)

        if (!fileName.endsWith(".ttf", ignoreCase = true)) {
            this.showLongToast("Only .ttf fonts are supported.")
            return
        }

        this.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)

        deleteOldFont(this)

        val savedFile = saveFontToInternalStorage(this, uri)
        if (savedFile != null && savedFile.exists()) {
            prefs.fontFamily = Constants.FontFamily.Custom
            AppReloader.restartApp(this)
        } else {
            this.showLongToast("Could not save font.")
        }
    }


    private fun getFileNameFromUri(context: Context, uri: Uri): String {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1) {
                cursor.moveToFirst()
                return cursor.getString(nameIndex)
            }
        }
        return emptyString()
    }

    private fun deleteOldFont(context: Context) {
        val file = File(context.filesDir, "CustomFont.ttf")
        if (file.exists()) {
            file.delete()
        }
    }

    private fun saveFontToInternalStorage(context: Context, fontUri: Uri): File? {
        val file = File(context.filesDir, "CustomFont.ttf")
        return try {
            context.contentResolver.openInputStream(fontUri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun createFullBackup() {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "backup_$timeStamp.json"

        val createFileIntent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_TITLE, fileName)
        }
        performFullBackup.launch(createFileIntent)
    }

    fun restoreFullBackup() {
        val openFileIntent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
        }
        performFullRestore.launch(openFileIntent)
    }

    fun createThemeBackup() {
        val timeStamp = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val fileName = "theme_$timeStamp.mtheme"

        val createFileIntent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_TITLE, fileName)
        }
        performThemeBackup.launch(createFileIntent)
    }

    fun restoreThemeBackup() {
        val openFileIntent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/octet-stream"
        }
        performThemeRestore.launch(openFileIntent)
    }

    fun pickCustomFont() {
        pickCustomFont.launch(arrayOf("*/*"))
    }

    fun setDefaultHomeScreen(context: Context, checkDefault: Boolean = false) {
        val isDefault = ismlauncherDefault(context)
        if (checkDefault && isDefault) {
            return // Launcher is already the default home app
        }

        if (context is Activity && !isDefault) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val roleManager = context.getSystemService(RoleManager::class.java)
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
                setDefaultHomeScreenLauncher.launch(intent)
                return
            } else {
                // For devices below API level 29, prompt the user to set the default launcher manually
                val intent = Intent(Settings.ACTION_HOME_SETTINGS)
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                } else {
                    // Fallback: Open general settings if HOME_SETTINGS is unavailable
                    val fallbackIntent = Intent(Settings.ACTION_SETTINGS)
                    if (fallbackIntent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(fallbackIntent)
                    } else {
                        showLongToast("Unable to open settings to set default launcher.")
                    }
                }
            }
        }

        val intent = Intent(Settings.ACTION_HOME_SETTINGS)
        setDefaultHomeScreenLauncher.launch(intent)

    }

    override fun onStop() {
        backToHomeScreen()
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        // Check if theme changed while we were away (e.g. in Settings)
        val defaultPrefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
        val newColor = defaultPrefs.getString("APP_THEME_COLOR", "Green")
        if (currentThemeColor != null && newColor != currentThemeColor) {
            recreate()
            return // Skip standard resume logic as we are restarting
        }

        backToHomeScreen()
    }

    override fun onUserLeaveHint() {
        backToHomeScreen()
        super.onUserLeaveHint()
    }

    private fun backToHomeScreen() {
        // Whenever home button is pressed or user leaves the launcher,
        // pop all the fragments except main
        if (navController.currentDestination?.id != R.id.mainFragment)
            navController.popBackStack(R.id.mainFragment, false)
    }

    private fun migration() {
        migration.migratePreferencesOnVersionUpdate(prefs)
        migration.migrateMessages(prefs)
        migration.deleteOldCacheFiles(applicationContext)
    }

    fun toggleStatusBar(show: Boolean) {
        if (show) showStatusBar(window) else hideStatusBar(window)
    }

    fun toggleNavigationBar(show: Boolean) {
        if (show) showNavigationBar(window) else hideNavigationBar(window)
    }

    fun launchWidgetPermission(intent: Intent, callback: (Int, Int, Intent?) -> Unit) {
        widgetResultCallback = callback
        widgetPermissionLauncher.launch(intent)
    }

    private var pendingWidgetId: Int = -1 // Store the original widget ID

    fun launchWidgetConfiguration(host: AppWidgetHost, widgetId: Int, callback: (Int, Int) -> Unit) {
        widgetHost = host
        widgetConfigCallback = callback
        pendingWidgetId = widgetId // Store the ID for later
        try {
            host.startAppWidgetConfigureActivityForResult(
                this,
                widgetId,
                0,
                Constants.REQUEST_CONFIGURE_APPWIDGET,
                null
            )
        } catch (e: Exception) {
            AppLogger.e("MainActivity", "❌ Failed to launch widget configuration: ${e.message}")
            widgetConfigCallback?.invoke(RESULT_CANCELED, widgetId)
            widgetConfigCallback = null
            widgetHost = null
            pendingWidgetId = -1
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Constants.REQUEST_CONFIGURE_APPWIDGET) {
            val widgetId = data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, pendingWidgetId) ?: pendingWidgetId
            widgetConfigCallback?.invoke(resultCode, widgetId)
            widgetConfigCallback = null
            widgetHost = null
            pendingWidgetId = -1
        }
    }

}

fun View.applyCustomFont() {
    // Exit if system font is selected
    if (Mlauncher.globalTypeface == android.graphics.Typeface.DEFAULT) return

    // 1. Handle Custom Components (using your specific interface)
    if (this is app.wazabe.mlauncher.helper.CustomFontView) {
        this.applyFont(Mlauncher.globalTypeface)
    }
    // 2. Handle Standard TextViews
    else if (this is android.widget.TextView) {
        this.typeface = Mlauncher.globalTypeface
    }

    // 3. Recursive crawl for containers (loops through all children)
    if (this is android.view.ViewGroup) {
        for (i in 0 until childCount) {
            getChildAt(i).applyCustomFont()
        }
    }
}
