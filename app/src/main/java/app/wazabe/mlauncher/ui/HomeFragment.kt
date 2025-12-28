package app.wazabe.mlauncher.ui

import HomeAppsAdapter
import android.annotation.SuppressLint
import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.Context.VIBRATOR_SERVICE
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Vibrator
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.format.DateFormat
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
import android.text.style.SuperscriptSpan
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.ScrollView
import android.widget.Space
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.SearchView
import androidx.biometric.BiometricPrompt
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import app.wazabe.mlauncher.MainActivity
import app.wazabe.mlauncher.MainViewModel
import app.wazabe.mlauncher.R
import app.wazabe.mlauncher.data.Constants
import app.wazabe.mlauncher.data.Constants.Action
import app.wazabe.mlauncher.data.Constants.AppDrawerFlag
import app.wazabe.mlauncher.data.Prefs
import app.wazabe.mlauncher.data.SavedWidgetEntity
import app.wazabe.mlauncher.data.database.WidgetDao
import app.wazabe.mlauncher.data.database.WidgetDatabase
import app.wazabe.mlauncher.databinding.FragmentAppDrawerBottomSheetBinding
import app.wazabe.mlauncher.databinding.FragmentHomeBinding
import app.wazabe.mlauncher.helper.IconCacheTarget
import app.wazabe.mlauncher.helper.IconPackHelper.getSafeAppIcon
import app.wazabe.mlauncher.helper.WeatherHelper
import app.wazabe.mlauncher.helper.analytics.AppUsageMonitor
import app.wazabe.mlauncher.helper.formatMillisToHMS
import app.wazabe.mlauncher.helper.getHexForOpacity
import app.wazabe.mlauncher.helper.getNextAlarm
import app.wazabe.mlauncher.helper.getSystemIcons
import app.wazabe.mlauncher.helper.hasUsageAccessPermission
import app.wazabe.mlauncher.helper.initActionService
import app.wazabe.mlauncher.helper.ismlauncherDefault
import app.wazabe.mlauncher.helper.openAppInfo
import app.wazabe.mlauncher.helper.openFirstWeatherApp
import app.wazabe.mlauncher.helper.receivers.BatteryReceiver
import app.wazabe.mlauncher.helper.receivers.DeviceAdmin
import app.wazabe.mlauncher.helper.receivers.PrivateSpaceReceiver
import app.wazabe.mlauncher.helper.setTopPadding
import app.wazabe.mlauncher.helper.showPermissionDialog
import app.wazabe.mlauncher.helper.utils.AppReloader
import app.wazabe.mlauncher.helper.utils.BiometricHelper
import app.wazabe.mlauncher.helper.utils.PrivateSpaceManager
import app.wazabe.mlauncher.helper.wordOfTheDay
import app.wazabe.mlauncher.listener.GestureAdapter
import app.wazabe.mlauncher.listener.NotificationDotManager
import app.wazabe.mlauncher.services.ActionService
import app.wazabe.mlauncher.ui.adapter.AppDrawerAdapter
import app.wazabe.mlauncher.ui.adapter.ContactDrawerAdapter
import app.wazabe.mlauncher.ui.components.DialogManager
import app.wazabe.mlauncher.ui.widgets.AppWidgetGroup
import app.wazabe.mlauncher.ui.widgets.ResizableWidgetWrapper
import app.wazabe.mlauncher.ui.widgets.WidgetActivity
import com.github.creativecodecat.components.views.FontBottomSheetDialogLocked
import com.github.droidworksstudio.common.AnalyticsHelper
import com.github.droidworksstudio.common.AppLogger
import com.github.droidworksstudio.common.ColorIconsExtensions
import com.github.droidworksstudio.common.ColorManager
import com.github.droidworksstudio.common.attachGestureManager
import com.github.droidworksstudio.common.getLocalizedString
import com.github.droidworksstudio.common.isGestureNavigationEnabled
import com.github.droidworksstudio.common.isSystemApp
import com.github.droidworksstudio.common.launchCalendar
import com.github.droidworksstudio.common.openAlarmApp
import com.github.droidworksstudio.common.openBatteryManager
import com.github.droidworksstudio.common.openCameraApp
import com.github.droidworksstudio.common.openDeviceSettings
import com.github.droidworksstudio.common.openDialerApp
import com.github.droidworksstudio.common.openDigitalWellbeing
import com.github.droidworksstudio.common.openPhotosApp
import com.github.droidworksstudio.common.openTextMessagesApp
import com.github.droidworksstudio.common.openWebBrowser
import com.github.droidworksstudio.common.showShortToast
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.absoluteValue
import kotlin.math.ceil

class HomeFragment : BaseFragment(), View.OnClickListener, View.OnLongClickListener, android.content.SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var prefs: Prefs
    private lateinit var viewModel: MainViewModel
    private lateinit var drawerBehavior: BottomSheetBehavior<View>
    private lateinit var appsAdapter: AppDrawerAdapter
    private lateinit var contactsAdapter: ContactDrawerAdapter
    private lateinit var dialogBuilder: DialogManager
    private lateinit var deviceManager: DevicePolicyManager
    private lateinit var batteryReceiver: BatteryReceiver
    private lateinit var biometricHelper: BiometricHelper
    private lateinit var weatherHelper: WeatherHelper
    private lateinit var privateSpaceReceiver: PrivateSpaceReceiver
    private lateinit var vibrator: Vibrator
    private lateinit var homeAppsAdapter: HomeAppsAdapter

    private lateinit var widgetDao: WidgetDao
    private lateinit var appWidgetManager: AppWidgetManager
    private lateinit var appWidgetHost: AppWidgetHost
    private val widgetWrappers = mutableListOf<ResizableWidgetWrapper>()
    private var isEditingWidgets: Boolean = false
    private var activeGridDialog: FontBottomSheetDialogLocked? = null
    private var lastWidgetInfo: AppWidgetProviderInfo? = null

    private var longPressToSelectApp: Int = 0
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        val view = binding.root
        prefs = Prefs(requireContext())
        batteryReceiver = BatteryReceiver()
        dialogBuilder = DialogManager(requireContext(), requireActivity())
        if (PrivateSpaceManager(requireContext()).isPrivateSpaceSupported()) {
            privateSpaceReceiver = PrivateSpaceReceiver()
        }

        longPressToSelectApp = if (prefs.homeLocked) {
            R.string.long_press_to_select_app_locked
        } else {
            R.string.long_press_to_select_app
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        biometricHelper = BiometricHelper(this.requireActivity())

        viewModel = activity?.run {
            ViewModelProvider(this)[MainViewModel::class.java]
        } ?: throw Exception("Invalid Activity")

        setupAppDrawer()

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (drawerBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                        drawerBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                    } else {
                        isEnabled = false
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                        isEnabled = true
                    }
                }
            }
        )

        viewModel.ismlauncherDefault()

        deviceManager =
            context?.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        @Suppress("DEPRECATION")
        vibrator = context?.getSystemService(VIBRATOR_SERVICE) as Vibrator

        initAppObservers()
        initClickListeners()
        initSwipeTouchListener()
        initPermissionCheck()
        initObservers()

        homeAppsAdapter = HomeAppsAdapter(
            prefs,
            onClick = { location -> homeAppClicked(location) },
            onLongClick = { location ->
                showAppList(AppDrawerFlag.SetHomeApp, includeHiddenApps = true, includeRecentApps = false, location)
            }
        )

        binding.homeAppsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = homeAppsAdapter
        }

        // Update view appearance/settings based on prefs
        updateUIFromPreferences()
        
        initWidgetHost()
    }

    override fun onStart() {
        super.onStart()
        updateUIFromPreferences()

        // Handle status bar once per view creation
        setTopPadding(binding.mainLayout)

        weatherHelper = WeatherHelper(
            requireContext(),
            viewLifecycleOwner
        ) { weatherText ->
            binding.weather.textSize = prefs.batterySize.toFloat()
            binding.weather.setTextColor(prefs.batteryColor)
            binding.weather.text = weatherText
            binding.weather.isVisible = true
        }

        // Weather updates
        if (prefs.showWeather) {
            weatherHelper.getWeather()
        } else {
            binding.weather.isVisible = false
        }

        // Update dynamic UI elements
        updateTimeAndInfo()

        // Register battery receiver
        context?.let { ctx ->
            batteryReceiver = BatteryReceiver()
            ctx.registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

            // Register private space receiver if supported
            if (PrivateSpaceManager(ctx).isPrivateSpaceSupported()) {
                privateSpaceReceiver = PrivateSpaceReceiver()
                ctx.registerReceiver(privateSpaceReceiver, IntentFilter(Intent.ACTION_PROFILE_AVAILABLE))
            }

            prefs.prefsNormal.registerOnSharedPreferenceChangeListener(this)
        }
        
        restoreWidgets()
    }

    override fun onStop() {
        super.onStop()

        context?.let { ctx ->
            try {
                batteryReceiver.let { ctx.unregisterReceiver(it) }
                if (PrivateSpaceManager(requireContext()).isPrivateSpaceSupported()) {
                    privateSpaceReceiver.let { ctx.unregisterReceiver(it) }
                }
            } catch (e: IllegalArgumentException) {
                // Receiver not registered — safe to ignore
                e.printStackTrace()
            }
            prefs.prefsNormal.unregisterOnSharedPreferenceChangeListener(this)
        }

        appWidgetHost.stopListening()
        dismissDialogs()
    }


    private fun updateUIFromPreferences() {
        val locale = prefs.appLanguage.locale()
        val is24HourFormat = DateFormat.is24HourFormat(requireContext())

        updateTimeAndInfo()

        binding.apply {
            val best12Raw = DateFormat.getBestDateTimePattern(locale, "hm") // 12-hour with AM/PM
            val best12 = if (prefs.showClockFormat) {
                best12Raw // keep AM/PM
            } else {
                best12Raw.replace("a", "").trim() // strip AM/PM
            }

            val best24 = DateFormat.getBestDateTimePattern(locale, "Hm") // 24-hour

            val timePattern = if (is24HourFormat) best24 else best12

            clock.format12Hour = timePattern
            clock.format24Hour = timePattern

            // Date format
            val datePattern = DateFormat.getBestDateTimePattern(locale, "EEEddMMM")
            date.format12Hour = datePattern
            date.format24Hour = datePattern

            // Static UI setup
            date.textSize = prefs.dateSize.toFloat()
            clock.textSize = prefs.clockSize.toFloat()
            alarm.textSize = prefs.alarmSize.toFloat()
            dailyWord.textSize = prefs.dailyWordSize.toFloat()
            battery.textSize = prefs.batterySize.toFloat()
            homeScreenPager.textSize = prefs.appSize.toFloat()

            clock.isVisible = prefs.showClock
            date.isVisible = prefs.showDate
            alarm.isVisible = prefs.showAlarm && alarm.text.toString().isNotBlank() && alarm.text.toString() != "No alarm is set."
            dailyWord.isVisible = prefs.showDailyWord && dailyWord.text.toString().isNotBlank()
            battery.isVisible = prefs.showBattery
            weather.isVisible = prefs.showWeather
            totalScreenTime.isVisible = prefs.appUsageStats
            mainLayout.setBackgroundColor(getHexForOpacity(prefs))

            date.setTextColor(prefs.dateColor)
            clock.setTextColor(prefs.clockColor)
            alarm.setTextColor(prefs.alarmClockColor)
            dailyWord.setTextColor(prefs.dailyWordColor)
            battery.setTextColor(prefs.batteryColor)
            totalScreenTime.setTextColor(prefs.appColor)
            setDefaultLauncher.setTextColor(prefs.appColor)

            val fabList = listOf(fabPhone, fabMessages, fabCamera, fabPhotos, fabBrowser)
            // fabSettings and fabAction are now in the drawer header
            fabSettings.isVisible = false
            fabAction.isVisible = false
            val fabFlags = prefs.getMenuFlags("HOME_BUTTON_FLAGS", "0000011") // Might return list of wrong size
            val colors = ColorManager.getRandomHueColors(prefs.shortcutIconsColor, fabList.size)

            for (i in fabList.indices) {
                val fab = fabList[i]

                val isVisible = if (i < fabFlags.size) fabFlags[i] else false
                val color = colors[i]

                fab.isVisible = isVisible

                // Skip recoloring for fabAction
                if (fab != fabAction) {
                    fab.setColorFilter(
                        if (prefs.iconRainbowColors) color else prefs.shortcutIconsColor
                    )
                }
            }

            // Alignments
            clock.gravity = prefs.clockAlignment.value()
            (clock.layoutParams as LinearLayout.LayoutParams).gravity = prefs.clockAlignment.value()

            date.gravity = prefs.dateAlignment.value()
            (date.layoutParams as LinearLayout.LayoutParams).gravity = prefs.dateAlignment.value()

            alarm.gravity = prefs.alarmAlignment.value()
            (alarm.layoutParams as LinearLayout.LayoutParams).gravity = prefs.alarmAlignment.value()

            dailyWord.gravity = prefs.dailyWordAlignment.value()
            (dailyWord.layoutParams as LinearLayout.LayoutParams).gravity = prefs.dailyWordAlignment.value()

        }
        if (::homeAppsAdapter.isInitialized) {
            homeAppsAdapter.notifyDataSetChanged()
        }
    }

    private fun updateTimeAndInfo() {
        val locale = prefs.appLanguage.locale()
        val is24HourFormat = DateFormat.is24HourFormat(requireContext())

        binding.apply {

            val best12Raw = DateFormat.getBestDateTimePattern(locale, "hm") // 12-hour with AM/PM
            val best12 = if (prefs.showClockFormat) {
                best12Raw // keep AM/PM
            } else {
                best12Raw.replace("a", "").trim() // strip AM/PM
            }

            val best24 = DateFormat.getBestDateTimePattern(locale, "Hm") // 24-hour

            val timePattern = if (is24HourFormat) best24 else best12

            clock.format12Hour = timePattern
            clock.format24Hour = timePattern

            // Date format
            val datePattern = DateFormat.getBestDateTimePattern(locale, "EEEddMMM")
            date.format12Hour = datePattern
            date.format24Hour = datePattern

            alarm.text = getNextAlarm(requireContext(), prefs)
            dailyWord.text = wordOfTheDay(prefs)
        }
    }


    override fun onClick(view: View) {
        when (view.id) {
            R.id.clock -> {
                when (val action = prefs.clickClockAction) {
                    Action.OpenApp -> openClickClockApp()
                    else -> handleOtherAction(action)
                }
                AnalyticsHelper.logUserAction("Clock Clicked")
            }

            R.id.date -> {
                when (val action = prefs.clickDateAction) {
                    Action.OpenApp -> openClickDateApp()
                    else -> handleOtherAction(action)
                }
                AnalyticsHelper.logUserAction("Date Clicked")
            }

            R.id.totalScreenTime -> {
                when (val action = prefs.clickAppUsageAction) {
                    Action.OpenApp -> openClickUsageApp()
                    else -> handleOtherAction(action)
                }
                AnalyticsHelper.logUserAction("TotalScreenTime Clicked")
            }

            R.id.setDefaultLauncher -> {
                viewModel.resetDefaultLauncherApp(requireContext())
                AnalyticsHelper.logUserAction("SetDefaultLauncher Clicked")
            }

            R.id.battery -> {
                context?.openBatteryManager()
                AnalyticsHelper.logUserAction("Battery Clicked")
            }

            R.id.weather -> {
                context?.openFirstWeatherApp()
                AnalyticsHelper.logUserAction("Weather Clicked")
            }

            R.id.fabPhone -> {
                context?.openDialerApp()
                AnalyticsHelper.logUserAction("fabPhone Clicked")
            }

            R.id.fabMessages -> {
                context?.openTextMessagesApp()
                AnalyticsHelper.logUserAction("fabMessages Clicked")
            }

            R.id.fabCamera -> {
                context?.openCameraApp()
                AnalyticsHelper.logUserAction("fabCamera Clicked")
            }

            R.id.fabPhotos -> {
                context?.openPhotosApp()
                AnalyticsHelper.logUserAction("fabPhotos Clicked")
            }

            R.id.fabBrowser -> {
                context?.openWebBrowser()
                AnalyticsHelper.logUserAction("fabBrowser Clicked")
            }

            R.id.fabSettings, R.id.drawerFabSettings -> {
                trySettings()
                AnalyticsHelper.logUserAction("Settings Clicked")
            }

            R.id.fabAction, R.id.drawerFabAction -> {
                when (val action = prefs.clickFloatingAction) {
                    Action.OpenApp -> openFabActionApp()
                    else -> handleOtherAction(action)
                }
                AnalyticsHelper.logUserAction("Action Clicked")
            }

            else -> {
                try { // Launch app
                    val appLocation = view.id
                    homeAppClicked(appLocation)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onLongClick(view: View): Boolean {
        if (prefs.homeLocked) return true

        val n = view.id
        showAppList(AppDrawerFlag.SetHomeApp, includeHiddenApps = true, includeRecentApps = false, n)
        AnalyticsHelper.logUserAction("Show App List")
        return true
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initSwipeTouchListener() {
        binding.touchArea.getHomeScreenGestureListener()
    }

    private fun initPermissionCheck() {
        val context = requireContext()
        if (prefs.recentAppsDisplayed || prefs.appUsageStats) {
            // Check if the usage permission is not granted
            if (!hasUsageAccessPermission(context)) {
                // Postpone showing the dialog until the activity is running
                Handler(Looper.getMainLooper()).post {
                    // Instantiate MainActivity and pass it to showPermissionDialog
                    showPermissionDialog(context)
                }
            }
        }
    }

    private fun initClickListeners() {
        binding.apply {
            clock.setOnClickListener(this@HomeFragment)
            date.setOnClickListener(this@HomeFragment)
            totalScreenTime.setOnClickListener(this@HomeFragment)
            setDefaultLauncher.setOnClickListener(this@HomeFragment)
            battery.setOnClickListener(this@HomeFragment)
            weather.setOnClickListener(this@HomeFragment)

            // fabPhone, fabMessages, etc. remain for now
            fabPhone.setOnClickListener(this@HomeFragment)
            fabMessages.setOnClickListener(this@HomeFragment)
            fabCamera.setOnClickListener(this@HomeFragment)
            fabPhotos.setOnClickListener(this@HomeFragment)
            fabBrowser.setOnClickListener(this@HomeFragment)
            // fabAction and fabSettings removed from here
        }
    }


    private fun initAppObservers() {
        binding.apply {
            firstRunTips.isVisible = prefs.firstSettingsOpen

            setDefaultLauncher.isVisible = !ismlauncherDefault(requireContext())

            val changeLauncherText = if (ismlauncherDefault(requireContext())) {
                R.string.advanced_settings_change_default_launcher
            } else {
                R.string.advanced_settings_set_as_default_launcher
            }

            setDefaultLauncher.text = getLocalizedString(changeLauncherText)
        }
    }

    private fun initObservers() {
        with(viewModel) {
            launcherDefault.observe(viewLifecycleOwner) { isDefault ->
                binding.setDefaultLauncher.isVisible = isDefault == true
            }

            homeAppsNum.observe(viewLifecycleOwner) { num ->
                homeAppsAdapter.notifyDataSetChanged()
                if (prefs.appUsageStats) {
                    updateAppCountWithUsageStats(num)
                } else {
                    updateAppCount(num)
                }
            }
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: android.content.SharedPreferences?, key: String?) {
        when (key) {
            "showDate", "showClock", "showAlarm", "showDailyWord", "showBattery",
            "dateSize", "clockSize", "alarmSize", "dailyWordSize", "batterySize", "appSize",
            "dateColor", "clockColor", "alarmColor", "dailyWordColor", "batteryColor", "appColor",
            "backgroundColor", "opacityNum", "showBackground", "textPaddingSize",
            "showWeather", "appUsageStats" -> {
                updateUIFromPreferences()
            }
            "homeAlignment", "clockAlignment", "dateAlignment", "alarmAlignment", "dailyWordAlignment", "drawerAlignment", "homeAlignmentBottom" -> {
                updateUIFromPreferences()
                // Some alignments might require layout updates
                binding.mainLayout.requestLayout()
            }
            "iconPackHome", "customIconPackHome", "iconPackAppList", "customIconPackAppList" -> {
                // Reload icons if needed - HomeFragment usually handles this via AppReloader or similar
                // For now, refresh UI
                updateUIFromPreferences()
            }
        }
    }


    private fun homeAppClicked(location: Int) {
        AnalyticsHelper.logUserAction("Clicked Home App: $location")
        if (prefs.getAppName(location).isEmpty()) showLongPressToast()
        else viewModel.launchApp(prefs.getHomeAppModel(location), this)
    }

    private fun showAppList(flag: AppDrawerFlag, includeHiddenApps: Boolean = false, includeRecentApps: Boolean = true, n: Int = 0) {
        viewModel.getAppList(includeHiddenApps, includeRecentApps)
        appsAdapter.flag = flag
        appsAdapter.location = n
        AnalyticsHelper.logUserAction("Display App List")
        drawerBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    private fun showNotesManager() {
        AnalyticsHelper.logUserAction("Display Notes Manager")
        try {
            if (findNavController().currentDestination?.id == R.id.mainFragment) {
                findNavController().navigate(R.id.action_mainFragment_to_notesManagerFragment)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("PrivateApi")
    private fun expandNotificationDrawer(context: Context) {
        try {
            Class.forName("android.app.StatusBarManager")
                .getMethod("expandNotificationsPanel")
                .invoke(context.getSystemService("statusbar"))
        } catch (exception: Exception) {
            initActionService(requireContext())?.openNotifications()
            exception.printStackTrace()
        }
        AnalyticsHelper.logUserAction("Expand Notification Drawer")
    }

    @SuppressLint("PrivateApi")
    private fun expandQuickSettings(context: Context) {
        try {
            Class.forName("android.app.StatusBarManager")
                .getMethod("expandSettingsPanel")
                .invoke(context.getSystemService("statusbar"))
        } catch (exception: Exception) {
            initActionService(requireContext())?.openQuickSettings()
            exception.printStackTrace()
        }
        AnalyticsHelper.logUserAction("Expand Quick Settings")
    }

    private fun openSwipeUpApp() {
        AnalyticsHelper.logUserAction("Open Swipe Up App")
        if (prefs.appShortSwipeUp.activityPackage.isNotEmpty())
            viewModel.launchApp(prefs.appShortSwipeUp, this)
        else
            requireContext().openDeviceSettings()
    }

    private fun openSwipeDownApp() {
        AnalyticsHelper.logUserAction("Open Swipe Down App")
        if (prefs.appShortSwipeDown.activityPackage.isNotEmpty())
            viewModel.launchApp(prefs.appShortSwipeDown, this)
        else
            requireContext().openDialerApp()
    }

    private fun openSwipeLeftApp() {
        AnalyticsHelper.logUserAction("Open Swipe Left App")
        if (prefs.appShortSwipeLeft.activityPackage.isNotEmpty())
            viewModel.launchApp(prefs.appShortSwipeLeft, this)
        else
            requireContext().openDeviceSettings()
    }

    private fun openSwipeRightApp() {
        AnalyticsHelper.logUserAction("Open Swipe Right App")
        if (prefs.appShortSwipeRight.activityPackage.isNotEmpty())
            viewModel.launchApp(prefs.appShortSwipeRight, this)
        else
            requireContext().openDialerApp()
    }

    private fun openLongSwipeUpApp() {
        AnalyticsHelper.logUserAction("Open Swipe Long Up App")
        if (prefs.appLongSwipeUp.activityPackage.isNotEmpty())
            viewModel.launchApp(prefs.appLongSwipeUp, this)
        else
            requireContext().openDeviceSettings()
    }

    private fun openLongSwipeDownApp() {
        AnalyticsHelper.logUserAction("Open Swipe Long Down App")
        if (prefs.appLongSwipeDown.activityPackage.isNotEmpty())
            viewModel.launchApp(prefs.appLongSwipeDown, this)
        else
            requireContext().openDialerApp()
    }

    private fun openLongSwipeLeftApp() {
        AnalyticsHelper.logUserAction("Open Swipe Long Left App")
        if (prefs.appLongSwipeLeft.activityPackage.isNotEmpty())
            viewModel.launchApp(prefs.appLongSwipeLeft, this)
        else
            requireContext().openDeviceSettings()
    }

    private fun openLongSwipeRightApp() {
        AnalyticsHelper.logUserAction("Open Swipe Long Right App")
        if (prefs.appLongSwipeRight.activityPackage.isNotEmpty())
            viewModel.launchApp(prefs.appLongSwipeRight, this)
        else
            requireContext().openDialerApp()
    }

    private fun openClickClockApp() {
        AnalyticsHelper.logUserAction("Open Clock App")
        if (prefs.appClickClock.activityPackage.isNotEmpty())
            viewModel.launchApp(prefs.appClickClock, this)
        else
            requireContext().openAlarmApp()
    }

    private fun openClickUsageApp() {
        AnalyticsHelper.logUserAction("Open Usage App")
        if (prefs.appClickUsage.activityPackage.isNotEmpty())
            viewModel.launchApp(prefs.appClickUsage, this)
        else
            requireContext().openDigitalWellbeing()
    }

    private fun openClickDateApp() {
        AnalyticsHelper.logUserAction("Open Date App")
        if (prefs.appClickDate.activityPackage.isNotEmpty())
            viewModel.launchApp(prefs.appClickDate, this)
        else
            requireContext().launchCalendar()
    }

    private fun openDoubleTapApp() {
        AnalyticsHelper.logUserAction("Open Double Tap App")
        if (prefs.appDoubleTap.activityPackage.isNotEmpty())
            viewModel.launchApp(prefs.appDoubleTap, this)
        else
            AppReloader.restartApp(requireContext())
    }

    private fun openFabActionApp() {
        AnalyticsHelper.logUserAction("Open Fab App")
        if (prefs.appFloating.activityPackage.isNotEmpty())
            viewModel.launchApp(prefs.appFloating, this)
        else
            findNavController().navigate(R.id.action_mainFragment_to_notesManagerFragment)
    }

    // This function handles all swipe actions that a independent of the actual swipe direction
    @SuppressLint("NewApi")
    private fun handleOtherAction(action: Action) {
        when (action) {
            Action.ShowNotification -> expandNotificationDrawer(requireContext())
            Action.LockScreen -> lockPhone()
            Action.TogglePrivateSpace -> PrivateSpaceManager(requireContext()).togglePrivateSpaceLock(showToast = false, launchSettings = false)
            Action.ShowAppList -> showAppList(AppDrawerFlag.LaunchApp, includeHiddenApps = false)
            Action.ShowNotesManager -> showNotesManager()
            Action.ShowDigitalWellbeing -> requireContext().openDigitalWellbeing()
            Action.OpenQuickSettings -> expandQuickSettings(requireContext())
            Action.ShowRecents -> initActionService(requireContext())?.showRecents()
            Action.OpenPowerDialog -> initActionService(requireContext())?.openPowerDialog()
            Action.TakeScreenShot -> initActionService(requireContext())?.takeScreenShot()
            Action.PreviousPage -> navigateToPreviousPage()
            Action.NextPage -> navigateToNextPage()
            Action.RestartApp -> AppReloader.restartApp(requireContext())
            Action.ShowWidgetPage -> showWidgetPage()
            Action.OpenApp -> {
                // this should be handled in the respective onSwipe[Up,Down,Right,Left] functions
            }

            Action.Disabled -> {
                // Do nothing
            }

        }
    }

    private fun showWidgetPage() {
        val context = requireContext()
        val intent = Intent(context, WidgetActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        context.startActivity(intent)
    }

    private fun lockPhone() {
        val context = requireContext()
        val deviceAdmin = ComponentName(context, DeviceAdmin::class.java)
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val actionService = ActionService.instance()

        when {
            // Use Device Admin if active
            dpm.isAdminActive(deviceAdmin) -> {
                dpm.lockNow()
                AnalyticsHelper.logUserAction("Lock Screen via Device Admin")
            }
            // Fallback to ActionService if available
            actionService != null -> {
                actionService.lockScreen()
                AnalyticsHelper.logUserAction("Lock Screen via ActionService")
            }
            // Otherwise prompt the user to enable Device Admin
            else -> {
                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                    putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, deviceAdmin)
                }
                startActivity(intent)
            }
        }
    }


    private fun showLongPressToast() = showShortToast(getLocalizedString(longPressToSelectApp))

    private fun textOnClick(view: View) = onClick(view)

    private fun textOnLongClick(view: View) = onLongClick(view)


    @SuppressLint("InflateParams", "ClickableViewAccessibility")
    private fun updateAppCountWithUsageStats(newAppsNum: Int) {
        val appUsageMonitor = AppUsageMonitor.getInstance(requireContext())
        val oldAppsNum = binding.homeAppsRecyclerView.childCount // current number of apps
        val diff = newAppsNum - oldAppsNum

        if (diff > 0) {
            // Add new apps
            for (i in oldAppsNum until newAppsNum) {
                // Create a horizontal LinearLayout to hold both existingAppView and newAppView
                val parentLinearLayout = LinearLayout(context)
                parentLinearLayout.apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, // Use MATCH_PARENT for full width
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                }

                // Create existingAppView
                val existingAppView =
                    layoutInflater.inflate(R.layout.home_app_button, null) as TextView
                existingAppView.apply {
                    // Set properties of existingAppView
                    textSize = prefs.appSize.toFloat()
                    id = i
                    text = prefs.getHomeAppModel(i).activityLabel
                    getHomeAppsGestureListener()
                    setOnClickListener(this@HomeFragment)
                    if (!prefs.extendHomeAppsArea) {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                    }
                    val padding: Int = prefs.textPaddingSize
                    setPadding(0, padding, 0, padding)
                    setTextColor(prefs.appColor)
                }

                // Create newAppView
                val newAppView = TextView(context)
                newAppView.apply {
                    // Set properties of newAppView
                    textSize = prefs.appSize.toFloat() / 1.5f
                    id = i
                    text = formatMillisToHMS(
                        appUsageMonitor.getUsageStats(
                            context,
                            prefs.getHomeAppModel(i).activityPackage
                        ), false
                    )
                    getHomeAppsGestureListener()
                    setOnClickListener(this@HomeFragment)
                    if (!prefs.extendHomeAppsArea) {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                    }
                    val padding: Int = prefs.textPaddingSize
                    setPadding(0, padding, 0, padding)
                    setTextColor(prefs.appColor)
                }

                // Add a space between existingAppView and newAppView
                val space = Space(context)
                space.layoutParams = LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f // Weight to fill available space
                )

                // Add existingAppView to parentLinearLayout
                parentLinearLayout.addView(existingAppView)
                // Add space and newAppView to parentLinearLayout
                parentLinearLayout.addView(space)
                parentLinearLayout.addView(newAppView)

                // Add parentLinearLayout to homeAppsLayout
                //binding.homeAppsLayout.addView(parentLinearLayout)
            }
        } else if (diff < 0) {
            // Remove extra apps
            //binding.homeAppsLayout.removeViews(oldAppsNum + diff, -diff)
        }

        // Create a new TextView instance
        val totalText = getLocalizedString(R.string.total_screen_time)
        val totalTime = appUsageMonitor.getTotalScreenTime(requireContext())
        val totalScreenTime = formatMillisToHMS(totalTime, true)
        AppLogger.d("totalScreenTime", totalScreenTime)
        val totalScreenTimeJoin = "$totalText: $totalScreenTime"
        // Set properties for the TextView (optional)
        binding.totalScreenTime.apply {
            text = totalScreenTimeJoin
            if (totalTime > 300L) { // Checking if totalTime is greater than 5 minutes (300,000 milliseconds)
                isVisible = true
            }
        }

        // Update the total number of pages and calculate maximum apps per page
        updatePagesAndAppsPerPage(prefs.homeAppsNum, prefs.homePagesNum)
        adjustTextViewMargins()
    }

    private fun adjustTextViewMargins() {
        binding.apply {

            privateLayout.apply {
                // Set visibility
                isVisible = PrivateSpaceManager(requireContext()).isPrivateSpaceSetUp()

                // Initial icon
                fun updatePrivateFabIcon() {
                    val isLocked = PrivateSpaceManager(requireContext()).isPrivateSpaceLocked()
                    val iconRes = if (isLocked) R.drawable.private_profile_on
                    else R.drawable.private_profile_off
                    privateFab.setImageResource(iconRes)
                }

                updatePrivateFabIcon() // set initial icon

                privateFab.setOnClickListener {
                    // Toggle lock
                    PrivateSpaceManager(requireContext()).togglePrivateSpaceLock(
                        showToast = false,
                        launchSettings = false
                    )
                    // Update icon after toggle
                    updatePrivateFabIcon()
                }
            }

            val views = listOf(
                setDefaultLauncher,
                totalScreenTime,
                homeScreenPager,
                fabLayout,
                //homeAppsLayout,
                privateLayout
            )

            // Check if device is using gesture navigation or 3-button navigation
            val isGestureNav = isGestureNavigationEnabled(requireContext())

            val numOfElements = 6
            val incrementBy = 35
            // Set margins based on navigation mode
            val margins = if (isGestureNav) {
                val startAt = resources.getDimensionPixelSize(R.dimen.bottom_margin_gesture_nav)
                List(numOfElements) { index -> startAt + (index * incrementBy) } // Adjusted margins for gesture navigation
            } else {
                val startAt = resources.getDimensionPixelSize(R.dimen.bottom_margin_3_button_nav)
                List(numOfElements) { index -> startAt + (index * incrementBy) } // Adjusted margins for 3-button navigation
            }

            val visibleViews = views.filter { it.isVisible }
            val visibleMargins =
                margins.take(visibleViews.size) // Trim margins list to match visible views

            // Reset margins for all views
            views.forEach { view ->
                val params = view.layoutParams as ViewGroup.MarginLayoutParams
                params.bottomMargin = 0
                view.layoutParams = params
            }

            // Apply correct spacing for visible views
            visibleViews.forEachIndexed { index, view ->
                val params = view.layoutParams as ViewGroup.MarginLayoutParams
                var bottomMargin = visibleMargins.getOrElse(index) { 0 }

                // Add extra space above fabLayout if it's visible
                if (prefs.homeAlignmentBottom) {
                    if (visibleViews.contains(fabLayout)) {
                        if (view == homeAppsRecyclerView) {
                            bottomMargin += 65
                        }
                    }
                }

                if (view == homeScreenPager) {
                    bottomMargin += 10
                }

                params.bottomMargin = bottomMargin
                view.layoutParams = params
            }
        }
    }


    @SuppressLint("InflateParams", "DiscouragedApi", "UseCompatLoadingForDrawables", "ClickableViewAccessibility")
    private fun updateAppCount(newAppsNum: Int) {
        val oldAppsNum = binding.homeAppsRecyclerView.childCount // Try to get current count if still used functionally
        // Note: For RecyclerView with adapter, this logic might be redundant, but keeping it for consistency if expected
        val diff = newAppsNum - oldAppsNum

        if (diff > 0) {
            // Add new apps
            for (i in oldAppsNum until newAppsNum) {
                val view = layoutInflater.inflate(R.layout.home_app_button, null) as TextView
                view.apply {
                    textSize = prefs.appSize.toFloat()
                    id = i
                    text = prefs.getHomeAppModel(i).activityLabel
                    getHomeAppsGestureListener()
                    setOnClickListener(this@HomeFragment)

                    if (!prefs.extendHomeAppsArea) {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                    }

                    gravity = prefs.homeAlignment.value()
                    isFocusable = true
                    isFocusableInTouchMode = true

                    val padding: Int = prefs.textPaddingSize
                    setPadding(0, padding, 0, padding)
                    setTextColor(prefs.appColor)
                    val appModel = prefs.getHomeAppModel(i)
                    val packageName = appModel.activityPackage

                    if (packageName.isNotBlank() && prefs.iconPackHome != Constants.IconPacks.Disabled) {
                        val iconPackPackage = prefs.customIconPackHome
                        // Try to get app icon, possibly using icon pack, with graceful fallback
                        AppLogger.e(message = "ICON "+iconPackPackage)
                        val nonNullDrawable: Drawable = getSafeAppIcon(
                            context = context,
                            packageName = packageName,
                            useIconPack = (iconPackPackage.isNotEmpty() && prefs.iconPackHome == Constants.IconPacks.Custom),
                            iconPackTarget = IconCacheTarget.HOME
                        )

                        // Use the drawable
                        val recoloredDrawable: Drawable? = getSystemIcons(
                            context,
                            prefs,
                            IconCacheTarget.HOME,
                            nonNullDrawable
                        )

                        val drawableToUse = recoloredDrawable ?: nonNullDrawable

                        // Set the icon size to match text size and add padding
                        var iconSize = (prefs.appSize * 1.4f).toInt()
                        if (prefs.iconPackHome == Constants.IconPacks.System || prefs.iconPackHome == Constants.IconPacks.Custom) {
                            iconSize *= 2
                        }
                        val iconPadding = (iconSize / 1.2f).toInt() // padding next to icon

                        drawableToUse.setBounds(0, 0, iconSize, iconSize)

                        // Set drawable position based on alignment
                        when (prefs.homeAlignment) {
                            Constants.Gravity.Left -> {
                                setCompoundDrawables(
                                    drawableToUse,
                                    null,
                                    null,
                                    null
                                )
                                // Add padding between text and icon if an icon is set
                                compoundDrawablePadding = iconPadding
                            }

                            Constants.Gravity.Right -> {
                                setCompoundDrawables(
                                    null,
                                    null,
                                    drawableToUse,
                                    null
                                )
                                // Add padding between text and icon if an icon is set
                                compoundDrawablePadding = iconPadding
                            }

                            else -> setCompoundDrawables(null, null, null, null)
                        }

                        val nm = NotificationManagerCompat.getEnabledListenerPackages(context)

                        if (nm.contains(context.packageName)) {
                            fun getCircledDigit(number: Int): String {
                                return when {
                                    number in 1..9 -> ('\u278A' + (number - 1)).toString() // ➊…➒
                                    number >= 10 -> '\u2789'.toString() // always ➓ for 10 or more
                                    else -> "" // no badge if 0 or invalid
                                }
                            }

                            val listener: (Map<String, Int>) -> Unit = { counts ->
                                val count = counts[packageName] ?: 0

                                this.text = if (count > 0) {
                                    val circledNumber = getCircledDigit(count)
                                    val newText = "${appModel.label} $circledNumber"
                                    val spannable = SpannableString(newText)

                                    val start = newText.indexOf(circledNumber)
                                    val end = start + circledNumber.length

                                    // Change size of the circled digit
                                    spannable.setSpan(
                                        AbsoluteSizeSpan((this.textSize * 0.8f).toInt(), false),
                                        start, end,
                                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                                    )

                                    // Set color
                                    val customColor = ColorIconsExtensions.getDominantColor(nonNullDrawable)
                                    spannable.setSpan(
                                        ForegroundColorSpan(customColor),
                                        start, end,
                                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                                    )

                                    // Move emoji up slightly
                                    spannable.setSpan(
                                        SuperscriptSpan(),
                                        start, end,
                                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                                    )

                                    spannable
                                } else {
                                    appModel.label
                                }

                                AppLogger.d("HomeFragment", "Notification count updated for $packageName: $count")
                            }

                            // Register listener for this TextView
                            NotificationDotManager.registerListener(listener)

                            // Make sure we update immediately based on current counts
                            listener(NotificationDotManager.getAllCounts())

                            // Unregister when detached
                            this.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                                override fun onViewAttachedToWindow(v: View) {}
                                override fun onViewDetachedFromWindow(v: View) {
                                    NotificationDotManager.unregisterListener(listener)
                                }
                            })

                        } else {
                            AppLogger.d("HomeFragment", "Notification listener permission not enabled for ${context.packageName}")
                        }
                    }
                }
                // Add the view to the layout
               // binding.homeAppsLayout.addView(view)
            }
        } else if (diff < 0) {
            // Remove extra apps
            //binding.homeAppsLayout.removeViews(oldAppsNum + diff, -diff)
        }

        // Update the total number of pages and calculate maximum apps per page
        updatePagesAndAppsPerPage(prefs.homeAppsNum, prefs.homePagesNum)
        adjustTextViewMargins()
    }


    private val homeScreenPager = "HomeScreenPager"

    private var currentPage = 0
    private lateinit var pageRanges: List<IntRange>

    private fun updatePagesAndAppsPerPage(totalApps: Int, totalPages: Int) {
        AppLogger.d(homeScreenPager, "updatePagesAndAppsPerPage: totalApps=$totalApps, totalPages=$totalPages")

        if (totalPages <= 0) {
            pageRanges = emptyList()
            AppLogger.d(homeScreenPager, "No pages to show. pageRanges cleared.")
            return
        }

        val baseAppsPerPage = totalApps / totalPages
        val extraApps = totalApps % totalPages
        AppLogger.d(homeScreenPager, "Base apps per page: $baseAppsPerPage, extra apps: $extraApps")

        var startIdx = 0
        pageRanges = List(totalPages) { page ->
            val appsThisPage = baseAppsPerPage + if (page < extraApps) 1 else 0
            val endIdx = startIdx + appsThisPage
            val range = startIdx until endIdx
            AppLogger.d(homeScreenPager, "Page $page → range $range (appsThisPage=$appsThisPage)")
            startIdx = endIdx
            range
        }

        if (currentPage >= pageRanges.size) {
            currentPage = 0
            AppLogger.d(homeScreenPager, "Current page reset to 0 as it was out of bounds")
        }

        updateAppsVisibility()
    }

    private fun updateAppsVisibility() {
        if (pageRanges.isEmpty() || currentPage !in pageRanges.indices) {
            AppLogger.d(homeScreenPager, "updateAppsVisibility: Invalid currentPage=$currentPage or empty pageRanges")
            return
        }

        val visibleRange = pageRanges[currentPage]
        AppLogger.d(homeScreenPager, "Showing apps for currentPage=$currentPage, visibleRange=$visibleRange")

        for (i in 0 until getTotalAppsCount()) {
            //val view = binding.homeAppsLayout.getChildAt(i)
            //view.isVisible = i in visibleRange
        }

        // Update page selector icons
        val totalPages = pageRanges.size
        val pageSelectorIcons = MutableList(totalPages) { R.drawable.ic_new_page }
        pageSelectorIcons[currentPage] = R.drawable.ic_current_page

        val spannable = SpannableStringBuilder()
        pageSelectorIcons.forEach { drawableRes ->
            val drawable = ContextCompat.getDrawable(requireContext(), drawableRes)?.apply {
                setBounds(0, 0, intrinsicWidth, intrinsicHeight)
                colorFilter = PorterDuffColorFilter(prefs.appColor, PorterDuff.Mode.SRC_IN)
            }
            val imageSpan = drawable?.let { ImageSpan(it, ImageSpan.ALIGN_BASELINE) }

            val placeholder = SpannableString(" ") // Placeholder
            imageSpan?.let { placeholder.setSpan(it, 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE) }

            spannable.append(placeholder)
            spannable.append(" ") // Space
        }

        binding.homeScreenPager.text = spannable
        if (prefs.homePagesNum > 1 && prefs.homePager) binding.homeScreenPager.isVisible = true
        if (prefs.showFloating) binding.fabLayout.isVisible = true
    }

    private fun navigateToPreviousPage() {
        val totalPages = pageRanges.size
        if (totalPages <= 0) {
            AppLogger.d(homeScreenPager, "handleSwipeLeft: No pages to swipe")
            return
        }

        currentPage = if (currentPage == 0) {
            totalPages - 1
        } else {
            currentPage - 1
        }

        AppLogger.d(homeScreenPager, "handleSwipeLeft: currentPage now $currentPage")
        updateAppsVisibility()
    }

    private fun navigateToNextPage() {
        val totalPages = pageRanges.size
        if (totalPages <= 0) {
            AppLogger.d(homeScreenPager, "handleSwipeRight: No pages to swipe")
            return
        }

        currentPage = if (currentPage == totalPages - 1) {
            0
        } else {
            currentPage + 1
        }

        AppLogger.d(homeScreenPager, "handleSwipeRight: currentPage now $currentPage")
        updateAppsVisibility()
    }

    private fun getTotalAppsCount(): Int {
        val count = binding.homeAppsRecyclerView.childCount
        AppLogger.d(homeScreenPager, "getTotalAppsCount: $count")
        return count
    }


    private fun trySettings() {
        lifecycleScope.launch(Dispatchers.Main) {
            if (prefs.settingsLocked) {
                biometricHelper.startBiometricSettingsAuth(object :
                    BiometricHelper.CallbackSettings {
                    override fun onAuthenticationSucceeded() {
                        sendToSettingFragment()
                    }

                    override fun onAuthenticationFailed() {
                        AppLogger.e(
                            "Authentication",
                            getLocalizedString(R.string.text_authentication_failed)
                        )
                    }

                    override fun onAuthenticationError(
                        errorCode: Int,
                        errorMessage: CharSequence?
                    ) {
                        when (errorCode) {
                            BiometricPrompt.ERROR_USER_CANCELED -> AppLogger.e(
                                "Authentication",
                                getLocalizedString(R.string.text_authentication_cancel)
                            )

                            else ->
                                AppLogger.e(
                                    "Authentication",
                                    getLocalizedString(R.string.text_authentication_error).format(
                                        errorMessage,
                                        errorCode
                                    )
                                )
                        }
                    }
                })
            } else {
                sendToSettingFragment()
            }
        }
    }

    private fun sendToSettingFragment() {
        try {
            val intent = Intent(requireContext(), SettingsActivity::class.java)
            startActivity(intent)
            viewModel.firstOpen(false)
        } catch (e: java.lang.Exception) {
            AppLogger.d("onLongClick", e.toString())
        }
    }

    private fun View.getHomeScreenGestureListener() {
        this.attachGestureManager(requireContext(), object : GestureAdapter() {
            override fun onShortSwipeLeft() {
                when (val action = prefs.shortSwipeLeftAction) {
                    Action.OpenApp -> openSwipeLeftApp()
                    else -> handleOtherAction(action)
                }
                AnalyticsHelper.logUserAction("SwipeLeft Short Gesture")
            }

            override fun onLongSwipeLeft() {
                when (val action = prefs.longSwipeLeftAction) {
                    Action.OpenApp -> openLongSwipeLeftApp()
                    else -> handleOtherAction(action)
                }
                AnalyticsHelper.logUserAction("SwipeLeft Long Gesture")
            }

            override fun onShortSwipeRight() {
                when (val action = prefs.shortSwipeRightAction) {
                    Action.OpenApp -> openSwipeRightApp()
                    else -> handleOtherAction(action)
                }
                AnalyticsHelper.logUserAction("SwipeRight Short Gesture")
            }

            override fun onLongSwipeRight() {
                when (val action = prefs.longSwipeRightAction) {
                    Action.OpenApp -> openLongSwipeRightApp()
                    else -> handleOtherAction(action)
                }
                AnalyticsHelper.logUserAction("SwipeRight Long Gesture")
            }

            override fun onShortSwipeUp() {
                when (val action = prefs.shortSwipeUpAction) {
                    Action.OpenApp -> openSwipeUpApp()
                    else -> handleOtherAction(action)
                }
                AnalyticsHelper.logUserAction("SwipeUp Short Gesture")
            }

            override fun onLongSwipeUp() {
                when (val action = prefs.longSwipeUpAction) {
                    Action.OpenApp -> openLongSwipeUpApp()
                    else -> handleOtherAction(action)
                }
                AnalyticsHelper.logUserAction("SwipeUp Long Gesture")
            }

            override fun onShortSwipeDown() {
                when (val action = prefs.shortSwipeDownAction) {
                    Action.OpenApp -> openSwipeDownApp()
                    else -> handleOtherAction(action)
                }
                AnalyticsHelper.logUserAction("SwipeDown Short Gesture")
            }

            override fun onLongSwipeDown() {
                when (val action = prefs.longSwipeDownAction) {
                    Action.OpenApp -> openLongSwipeDownApp()
                    else -> handleOtherAction(action)
                }
                AnalyticsHelper.logUserAction("SwipeDown Long Gesture")
            }

            override fun onLongPress() {
                AnalyticsHelper.logUserAction("LongPress Gesture")
                trySettings()
            }

            override fun onDoubleTap() {
                when (val action = prefs.doubleTapAction) {
                    Action.OpenApp -> openDoubleTapApp()
                    else -> handleOtherAction(action)
                }
                AnalyticsHelper.logUserAction("DoubleTap Gesture")
            }
        })
    }

    private fun View.getHomeAppsGestureListener() {
        this.attachGestureManager(requireContext(), object : GestureAdapter() {

            override fun onLongPress() {
                textOnLongClick(this@getHomeAppsGestureListener)
            }

            override fun onSingleTap() {
                textOnClick(this@getHomeAppsGestureListener)
            }

            override fun onShortSwipeLeft() {
                when (val action = prefs.shortSwipeLeftAction) {
                    Action.OpenApp -> openSwipeLeftApp()
                    else -> handleOtherAction(action)
                }
                AnalyticsHelper.logUserAction("SwipeLeft Short Gesture")
            }

            override fun onLongSwipeLeft() {
                when (val action = prefs.longSwipeLeftAction) {
                    Action.OpenApp -> openLongSwipeLeftApp()
                    else -> handleOtherAction(action)
                }
                AnalyticsHelper.logUserAction("SwipeLeft Long Gesture")
            }

            override fun onShortSwipeRight() {
                when (val action = prefs.shortSwipeRightAction) {
                    Action.OpenApp -> openSwipeRightApp()
                    else -> handleOtherAction(action)
                }
                AnalyticsHelper.logUserAction("SwipeRight Short Gesture")
            }

            override fun onLongSwipeRight() {
                when (val action = prefs.longSwipeRightAction) {
                    Action.OpenApp -> openLongSwipeRightApp()
                    else -> handleOtherAction(action)
                }
                AnalyticsHelper.logUserAction("SwipeRight Long Gesture")
            }

            override fun onShortSwipeUp() {
                when (val action = prefs.shortSwipeUpAction) {
                    Action.OpenApp -> openSwipeUpApp()
                    else -> handleOtherAction(action)
                }
                AnalyticsHelper.logUserAction("SwipeUp Short Gesture")
            }

            override fun onLongSwipeUp() {
                when (val action = prefs.longSwipeUpAction) {
                    Action.OpenApp -> openLongSwipeUpApp()
                    else -> handleOtherAction(action)
                }
                AnalyticsHelper.logUserAction("SwipeUp Long Gesture")
            }

            override fun onShortSwipeDown() {
                when (val action = prefs.shortSwipeDownAction) {
                    Action.OpenApp -> openSwipeDownApp()
                    else -> handleOtherAction(action)
                }
                AnalyticsHelper.logUserAction("SwipeDown Short Gesture")
            }

            override fun onLongSwipeDown() {
                when (val action = prefs.longSwipeDownAction) {
                    Action.OpenApp -> openLongSwipeDownApp()
                    else -> handleOtherAction(action)
                }
                AnalyticsHelper.logUserAction("SwipeDown Long Gesture")
            }
        })
    }

    private fun setupAppDrawer() {
        val drawerContainer = binding.coordinatorLayout.findViewById<View>(R.id.appDrawerContainer)
        drawerBehavior = BottomSheetBehavior.from(drawerContainer)

        val gravity = when (prefs.drawerAlignment) {
            Constants.Gravity.Left -> Gravity.LEFT
            Constants.Gravity.Center -> Gravity.CENTER
            Constants.Gravity.Right -> Gravity.RIGHT
        }

        appsAdapter = AppDrawerAdapter(
            requireContext(),
            this,
            AppDrawerFlag.LaunchApp,
            gravity,
            { appModel ->
                viewModel.selectedApp(this, appModel, appsAdapter.flag, appsAdapter.location)
                if (appsAdapter.flag == AppDrawerFlag.SetHomeApp) {
                    homeAppsAdapter.notifyDataSetChanged()
                }
                drawerBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            },
            { appModel ->
                if (requireContext().isSystemApp(appModel.activityPackage))
                    showShortToast(getLocalizedString(R.string.can_not_delete_system_apps))
                else {
                    val intent = Intent(Intent.ACTION_DELETE, "package:${appModel.activityPackage}".toUri())
                    requireContext().startActivity(intent)
                }
            },
            { p, a -> prefs.setAppAlias(p, a) },
            { p, t, u -> prefs.setAppTag(p, t, u); appsAdapter.notifyDataSetChanged() },
            { flag, appModel ->
                val newSet = mutableSetOf<String>()
                newSet.addAll(prefs.hiddenApps)
                if (flag == AppDrawerFlag.HiddenApps) {
                    newSet.remove(appModel.activityPackage + "|" + appModel.activityClass + "|" + appModel.user.hashCode())
                } else {
                    newSet.add(appModel.activityPackage + "|" + appModel.activityClass + "|" + appModel.user.hashCode())
                }
                prefs.hiddenApps = newSet
                viewModel.getAppList()
            },
            { appModel ->
                openAppInfo(requireContext(), appModel.user, appModel.activityPackage)
                drawerBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        )

        contactsAdapter = ContactDrawerAdapter(
            requireContext(),
            gravity,
            { contactModel ->
                viewModel.selectedContact(this, contactModel, 0)
                drawerBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        )

        val drawerBinding = binding.appDrawerLayout

        var statusBarSize = 0
        ViewCompat.setOnApplyWindowInsetsListener(drawerBinding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            statusBarSize = systemBars.top // On stocke la valeur

            // On garde uniquement la logique du bas ici
            val basePeekHeight = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 60f, resources.displayMetrics
            ).toInt()
            drawerBehavior.peekHeight = basePeekHeight + systemBars.bottom
            insets
        }

// 2. On ajuste la marge en temps réel pendant le glissement
        drawerBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {}

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                // slideOffset va de 0.0 (fermé) à 1.0 (ouvert)
                val headerParams = drawerBinding.drawerHeader.layoutParams as RelativeLayout.LayoutParams

                // La marge est proportionnelle à l'ouverture
                // Si fermé (0.0) -> marge = 0
                // Si ouvert (1.0) -> marge = statusBarSize
                headerParams.topMargin = (slideOffset * statusBarSize).toInt()

                drawerBinding.drawerHeader.layoutParams = headerParams
            }
        })

        drawerBinding.drawerFabAction.setOnClickListener(this)
        drawerBinding.drawerFabSettings.setOnClickListener(this)

        drawerBinding.appsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        drawerBinding.contactsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        drawerBinding.appsRecyclerView.adapter = appsAdapter
        drawerBinding.contactsRecyclerView.adapter = contactsAdapter

        initAppDrawerViewModel(drawerBinding)
        setupAppDrawerSearch(drawerBinding)
    }

    private fun initAppDrawerViewModel(drawerBinding: FragmentAppDrawerBottomSheetBinding) {
        viewModel.appList.observe(viewLifecycleOwner) { rawAppList ->
            rawAppList?.let { list ->
                val appsByProfile = list.groupBy { it.profileType }
                val mergedList = listOf("SYSTEM", "PRIVATE", "WORK", "USER").flatMap { profile ->
                    appsByProfile[profile].orEmpty()
                }
                drawerBinding.listEmptyHint.isVisible = mergedList.isEmpty()
                drawerBinding.sidebarContainer.isVisible = prefs.showAZSidebar
                appsAdapter.setAppList(mergedList.toMutableList())
            }
        }

        viewModel.contactList.observe(viewLifecycleOwner) { newList ->
            newList?.let {
                drawerBinding.listEmptyHint.isVisible = it.isEmpty()
                contactsAdapter.setContactList(it.toMutableList())
            }
        }
    }

    private fun setupAppDrawerSearch(drawerBinding: FragmentAppDrawerBottomSheetBinding) {
        drawerBinding.search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                val searchQuery = query?.trim() ?: ""
                if (searchQuery.isNotEmpty()) {
                    if (drawerBinding.menuView.displayedChild == 0) {
                        appsAdapter.launchFirstInList()
                    } else {
                        contactsAdapter.launchFirstInList()
                    }
                    drawerBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let {
                    if (drawerBinding.menuView.displayedChild == 0) appsAdapter.filter.filter(it.trim())
                    else contactsAdapter.filter.filter(it.trim())
                }
                return false
            }
        })
    }

    private fun dismissDialogs() {
        dialogBuilder.backupRestoreBottomSheet?.dismiss()
        dialogBuilder.saveLoadThemeBottomSheet?.dismiss()
        dialogBuilder.saveDownloadWOTDBottomSheet?.dismiss()
        dialogBuilder.singleChoiceBottomSheetPill?.dismiss()
        dialogBuilder.singleChoiceBottomSheet?.dismiss()
        dialogBuilder.colorPickerBottomSheet?.dismiss()
        dialogBuilder.sliderBottomSheet?.dismiss()
        dialogBuilder.flagSettingsBottomSheet?.dismiss()
        dialogBuilder.showDeviceBottomSheet?.dismiss()
    }

    // --- Widget Logic ---

    private fun initWidgetHost() {
        widgetDao = WidgetDatabase.getDatabase(requireContext()).widgetDao()
        appWidgetManager = AppWidgetManager.getInstance(requireContext())
        appWidgetHost = AppWidgetHost(requireContext(), APP_WIDGET_HOST_ID)
        appWidgetHost.startListening()

        binding.homeWidgetGrid.apply {
            setOnLongClickListener {
                val resizing = widgetWrappers.any { it.isResizeMode }
                if (!resizing) {
                    showGridMenu()
                    true
                } else false
            }
        }
    }

    private fun showGridMenu() {
        activeGridDialog?.dismiss()
        val bottomSheetDialog = FontBottomSheetDialogLocked(requireContext())
        activeGridDialog = bottomSheetDialog

        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        fun addOption(title: String, action: () -> Unit) {
            val option = TextView(requireContext()).apply {
                text = title
                textSize = 16f
                setPadding(16, 32, 16, 32)
                setOnClickListener { action(); bottomSheetDialog.dismiss() }
            }
            container.addView(option)
        }

        addOption(getLocalizedString(R.string.widgets_add_widget)) { showCustomWidgetPicker() }

        val editTitle = if (isEditingWidgets) getLocalizedString(R.string.widgets_stop_editing_widget) else getLocalizedString(R.string.widgets_edit_widget)
        addOption(editTitle) {
            isEditingWidgets = !isEditingWidgets
            updateWidgetEditMode()
        }

        if (isEditingWidgets) {
            addOption(getLocalizedString(R.string.widgets_remove_widget)) { removeAllWidgets() }
        }

        bottomSheetDialog.setContentView(container)
        bottomSheetDialog.show()
    }

    private fun updateWidgetEditMode() {
        if (isEditingWidgets) {
            val border = GradientDrawable().apply {
                setStroke(4, "#80F5A97F".toColorInt())
                cornerRadius = 16f
            }
            binding.homeWidgetGrid.background = border
        } else {
            binding.homeWidgetGrid.background = null
        }
        widgetWrappers.forEach { it.invalidate() }
    }

    private fun showCustomWidgetPicker() {
        val widgets = appWidgetManager.installedProviders
        val pm = requireContext().packageManager

        val grouped = widgets.groupBy { it.provider.packageName }.map { (pkg, widgetList) ->
            val appInfo = try { pm.getApplicationInfo(pkg, 0) } catch (_: Exception) { null }
            val appName = appInfo?.let { pm.getApplicationLabel(it).toString() } ?: pkg
            val appIcon = appInfo?.let { pm.getApplicationIcon(it) }
            AppWidgetGroup(appName, appIcon, widgetList.toMutableList())
        }.sortedBy { it.appName.lowercase() }

        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        val scrollView = ScrollView(requireContext()).apply { addView(container) }

        activeGridDialog?.dismiss()
        val bottomSheetDialog = FontBottomSheetDialogLocked(requireContext())
        activeGridDialog = bottomSheetDialog
        bottomSheetDialog.setContentView(scrollView)
        bottomSheetDialog.show()

        grouped.forEach { group ->
            val appRow = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(16, 24, 16, 24)
                gravity = Gravity.CENTER_VERTICAL
            }
            val iconView = ImageView(requireContext()).apply {
                setImageDrawable(group.appIcon)
                layoutParams = LinearLayout.LayoutParams(96, 96)
            }
            val labelView = TextView(requireContext()).apply {
                text = group.appName
                textSize = 18f
                setTypeface(null, Typeface.BOLD)
                setPadding(24, 0, 0, 0)
            }
            appRow.addView(iconView)
            appRow.addView(labelView, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

            val widgetContainer = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                visibility = View.GONE
            }

            group.widgets.forEach { widgetInfo ->
                val widgetLabel = widgetInfo.loadLabel(pm)
                val widgetRow = TextView(requireContext()).apply {
                    text = widgetLabel
                    textSize = 16f
                    setPadding(120, 24, 16, 24)
                    setOnClickListener {
                        addWidget(widgetInfo)
                        bottomSheetDialog.dismiss()
                    }
                }
                widgetContainer.addView(widgetRow)
            }

            appRow.setOnClickListener {
                widgetContainer.visibility = if (widgetContainer.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            }

            container.addView(appRow)
            container.addView(widgetContainer)
        }
    }

    private fun addWidget(widgetInfo: AppWidgetProviderInfo) {
        lastWidgetInfo = widgetInfo
        val widgetId = appWidgetHost.allocateAppWidgetId()
        val bound = appWidgetManager.bindAppWidgetIdIfAllowed(widgetId, widgetInfo.provider)
        if (bound) {
            maybeConfigureOrCreate(widgetInfo, widgetId)
        } else {
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, widgetInfo.provider)
            }
            (requireActivity() as MainActivity).launchWidgetPermission(intent) { resultCode, returnedId, _ ->
                handleWidgetResult(resultCode, returnedId)
            }
        }
    }

    private fun handleWidgetResult(resultCode: Int, appWidgetId: Int) {
        if (resultCode == Activity.RESULT_OK) {
            lastWidgetInfo?.let { maybeConfigureOrCreate(it, appWidgetId) }
        } else {
            appWidgetHost.deleteAppWidgetId(appWidgetId)
        }
        lastWidgetInfo = null
    }

    private fun maybeConfigureOrCreate(widgetInfo: AppWidgetProviderInfo, widgetId: Int) {
        if (widgetInfo.configure != null) {
            val intent = Intent().apply {
                component = widgetInfo.configure
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            }
            (requireActivity() as MainActivity).launchWidgetPermission(intent) { resultCode, returnedId, _ ->
                if (resultCode == Activity.RESULT_OK) {
                    createWidgetWrapperSafe(widgetInfo, returnedId)
                } else {
                    appWidgetHost.deleteAppWidgetId(returnedId)
                }
            }
        } else {
            createWidgetWrapperSafe(widgetInfo, widgetId)
        }
    }

    private fun createWidgetWrapperSafe(widgetInfo: AppWidgetProviderInfo, appWidgetId: Int) {
        binding.homeWidgetGrid.post {
            createWidgetWrapper(widgetInfo, appWidgetId)
        }
    }

    private fun createWidgetWrapper(widgetInfo: AppWidgetProviderInfo, appWidgetId: Int) {
        val hostView = try {
            appWidgetHost.createView(requireContext(), appWidgetId, widgetInfo)
        } catch (e: Exception) {
            appWidgetHost.deleteAppWidgetId(appWidgetId)
            return
        }

        val cellWidth = (binding.homeWidgetGrid.width - (GRID_COLUMNS - 1) * CELL_MARGIN) / GRID_COLUMNS
        val cellHeight = cellWidth

        val cellsW = ceil(widgetInfo.minWidth.toDouble() / (cellWidth + CELL_MARGIN)).toInt().coerceAtLeast(MIN_CELL_W)
        val cellsH = ceil(widgetInfo.minHeight.toDouble() / (cellHeight + CELL_MARGIN)).toInt().coerceAtLeast(MIN_CELL_H)

        val wrapper = ResizableWidgetWrapper(
            requireContext(), hostView, widgetInfo, appWidgetHost,
            onUpdate = { saveWidgets() },
            onDelete = { deleteWidget(appWidgetId) },
            isEditingProvider = { isEditingWidgets },
            GRID_COLUMNS, CELL_MARGIN, cellsW, cellsH
        )

        addWrapperToGrid(wrapper)
        saveWidgets()
        updateEmptyPlaceholder()
    }

    private fun addWrapperToGrid(wrapper: ResizableWidgetWrapper) {
        val parentWidth = binding.homeWidgetGrid.width.coerceAtLeast(1)
        val cellWidth = (parentWidth - (GRID_COLUMNS - 1) * CELL_MARGIN) / GRID_COLUMNS
        val cellHeight = cellWidth

        val occupied = widgetWrappers.map { w ->
            Pair(
                ((w.translationX + cellWidth / 2) / (cellWidth + CELL_MARGIN)).toInt(),
                ((w.translationY + cellHeight / 2) / (cellHeight + CELL_MARGIN)).toInt()
            )
        }

        var row = 0
        var col = 0
        var placed = false
        loop@ for (r in 0..100) {
            for (c in 0 until GRID_COLUMNS) {
                if (occupied.none { it.first == c && it.second == r }) {
                    col = c
                    row = r
                    placed = true
                    break@loop
                }
            }
        }

        wrapper.translationX = col * (cellWidth + CELL_MARGIN).toFloat()
        wrapper.translationY = row * (cellHeight + CELL_MARGIN).toFloat()

        binding.homeWidgetGrid.addView(wrapper)
        widgetWrappers.add(wrapper)
    }

    private fun deleteWidget(widgetId: Int) {
        appWidgetHost.deleteAppWidgetId(widgetId)
        val wrapper = widgetWrappers.find { it.hostView.appWidgetId == widgetId }
        wrapper?.let {
            binding.homeWidgetGrid.removeView(it)
            widgetWrappers.remove(it)
        }
        lifecycleScope.launch(Dispatchers.IO) {
            widgetDao.deleteById(widgetId)
        }
        updateEmptyPlaceholder()
    }

    private fun removeAllWidgets() {
        widgetWrappers.toList().forEach { deleteWidget(it.hostView.appWidgetId) }
        updateEmptyPlaceholder()
    }

    private fun saveWidgets() {
        val parentWidth = binding.homeWidgetGrid.width.coerceAtLeast(1)
        val cellWidth = (parentWidth - CELL_MARGIN * (GRID_COLUMNS - 1)) / GRID_COLUMNS
        val cellHeight = cellWidth

        val savedList = widgetWrappers.map { wrapper ->
            val col = ((wrapper.translationX + cellWidth / 2) / (cellWidth + CELL_MARGIN)).toInt().coerceIn(0, GRID_COLUMNS - 1)
            val row = ((wrapper.translationY + cellHeight / 2) / (cellHeight + CELL_MARGIN)).toInt().coerceAtLeast(0)
            val cellsW = ((wrapper.width + CELL_MARGIN) / (cellWidth + CELL_MARGIN))
            val cellsH = ((wrapper.height + CELL_MARGIN) / (cellHeight + CELL_MARGIN))
            SavedWidgetEntity(wrapper.hostView.appWidgetId, col, row, wrapper.width, wrapper.height, cellsW, cellsH)
        }

        lifecycleScope.launch(Dispatchers.IO) {
            widgetDao.insertAll(savedList)
        }
    }

    private fun restoreWidgets() {
        lifecycleScope.launch {
            val savedWidgets = withContext(Dispatchers.IO) { widgetDao.getAll() }
            binding.homeWidgetGrid.post {
                val parentWidth = binding.homeWidgetGrid.width.coerceAtLeast(1)
                val cellWidth = (parentWidth - CELL_MARGIN * (GRID_COLUMNS - 1)) / GRID_COLUMNS
                val cellHeight = cellWidth

                savedWidgets.forEach { saved ->
                    val info = appWidgetManager.getAppWidgetInfo(saved.appWidgetId) ?: return@forEach
                    val hostView = try {
                        appWidgetHost.createView(requireContext(), saved.appWidgetId, info)
                    } catch (e: Exception) { return@forEach }

                    val wrapper = ResizableWidgetWrapper(
                        requireContext(), hostView, info, appWidgetHost,
                        onUpdate = { saveWidgets() },
                        onDelete = { deleteWidget(saved.appWidgetId) },
                        isEditingProvider = { isEditingWidgets },
                        GRID_COLUMNS, CELL_MARGIN, saved.cellsW, saved.cellsH
                    )

                    wrapper.translationX = saved.col * (cellWidth + CELL_MARGIN).toFloat()
                    wrapper.translationY = saved.row * (cellHeight + CELL_MARGIN).toFloat()
                    wrapper.layoutParams = FrameLayout.LayoutParams(saved.width, saved.height)

                    binding.homeWidgetGrid.addView(wrapper)
                    widgetWrappers.add(wrapper)
                }
                updateEmptyPlaceholder()
            }
        }
    }

    private fun updateEmptyPlaceholder() {
        binding.widgetEmptyPlaceholder.visibility = if (widgetWrappers.isEmpty()) View.VISIBLE else View.GONE
    }

    companion object {
        private val APP_WIDGET_HOST_ID = "CascadeLauncher".hashCode().absoluteValue
        private const val GRID_COLUMNS = 14
        private const val CELL_MARGIN = 16
        private const val MIN_CELL_W = 2
        private const val MIN_CELL_H = 1
    }

}
