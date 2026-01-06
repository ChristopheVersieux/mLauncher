package app.wazabe.mlauncher.data

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager.NameNotFoundException
import android.graphics.Typeface
import androidx.compose.runtime.Composable
import androidx.core.content.res.ResourcesCompat
import com.github.droidworksstudio.common.AppLogger
import androidx.compose.ui.res.stringResource
import app.wazabe.mlauncher.Mlauncher
import app.wazabe.mlauncher.R
import app.wazabe.mlauncher.helper.IconCacheTarget
import app.wazabe.mlauncher.helper.getTrueSystemFont
import java.io.File
import java.util.Locale

interface EnumOption {
    @Composable
    fun string(): String
}


object Constants {
    const val MIN_HOME_APPS = 0

    const val MIN_HOME_PAGES = 1

    const val MIN_TEXT_SIZE = 10
    const val MAX_TEXT_SIZE = 100

    const val MIN_CLOCK_DATE_SIZE = 10
    const val MAX_CLOCK_DATE_SIZE = 120

    const val MIN_ALARM_SIZE = 10
    const val MAX_ALARM_SIZE = 120


    const val MIN_BATTERY_SIZE = 10
    const val MAX_BATTERY_SIZE = 75

    const val MIN_TEXT_PADDING = 0
    const val MAX_TEXT_PADDING = 50

    const val MIN_RECENT_COUNTER = 1
    const val MAX_RECENT_COUNTER = 35

    const val MIN_FILTER_STRENGTH = 0
    const val MAX_FILTER_STRENGTH = 100

    const val MIN_OPACITY = 0
    const val MAX_OPACITY = 100

    const val DOUBLE_CLICK_TIME_DELTA = 300L // Adjust as needed
    const val SWIPE_VELOCITY_THRESHOLD = 450f // Adjust as needed

    // Update SWIPE_DISTANCE_THRESHOLD dynamically based on screen dimensions
    const val MIN_THRESHOLD = 0f
    const val MAX_THRESHOLD = 1f
    var SHORT_SWIPE_THRESHOLD = 0f  // pixels
    var LONG_SWIPE_THRESHOLD = 0f // pixels
    var USR_DPIX = 0f
    var USR_DPIY = 0f


    // Update MAX_HOME_PAGES dynamically based on MAX_HOME_APPS
    var MAX_HOME_APPS = 20
    var MAX_HOME_PAGES = 10

    const val ACCESS_FINE_LOCATION = 666
    const val READ_CONTACTS = 777
    const val REQUEST_BIND_APPWIDGET = 888
    const val REQUEST_CONFIGURE_APPWIDGET = 999

    fun updateMaxHomePages(context: Context) {
        val prefs = Prefs(context)

        MAX_HOME_PAGES = if (prefs.homeAppsNum < MAX_HOME_PAGES) {
            prefs.homeAppsNum
        } else {
            MAX_HOME_PAGES
        }
    }

    fun updateMaxAppsBasedOnPages(context: Context) {
        val prefs = Prefs(context)

        // Define maximum apps per page
        val maxAppsPerPage = 20

        // Set MAX_HOME_APPS to the number of apps based on pages and apps per page
        MAX_HOME_APPS = maxAppsPerPage * prefs.homePagesNum
    }


    fun updateSwipeDistanceThreshold(context: Context, direction: String) {
        val prefs = Prefs(context)
        val metrics = context.resources.displayMetrics

        USR_DPIX = metrics.xdpi
        USR_DPIY = metrics.ydpi

        val screenWidthInches = metrics.widthPixels / USR_DPIX
        val screenHeightInches = metrics.heightPixels / USR_DPIY

        if (direction.equals("left", true) || direction.equals("right", true)) {
            LONG_SWIPE_THRESHOLD = screenWidthInches * prefs.longSwipeThreshold
            SHORT_SWIPE_THRESHOLD = screenWidthInches * prefs.shortSwipeThreshold
        } else {
            LONG_SWIPE_THRESHOLD = screenHeightInches * prefs.longSwipeThreshold
            SHORT_SWIPE_THRESHOLD = screenHeightInches * prefs.shortSwipeThreshold
        }

        AppLogger.d(
            "GestureThresholds",
            "Updated thresholds for $direction: SHORT = $SHORT_SWIPE_THRESHOLD inches, LONG = $LONG_SWIPE_THRESHOLD inches"
        )
    }

    enum class AppDrawerFlag {
        None,
        LaunchApp,
        HiddenApps,
        PrivateApps,
        SetHomeApp,
        SetShortSwipeUp,
        SetShortSwipeDown,
        SetShortSwipeLeft,
        SetShortSwipeRight,
        SetLongSwipeUp,
        SetLongSwipeDown,
        SetLongSwipeLeft,
        SetLongSwipeRight,
        SetClickClock,
        SetAppUsage,
        SetFloating,
        SetClickDate,
        SetDoubleTap,
    }


    enum class Gravity : EnumOption {
        Left,
        Center,
        Right,
        IconOnly;

        @Composable
        override fun string(): String {
            return when (this) {
                Left -> stringResource(R.string.left)
                Center -> stringResource(R.string.center)
                Right -> stringResource(R.string.right)
                IconOnly -> stringResource(R.string.icon_only)
            }
        }

        fun getString(context: Context): String {
            return when (this) {
                Left -> context.getString(R.string.left)
                Center -> context.getString(R.string.center)
                Right -> context.getString(R.string.right)
                IconOnly -> context.getString(R.string.icon_only)
            }
        }

        @SuppressLint("RtlHardcoded")
        fun value(): Int {
            return when (this) {
                Left -> android.view.Gravity.LEFT
                Center -> android.view.Gravity.CENTER
                Right -> android.view.Gravity.RIGHT
                IconOnly -> android.view.Gravity.CENTER  // Icons centered
            }
        }
    }

    fun getCustomIconPackName(context: Context, target: String): String {
        return when (target) {
            IconCacheTarget.APP_LIST.name -> {
                val customPackageName = Prefs(context).customIconPackAppList
                if (customPackageName.isEmpty()) {
                    context.getString(R.string.system_custom)
                }
                try {
                    val pm = context.packageManager
                    val appInfo = pm.getApplicationInfo(customPackageName, 0)
                    val customName = pm.getApplicationLabel(appInfo).toString()
                    context.getString(R.string.system_custom_plus, customName)
                } catch (_: NameNotFoundException) {
                    context.getString(R.string.system_custom)
                }
            }

            IconCacheTarget.HOME.name -> {
                val customPackageName = Prefs(context).customIconPackHome
                if (customPackageName.isEmpty()) {
                    context.getString(R.string.system_custom)
                }
                try {
                    val pm = context.packageManager
                    val appInfo = pm.getApplicationInfo(customPackageName, 0)
                    val customName = pm.getApplicationLabel(appInfo).toString()
                    context.getString(R.string.system_custom_plus, customName)
                } catch (_: NameNotFoundException) {
                    context.getString(R.string.system_custom)
                }
            }

            else -> context.getString(R.string.system_custom)
        }
    }

    enum class IconPacks : EnumOption {
        System,
        Custom,
        CloudDots,
        LauncherDots,
        NiagaraDots,
        SpinnerDots,
        Disabled;

        fun getString(context: Context, target: String): String {
            return when (this) {
                System -> context.getString(R.string.system_default)
                Custom -> getCustomIconPackName(context, target)
                CloudDots -> context.getString(R.string.app_icons_cloud_dots)
                LauncherDots -> context.getString(R.string.app_icons_launcher_dots)
                NiagaraDots -> context.getString(R.string.app_icons_niagara_dots)
                SpinnerDots -> context.getString(R.string.app_icons_spinner_dots)
                Disabled -> context.getString(R.string.disabled)
            }
        }

        @Composable
        override fun string(): String {
            return when (this) {
                System -> stringResource(R.string.system_default)
                Custom -> stringResource(R.string.system_custom)
                CloudDots -> stringResource(R.string.app_icons_cloud_dots)
                LauncherDots -> stringResource(R.string.app_icons_launcher_dots)
                NiagaraDots -> stringResource(R.string.app_icons_niagara_dots)
                SpinnerDots -> stringResource(R.string.app_icons_spinner_dots)
                Disabled -> stringResource(R.string.disabled)
            }
        }
    }

    enum class Action : EnumOption {
        OpenApp,
        TogglePrivateSpace,
        LockScreen,
        ShowNotification,
        ShowAppList,
        ShowWidgetPage,
        ShowNotesManager,
        ShowDigitalWellbeing,
        OpenQuickSettings,
        ShowRecents,
        OpenPowerDialog,
        TakeScreenShot,
        PreviousPage,
        NextPage,
        RestartApp,
        Disabled;

        fun getString(context: Context): String {
            return when (this) {
                OpenApp -> context.getString(R.string.open_app)
                LockScreen -> context.getString(R.string.lock_screen)
                TogglePrivateSpace -> context.getString(R.string.private_space, "Toggle")
                ShowNotification -> context.getString(R.string.show_notifications)
                ShowAppList -> context.getString(R.string.show_app_list)
                ShowWidgetPage -> context.getString(R.string.show_widget_page)
                ShowNotesManager -> context.getString(R.string.show_notes_manager)
                ShowDigitalWellbeing -> context.getString(R.string.show_digital_wellbeing)
                OpenQuickSettings -> context.getString(R.string.open_quick_settings)
                ShowRecents -> context.getString(R.string.show_recents)
                OpenPowerDialog -> context.getString(R.string.open_power_dialog)
                TakeScreenShot -> context.getString(R.string.take_a_screenshot)
                PreviousPage -> context.getString(R.string.previous_page)
                NextPage -> context.getString(R.string.next_page)
                RestartApp -> context.getString(R.string.restart_launcher)
                Disabled -> context.getString(R.string.disabled)
            }
        }

        @Composable
        override fun string(): String {
            return when (this) {
                OpenApp -> stringResource(R.string.open_app)
                LockScreen -> stringResource(R.string.lock_screen)
                TogglePrivateSpace -> stringResource(R.string.private_space)
                ShowNotification -> stringResource(R.string.show_notifications)
                ShowAppList -> stringResource(R.string.show_app_list)
                ShowWidgetPage -> stringResource(R.string.show_widget_page)
                ShowNotesManager -> stringResource(R.string.show_notes_manager)
                ShowDigitalWellbeing -> stringResource(R.string.show_digital_wellbeing)
                OpenQuickSettings -> stringResource(R.string.open_quick_settings)
                ShowRecents -> stringResource(R.string.show_recents)
                OpenPowerDialog -> stringResource(R.string.open_power_dialog)
                TakeScreenShot -> stringResource(R.string.take_a_screenshot)
                PreviousPage -> stringResource(R.string.previous_page)
                NextPage -> stringResource(R.string.next_page)
                RestartApp -> stringResource(R.string.restart_launcher)
                Disabled -> stringResource(R.string.disabled)
            }
        }
    }

    enum class SearchEngines : EnumOption {
        Bing,
        Brave,
        DuckDuckGo,
        Google,
        Mojeek,
        Qwant,
        Seznam,
        StartPage,
        SwissCow,
        Yahoo,
        Yandex;

        fun getString(context: Context): String {
            return when (this) {
                Bing -> context.getString(R.string.search_bing)
                Brave -> context.getString(R.string.search_brave)
                DuckDuckGo -> context.getString(R.string.search_duckduckgo)
                Google -> context.getString(R.string.search_google)
                Mojeek -> context.getString(R.string.search_mojeek)
                Qwant -> context.getString(R.string.search_qwant)
                Seznam -> context.getString(R.string.search_seznam)
                StartPage -> context.getString(R.string.search_startpage)
                SwissCow -> context.getString(R.string.search_swisscow)
                Yahoo -> context.getString(R.string.search_yahoo)
                Yandex -> context.getString(R.string.search_yandex)
            }
        }

        fun getURL(): String {
            return when (this) {
                Bing -> "https://bing.com/search?q="
                Brave -> "https://search.brave.com/search?q="
                DuckDuckGo -> "https://duckduckgo.com/?q="
                Google -> "https://google.com/search?q="
                Mojeek -> "https://www.mojeek.com/search?q="
                Qwant -> "https://www.qwant.com/?q="
                Seznam -> "https://search.seznam.cz/?q="
                StartPage -> "https://www.startpage.com/sp/search?q="
                SwissCow -> "https://swisscows.com/web?query="
                Yahoo -> "https://search.yahoo.com/search?p="
                Yandex -> "https://yandex.com/search/?text="
            }
        }

        @Composable
        override fun string(): String {
            return when (this) {
                Bing -> stringResource(R.string.search_bing)
                Brave -> stringResource(R.string.search_brave)
                DuckDuckGo -> stringResource(R.string.search_duckduckgo)
                Google -> stringResource(R.string.search_google)
                Mojeek -> stringResource(R.string.search_mojeek)
                Qwant -> stringResource(R.string.search_qwant)
                Seznam -> stringResource(R.string.search_seznam)
                StartPage -> stringResource(R.string.search_startpage)
                SwissCow -> stringResource(R.string.search_swisscow)
                Yahoo -> stringResource(R.string.search_yahoo)
                Yandex -> stringResource(R.string.search_yandex)
            }
        }
    }


    enum class Theme : EnumOption {
        System,
        Dark,
        Light;

        fun getString(context: Context): String {
            return when (this) {
                System -> context.getString(R.string.system_default)
                Dark -> context.getString(R.string.dark)
                Light -> context.getString(R.string.light)
            }
        }

        // Keep this for Composable usage
        @Composable
        override fun string(): String {
            return when (this) {
                System -> stringResource(R.string.system_default)
                Dark -> stringResource(R.string.dark)
                Light -> stringResource(R.string.light)
            }
        }
    }

    enum class TempUnits : EnumOption {
        Celsius,
        Fahrenheit;

        // Keep this for Composable usage
        @Composable
        override fun string(): String {
            return when (this) {
                Celsius -> stringResource(R.string.celsius)
                Fahrenheit -> stringResource(R.string.fahrenheit)
            }
        }
    }

    enum class DrawerType : EnumOption {
        Alphabetical,
        MostUsed;

        @Composable
        override fun string(): String {
             return when (this) {
                Alphabetical -> stringResource(R.string.alphabetical)
                MostUsed -> stringResource(R.string.most_used)
            }
        }
        
        fun getString(context: Context): String {
            return when (this) {
                Alphabetical -> context.getString(R.string.alphabetical)
                MostUsed -> context.getString(R.string.most_used)
            }
        }
    }


    enum class FontFamily : EnumOption {
        System,
        Custom,
        Bitter,
        Doto,
        FiraCode,
        Hack,
        Lato,
        Merriweather,
        Montserrat,
        Quicksand,
        Raleway,
        Roboto,
        SourceCodePro;


        fun getString(context: Context): String {
            return when (this) {
                System -> context.getString(R.string.system_default)
                Bitter -> context.getString(R.string.settings_font_bitter)
                Doto -> context.getString(R.string.settings_font_doto)
                FiraCode -> context.getString(R.string.settings_font_firacode)
                Hack -> context.getString(R.string.settings_font_hack)
                Lato -> context.getString(R.string.settings_font_lato)
                Merriweather -> context.getString(R.string.settings_font_merriweather)
                Montserrat -> context.getString(R.string.settings_font_montserrat)
                Quicksand -> context.getString(R.string.settings_font_quicksand)
                Raleway -> context.getString(R.string.settings_font_raleway)
                Roboto -> context.getString(R.string.settings_font_roboto)
                SourceCodePro -> context.getString(R.string.settings_font_sourcecodepro)
                Custom -> context.getString(R.string.system_custom)
            }
        }

        @Composable
        override fun string(): String {
            return when (this) {
                System -> stringResource(R.string.system_default)
                Bitter -> stringResource(R.string.settings_font_bitter)
                Doto -> stringResource(R.string.settings_font_doto)
                FiraCode -> stringResource(R.string.settings_font_firacode)
                Hack -> stringResource(R.string.settings_font_hack)
                Lato -> stringResource(R.string.settings_font_lato)
                Merriweather -> stringResource(R.string.settings_font_merriweather)
                Montserrat -> stringResource(R.string.settings_font_montserrat)
                Quicksand -> stringResource(R.string.settings_font_quicksand)
                Raleway -> stringResource(R.string.settings_font_raleway)
                Roboto -> stringResource(R.string.settings_font_roboto)
                SourceCodePro -> stringResource(R.string.settings_font_sourcecodepro)
                Custom -> stringResource(R.string.system_custom)
            }
        }
    }

    const val URL_GOOGLE_PLAY_STORE = "https://play.google.com/store/search?c=apps&q"
    const val APP_GOOGLE_PLAY_STORE = "market://search?c=apps&q"
}
