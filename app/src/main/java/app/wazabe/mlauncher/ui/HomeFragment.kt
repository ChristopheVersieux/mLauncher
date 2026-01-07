package app.wazabe.mlauncher.ui

import HomeAppsAdapter
import android.annotation.SuppressLint
import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.Context.VIBRATOR_SERVICE
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.os.Vibrator
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
import android.text.style.SuperscriptSpan
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.TypedValue
import android.view.GestureDetector
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.ScrollView
import android.widget.Space
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.SearchView
import androidx.biometric.BiometricPrompt
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
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
import app.wazabe.mlauncher.data.AppListItem
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
import app.wazabe.mlauncher.helper.analytics.AppUsageMonitor
import app.wazabe.mlauncher.helper.formatMillisToHMS
import app.wazabe.mlauncher.helper.getHexForOpacity
import app.wazabe.mlauncher.helper.getSystemIcons
import app.wazabe.mlauncher.helper.hasUsageAccessPermission
import app.wazabe.mlauncher.helper.initActionService
import app.wazabe.mlauncher.helper.openAppInfo
import app.wazabe.mlauncher.helper.receivers.DeviceAdmin
import app.wazabe.mlauncher.helper.setTopPadding
import app.wazabe.mlauncher.helper.showPermissionDialog
import app.wazabe.mlauncher.helper.utils.AppReloader
import app.wazabe.mlauncher.helper.utils.BiometricHelper
import app.wazabe.mlauncher.helper.utils.PrivateSpaceManager
import app.wazabe.mlauncher.listener.GestureAdapter
import app.wazabe.mlauncher.listener.GestureManager
import app.wazabe.mlauncher.listener.NotificationDotManager
import app.wazabe.mlauncher.services.ActionService
import app.wazabe.mlauncher.ui.adapter.AppDrawerAdapter
import app.wazabe.mlauncher.ui.adapter.ContactDrawerAdapter
import app.wazabe.mlauncher.ui.components.DialogManager
import app.wazabe.mlauncher.ui.widgets.AppWidgetGroup
import app.wazabe.mlauncher.ui.widgets.LauncherAppWidgetHost
import app.wazabe.mlauncher.ui.widgets.ResizableWidgetWrapper
import app.wazabe.mlauncher.ui.widgets.WidgetActivity
import com.github.creativecodecat.components.views.FontBottomSheetDialogLocked
import com.github.droidworksstudio.common.AnalyticsHelper
import com.github.droidworksstudio.common.AppLogger
import com.github.droidworksstudio.common.ColorIconsExtensions
import com.github.droidworksstudio.common.attachGestureManager
import com.github.droidworksstudio.common.hideKeyboard
import com.github.droidworksstudio.common.isSystemApp
import com.github.droidworksstudio.common.launchCalendar
import com.github.droidworksstudio.common.openAlarmApp
import com.github.droidworksstudio.common.openDeviceSettings
import com.github.droidworksstudio.common.openDialerApp
import com.github.droidworksstudio.common.openDigitalWellbeing
import com.github.droidworksstudio.common.showShortToast
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.absoluteValue
import kotlin.math.ceil

class HomeFragment : BaseFragment(), View.OnClickListener, View.OnLongClickListener, SharedPreferences.OnSharedPreferenceChangeListener{

    companion object {
        private const val TAG = "HomeFragment"
        private val APP_WIDGET_HOST_ID = "CascadeLauncher".hashCode().absoluteValue
        private const val GRID_COLUMNS = 14
        private const val CELL_MARGIN = 16
        private const val MIN_CELL_W = 2
        private const val MIN_CELL_H = 1
    }

    private lateinit var prefs: Prefs
    private lateinit var viewModel: MainViewModel
    private lateinit var drawerBehavior: BottomSheetBehavior<View>
    private lateinit var appsAdapter: AppDrawerAdapter
    private lateinit var contactsAdapter: ContactDrawerAdapter
    private lateinit var dialogBuilder: DialogManager
    private lateinit var deviceManager: DevicePolicyManager


    private lateinit var biometricHelper: BiometricHelper

    private lateinit var vibrator: Vibrator
    private lateinit var homeAppsAdapter: HomeAppsAdapter

    private lateinit var widgetDao: WidgetDao
    private lateinit var appWidgetManager: AppWidgetManager
    private lateinit var appWidgetHost: LauncherAppWidgetHost
    private val widgetWrappers = mutableListOf<ResizableWidgetWrapper>()
    private var isEditingWidgets: Boolean = false
    private var activeGridDialog: FontBottomSheetDialogLocked? = null
    private var lastWidgetInfo: AppWidgetProviderInfo? = null

    private var longPressToSelectApp: Int = 0
    private var selectedTag: String? = null
    private var currentProfileFilter: String? = null
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
        dialogBuilder = DialogManager(requireContext(), requireActivity())


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
                showHomeAppMenu(location)
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

        // Update dynamic UI elements
        updateTimeAndInfo()

        // Register battery receiver
        context?.let { ctx ->


            prefs.prefsNormal.registerOnSharedPreferenceChangeListener(this)
        }
    }

    override fun onResume() {
        super.onResume()
        
        // Reset adapter flags to LaunchApp mode to prevent staying in edit mode
        if (::appsAdapter.isInitialized) {
            appsAdapter.flag = Constants.AppDrawerFlag.LaunchApp
            appsAdapter.location = 0
        }
        
        if (::appWidgetHost.isInitialized) {
            appWidgetHost.startListening()
            restoreWidgets()
        }
    }

    override fun onPause() {
        super.onPause()
        if (::appWidgetHost.isInitialized) {
            appWidgetHost.stopListening()
        }
    }

    override fun onStop() {
        super.onStop()
        context?.let { ctx ->
            prefs.prefsNormal.unregisterOnSharedPreferenceChangeListener(this)
        }
        dismissDialogs()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    private fun updateUIFromPreferences() {
        binding.apply {
            // Static UI setup
            dailyWord.textSize = prefs.appSize.toFloat()
            homeScreenPager.textSize = prefs.appSize.toFloat()

            dailyWord.isVisible = dailyWord.text.toString().isNotBlank()
            dailyWord.setTextColor(prefs.appColor)
            
            // Rounded corners for drawer
            val radius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 28f, resources.displayMetrics)
            
            // Drawer must be opaque to cover widgets
            val bgColor = prefs.backgroundColor
            val opaqueColor = android.graphics.Color.argb(255, 
                android.graphics.Color.red(bgColor),
                android.graphics.Color.green(bgColor),
                android.graphics.Color.blue(bgColor)
            )

            val backgroundDrawable = GradientDrawable().apply {
                setColor(opaqueColor)
                cornerRadii = floatArrayOf(radius, radius, radius, radius, 0f, 0f, 0f, 0f)
            }
            appDrawerLayout.root.background = backgroundDrawable
            mainLayout.setBackgroundColor(getHexForOpacity(prefs))

            dailyWord.gravity = Gravity.CENTER
            (dailyWord.layoutParams as LinearLayout.LayoutParams).gravity = Gravity.CENTER
            
            setupAppDrawerSearch(appDrawerLayout)
        }
        if (::homeAppsAdapter.isInitialized) {
            homeAppsAdapter.notifyDataSetChanged()
        }
    }

    private fun updateTimeAndInfo() {
        viewModel.fetchRandomFact()
    }


    override fun onClick(view: View) {
        try { // Launch app
            val appLocation = view.id
            homeAppClicked(appLocation)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onLongClick(view: View): Boolean {
        if (prefs.homeLocked) {
            trySettings()
            return true
        }

        val n = view.id
        showAppList(AppDrawerFlag.SetHomeApp, includeRecentApps = false, n)
        AnalyticsHelper.logUserAction("Show App List")
        return true
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initSwipeTouchListener() {
        // Modified: Check if touch lands on widget grid, if so, don't consume
        val gestureManager = GestureManager(requireContext(), object : GestureAdapter() {
            override fun onShortSwipeLeft() {
                when (val action = prefs.shortSwipeLeftAction) {
                    Action.OpenApp -> openSwipeLeftApp()
                    else -> handleOtherAction(action)
                }
            }
            override fun onLongSwipeLeft() {
                when (val action = prefs.longSwipeLeftAction) {
                    Action.OpenApp -> openLongSwipeLeftApp()
                    else -> handleOtherAction(action)
                }
            }
            override fun onShortSwipeRight() {
                when (val action = prefs.shortSwipeRightAction) {
                    Action.OpenApp -> openSwipeRightApp()
                    else -> handleOtherAction(action)
                }
            }
            override fun onLongSwipeRight() {
                when (val action = prefs.longSwipeRightAction) {
                    Action.OpenApp -> openLongSwipeRightApp()
                    else -> handleOtherAction(action)
                }
            }
            override fun onShortSwipeUp() {
                when (val action = prefs.shortSwipeUpAction) {
                    Action.OpenApp -> openSwipeUpApp()
                    else -> handleOtherAction(action)
                }
            }
            override fun onLongSwipeUp() {
                when (val action = prefs.longSwipeUpAction) {
                    Action.OpenApp -> openLongSwipeUpApp()
                    else -> handleOtherAction(action)
                }
            }
            override fun onShortSwipeDown() {
                when (val action = prefs.shortSwipeDownAction) {
                    Action.OpenApp -> openSwipeDownApp()
                    else -> handleOtherAction(action)
                }
            }
            override fun onLongSwipeDown() {
                when (val action = prefs.longSwipeDownAction) {
                    Action.OpenApp -> openLongSwipeDownApp()
                    else -> handleOtherAction(action)
                }
            }
            override fun onLongPress() {
                AppLogger.d(TAG, "touchArea.onLongPress - opening settings")
                trySettings()
            }
            override fun onDoubleTap() {
                when (val action = prefs.doubleTapAction) {
                    Action.OpenApp -> openDoubleTapApp()
                    else -> handleOtherAction(action)
                }
            }
        })
        
        binding.touchArea.setOnTouchListener { _, event ->
            AppLogger.d(TAG, "touchArea.onTouch: action=${event.actionMasked}, raw=(${event.rawX},${event.rawY})")
            
            // Check if touch lands on widget grid - if so, pass through to widgets
            val widgetGrid = binding.homeWidgetGrid
            val location = IntArray(2)
            widgetGrid.getLocationOnScreen(location)
            val gridLeft = location[0]
            val gridTop = location[1]
            val gridRight = gridLeft + widgetGrid.width
            val gridBottom = gridTop + widgetGrid.height
            
            val x = event.rawX
            val y = event.rawY
            val touchOnWidgetGrid = x >= gridLeft && x <= gridRight && y >= gridTop && y <= gridBottom && widgetGrid.childCount > 1 // >1 because placeholder is also a child
            
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                AppLogger.d(TAG, "touchArea: ACTION_DOWN at ($x, $y), widgetGrid bounds=($gridLeft,$gridTop,$gridRight,$gridBottom), touchOnWidgetGrid=$touchOnWidgetGrid, childCount=${widgetGrid.childCount}")
            }
            
            if (touchOnWidgetGrid) {
                AppLogger.d(TAG, "touchArea: Touch on widget grid, passing through")
                return@setOnTouchListener false  // Don't consume, let widgets handle
            }
            
            val result = gestureManager.onTouchEvent(event)
            AppLogger.d(TAG, "touchArea protocol: gestureManager.onTouchEvent result=$result")
            result
        }
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

    @SuppressLint("ClickableViewAccessibility")
    private fun initClickListeners() {
        // Long press anywhere on home screen (grid, touch area, or likely the apps list background) opens settings
        val openSettingsAction = View.OnLongClickListener { 
            AppLogger.d(TAG, "General LongClick triggered on ${it.id}")
            trySettings()
            true
        }

        binding.homeWidgetGrid.setOnLongClickListener(openSettingsAction)
        binding.touchArea.setOnLongClickListener(openSettingsAction)
        
        val gestureDetector = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            override fun onLongPress(e: MotionEvent) {
                val child = binding.homeAppsRecyclerView.findChildViewUnder(e.x, e.y)
                if (child == null) {
                    trySettings()
                }
            }
        })

        binding.homeAppsRecyclerView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false
        }

        binding.mainLayout.setOnLongClickListener(openSettingsAction)
        binding.coordinatorLayout.setOnLongClickListener(openSettingsAction)
        
        binding.appDrawerLayout.search.setOnClickListener {
            // Focus on search view
            binding.appDrawerLayout.search.isIconified = false
        }
        
        binding.dailyWord.setOnClickListener {
            viewModel.fetchRandomFact(force = true)
        }
    }


    private fun initAppObservers() {
        // No remaining views to observe
    }

    private fun initObservers() {
        with(viewModel) {
            launcherDefault.observe(viewLifecycleOwner) { isDefault ->
               // default launcher check visual removed
            }

            randomFact.observe(viewLifecycleOwner) { fact ->
                binding.dailyWord.text = fact
                binding.dailyWord.isVisible = fact.isNotBlank()
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


    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            "SHOW_DATE", "SHOW_CLOCK", "SHOW_ALARM", "SHOW_BATTERY",
            "DATE_SIZE_TEXT", "CLOCK_SIZE_TEXT", "ALARM_SIZE_TEXT", "BATTERY_SIZE_TEXT", "APP_SIZE_TEXT",
            "DATE_COLOR", "CLOCK_COLOR", "ALARM_CLOCK_COLOR", "BATTERY_COLOR", "APP_COLOR",
            "BACKGROUND_COLOR", "APP_OPACITY", "SHOW_BACKGROUND", "TEXT_PADDING_SIZE",
            "SHOW_WEATHER", "APP_USAGE_STATS", "DRAWER_TYPE", "HIDE_SEARCH_VIEW" -> {
                if (key == "DRAWER_TYPE" || key == "HIDE_SEARCH_VIEW") {
                    selectedTag = null
                    currentProfileFilter = null
                    viewModel.clearAppCache()
                    if (key == "HIDE_SEARCH_VIEW") {
                        setupAppDrawer() // Re-setup to hide/show search
                    }
                }
                updateUIFromPreferences()
                viewModel.getAppList()
            }
            "HOME_ALIGNMENT", "CLOCK_ALIGNMENT", "DATE_ALIGNMENT", "ALARM_ALIGNMENT", "DRAWER_ALIGNMENT", "HOME_ALIGNMENT_BOTTOM" -> {
                if (key == "DRAWER_ALIGNMENT") {
                    setupAppDrawer() // Re-create adapter to pick new XML layout
                }
                if (key == "HOME_ALIGNMENT") {
                    // Recreate homeAppsAdapter to pick new XML layout
                    homeAppsAdapter = HomeAppsAdapter(
                        prefs,
                        onClick = { location -> homeAppClicked(location) },
                        onLongClick = { location -> showHomeAppMenu(location) }
                    )
                    binding.homeAppsRecyclerView.adapter = homeAppsAdapter
                }
                updateUIFromPreferences()
                binding.mainLayout.requestLayout()
            }
            "ICON_PACK_HOME", "CUSTOM_ICON_PACK_HOME", "ICON_PACK_APP_LIST", "CUSTOM_ICON_PACK_APP_LIST" -> {
                updateUIFromPreferences()
            }
        }
    }


    private fun homeAppClicked(location: Int) {
        AnalyticsHelper.logUserAction("Clicked Home App: $location")
        if (prefs.getAppName(location).isEmpty()) {
            if (prefs.homeLocked) trySettings() else showLongPressToast()
        } else {
            viewModel.launchApp(prefs.getHomeAppModel(location), this)
        }
    }

    private fun showAppList(flag: AppDrawerFlag, includeRecentApps: Boolean = true, n: Int = 0) {
        viewModel.getAppList(includeRecentApps)
        appsAdapter.flag = flag
        appsAdapter.location = n
        appsAdapter.notifyDataSetChanged()
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
            Action.ShowAppList -> showAppList(AppDrawerFlag.LaunchApp)
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


    private fun showLongPressToast() = showShortToast(getString(longPressToSelectApp))

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


        // Update the total number of pages and calculate maximum apps per page
        updatePagesAndAppsPerPage(prefs.homeAppsNum, prefs.homePagesNum)
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
                            getString(R.string.text_authentication_failed)
                        )
                    }

                    override fun onAuthenticationError(
                        errorCode: Int,
                        errorMessage: CharSequence?
                    ) {
                        when (errorCode) {
                            BiometricPrompt.ERROR_USER_CANCELED -> AppLogger.e(
                                "Authentication",
                                getString(R.string.text_authentication_cancel)
                            )

                            else ->
                                AppLogger.e(
                                    "Authentication",
                                    getString(R.string.text_authentication_error).format(
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
            Constants.Gravity.IconOnly -> Gravity.CENTER
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
                showCenteredAppOptionsDialog(requireContext(), viewModel, appModel)
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
                TypedValue.COMPLEX_UNIT_DIP, 24f, resources.displayMetrics
            ).toInt()
            
            // Push search down prevents it from being visible in peek area (Nav Bar + 24dp Buffer)
            // We use a spacer view instead of margin on searchview to avoid layout issues
            val spacerParams = drawerBinding.peekSpacer.layoutParams
            val buffer = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 60f, resources.displayMetrics).toInt()
            spacerParams.height = systemBars.bottom + buffer
            drawerBinding.peekSpacer.layoutParams = spacerParams
            
            drawerBehavior.peekHeight = basePeekHeight + systemBars.bottom
            insets
        }

// 2. On ajuste la marge en temps réel pendant le glissement
        drawerBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                // Reset to normal launch mode when drawer collapses
                if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    drawerBinding.search.clearFocus()
                    hideKeyboard()
                    if (::appsAdapter.isInitialized) {
                        appsAdapter.flag = Constants.AppDrawerFlag.LaunchApp
                        appsAdapter.location = 0
                    }
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                // slideOffset va de 0.0 (fermé) à 1.0 (ouvert)
                val headerParams = drawerBinding.drawerHeader.layoutParams as RelativeLayout.LayoutParams

                // La marge est proportionnelle à l'ouverture
                // Si fermé (0.0) -> marge = 0
                // Si ouvert (1.0) -> marge = statusBarSize
                headerParams.topMargin = (slideOffset * statusBarSize).toInt()
                drawerBinding.drawerHeader.layoutParams = headerParams
                
                // Animate peekSpacer: full height when collapsed (0), minimal when expanded (1)
                val maxSpacerHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 120f, resources.displayMetrics).toInt()
                val minSpacerHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8f, resources.displayMetrics).toInt()
                val spacerParams = drawerBinding.peekSpacer.layoutParams
                spacerParams.height = (maxSpacerHeight - (slideOffset * (maxSpacerHeight - minSpacerHeight))).toInt()
                drawerBinding.peekSpacer.layoutParams = spacerParams
            }
        })

        drawerBinding.appsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        drawerBinding.contactsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        drawerBinding.appsRecyclerView.adapter = appsAdapter
        drawerBinding.contactsRecyclerView.adapter = contactsAdapter

        drawerBinding.clearFiltersButton.setOnClickListener {
            drawerBinding.search.setQuery("", false)
            selectedTag = null
            currentProfileFilter = null
            viewModel.clearAppCache()
            viewModel.getAppList()
        }

        initAppDrawerViewModel(drawerBinding)
        setupAppDrawerSearch(drawerBinding)
    }


    private fun showCenteredAppOptionsDialog(context: Context, viewModel: MainViewModel, appModel: AppListItem) {
        val dialogView = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
            gravity = Gravity.CENTER_HORIZONTAL
        }

        // App Icon
        val iconView = android.widget.ImageView(context)
        val iconSize = (60 * resources.displayMetrics.density).toInt()
        val iconParams = android.widget.LinearLayout.LayoutParams(iconSize, iconSize)
        iconParams.bottomMargin = 20
        iconView.layoutParams = iconParams
        try {
            iconView.setImageDrawable(app.wazabe.mlauncher.helper.IconPackHelper.getSafeAppIcon(context, appModel.activityPackage, true, app.wazabe.mlauncher.helper.IconCacheTarget.APP_LIST))
        } catch (e: Exception) {
            iconView.setImageResource(R.drawable.app_launcher)
        }
        dialogView.addView(iconView)

        // App Name
        val nameView = android.widget.TextView(context)
        nameView.text = appModel.label
        nameView.textSize = 20f
        // Removed setTextColor to use system default
        nameView.gravity = Gravity.CENTER
        val nameParams = android.widget.LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        nameParams.bottomMargin = 40
        nameView.layoutParams = nameParams
        dialogView.addView(nameView)

        val dialog = androidx.appcompat.app.AlertDialog.Builder(context)
            .setView(dialogView)
            .create()

        // Helper to add buttons
        fun addButton(text: String, iconRes: Int, onClick: () -> Unit) {
            val button = com.google.android.material.button.MaterialButton(context, null, android.R.attr.borderlessButtonStyle)
            button.text = text
            button.setIconResource(iconRes)
            button.iconTint = null // Keep original icon colors
            button.iconPadding = (16 * resources.displayMetrics.density).toInt() // Add space between icon and text
            button.iconGravity = com.google.android.material.button.MaterialButton.ICON_GRAVITY_TEXT_START
            button.gravity = Gravity.START or Gravity.CENTER_VERTICAL
            button.setOnClickListener {
                dialog.dismiss()
                onClick()
            }
            // Ensure full width
            val params = android.widget.LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            button.layoutParams = params
            dialogView.addView(button)
        }

        // Helper to add separator
        fun addSeparator() {
            val view = android.view.View(context)
            val isDark = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
            view.setBackgroundColor(if (isDark) android.graphics.Color.parseColor("#33FFFFFF") else android.graphics.Color.parseColor("#1F000000"))
            val params = android.widget.LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (1 * resources.displayMetrics.density).toInt()
            )
            view.layoutParams = params
            dialogView.addView(view)
        }

        addButton("Rename", R.drawable.ic_rename) {
            showRenameDialog(context, viewModel, appModel.activityPackage, appModel.customLabel)
        }
        
        addSeparator()

        addButton("Tag", R.drawable.ic_tag) {
            showTagDialog(context, viewModel, appModel.activityPackage, appModel.customTag, appModel.user)
        }
        
        addSeparator()

        val isHidden = prefs.hiddenApps.contains(appModel.activityPackage + "|" + appModel.activityClass + "|" + appModel.user.hashCode())
        val hideText = if (isHidden) "Show App" else "Hide App"
        val hideIcon = if (isHidden) R.drawable.visibility else R.drawable.visibility_off
        addButton(hideText, hideIcon) {
             val newSet = mutableSetOf<String>()
             newSet.addAll(prefs.hiddenApps)
             val key = appModel.activityPackage + "|" + appModel.activityClass + "|" + appModel.user.hashCode()
             if (isHidden) newSet.remove(key) else newSet.add(key)
             prefs.hiddenApps = newSet
             viewModel.getAppList()
        }
        
        addSeparator()
        
        addButton("App Info", R.drawable.ic_info) {
            openAppInfo(requireContext(), appModel.user, appModel.activityPackage)
            drawerBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }

        dialog.show()
    }

    private fun showRenameDialog(context: Context, viewModel: MainViewModel, pkg: String, alias: String) {
        val editText = android.widget.EditText(context)
        editText.setText(alias)
        editText.setSelectAllOnFocus(true)

        val container = android.widget.FrameLayout(context)
        val params = android.widget.FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        val margin = (20 * resources.displayMetrics.density).toInt()
        params.setMargins(margin, margin, margin, margin)
        editText.layoutParams = params
        container.addView(editText)

        androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle(R.string.rename)
            .setView(container)
            .setPositiveButton(R.string.save) { _, _ ->
                val newName = editText.text.toString().trim()
                Prefs(context).setAppAlias(pkg, newName)
                viewModel.getAppList()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
        
        editText.requestFocus()
    }

    private fun showTagDialog(context: Context, viewModel: MainViewModel, pkg: String, tag: String, user: UserHandle) {
        val editText = android.widget.EditText(context)
        editText.hint = context.getString(R.string.new_tag)
        editText.setSelectAllOnFocus(true)

        // Collect existing tags
        val existingTags = viewModel.appList.value?.map { it.customTag }?.filter { !it.isNullOrBlank() }?.distinct()?.sorted() ?: emptyList()

        val container = android.widget.LinearLayout(context)
        container.orientation = android.widget.LinearLayout.VERTICAL
        val margin = (20 * resources.displayMetrics.density).toInt()
        container.setPadding(margin, margin, margin, margin)

        // Filter Chips (only if tags exist)
        if (existingTags.isNotEmpty()) {
            val scrollView = android.widget.HorizontalScrollView(context)
            val scrollParams = android.widget.LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            scrollParams.bottomMargin = margin
            scrollView.layoutParams = scrollParams
            scrollView.isHorizontalScrollBarEnabled = false

            val chipGroup = com.google.android.material.chip.ChipGroup(context).apply {
                setTag("DIALOG_CHIP_GROUP")
                isSingleLine = true
            }
            
            existingTags.forEach { existingTag ->
                val chip = com.google.android.material.chip.Chip(context)
                chip.text = existingTag
                val currentAppTags = tag.split(",").map { it.trim() }.filter { it.isNotBlank() }
                chip.isCheckable = true
                chip.isChecked = currentAppTags.contains(existingTag)
                // Chip handles toggling itself because isCheckable = true
                chipGroup.addView(chip)
            }
            scrollView.addView(chipGroup)
            container.addView(scrollView)
        }

        // EditText
        val params = android.widget.LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        editText.layoutParams = params
        container.addView(editText)

        androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle(R.string.tag)
            .setView(container)
            .setPositiveButton(R.string.save) { _, _ ->
                val typedTags = editText.text.toString().split(",").map { it.trim() }.filter { it.isNotBlank() }
                val chipGroup = container.findViewWithTag<com.google.android.material.chip.ChipGroup>("DIALOG_CHIP_GROUP")
                val selectedChips = mutableListOf<String>()
                
                if (chipGroup != null) {
                    for (i in 0 until chipGroup.childCount) {
                        val chip = chipGroup.getChildAt(i) as? com.google.android.material.chip.Chip
                        if (chip?.isChecked == true) {
                            selectedChips.add(chip.text.toString())
                        }
                    }
                }

                // Merge: selected chips + typed tags, maintaining order of chips then new ones
                val finalTagsSet = (selectedChips + typedTags).distinct()
                val finalTagString = finalTagsSet.joinToString(", ")

                android.util.Log.d("TagEdit", "Final merged tags for $pkg: '$finalTagString'")
                Prefs(context).setAppTag(pkg, finalTagString, user)
                viewModel.clearAppCache()
                viewModel.getAppList()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
        
        editText.requestFocus()
    }

    private fun initAppDrawerViewModel(drawerBinding: FragmentAppDrawerBottomSheetBinding) {
        viewModel.appList.observe(viewLifecycleOwner) { rawAppList ->
            rawAppList?.let { list ->
                setupFilterChips(drawerBinding, list) {
                    viewModel.appList.value = viewModel.appList.value // Trigger refresh
                }

                val appsByProfile = list.groupBy { it.profileType }
                val allProfiles = listOf("SYSTEM", "PRIVATE", "WORK", "USER")
                
                // Merge apps based on profile filter
                val mergedList = allProfiles.flatMap { profile ->
                    val apps = appsByProfile[profile].orEmpty()
                    if (apps.isNotEmpty() && (currentProfileFilter == null || currentProfileFilter.equals(profile, true))) {
                        apps
                    } else emptyList()
                }

                // Filter by Tag if needed
                val finalFilteredList = if (selectedTag != null) {
                    mergedList.filter { 
                         it.customTag.split(",").map { t -> t.trim() }.contains(selectedTag)
                    }
                } else {
                    mergedList
                }

                val isEmpty = finalFilteredList.isEmpty()
                if (drawerBinding.menuView.displayedChild == 0) { // Apps view
                    drawerBinding.emptyStateContainer.isVisible = isEmpty
                    drawerBinding.appsRecyclerView.isVisible = !isEmpty
                    drawerBinding.sidebarContainer.isVisible = prefs.showAZSidebar && prefs.drawerType == Constants.DrawerType.Alphabetical && !isEmpty
                }
                appsAdapter.setAppList(finalFilteredList.toMutableList())
            }
        }

        viewModel.contactList.observe(viewLifecycleOwner) { newList ->
            newList?.let {
                val isEmpty = it.isEmpty()
                if (drawerBinding.menuView.displayedChild == 1) { // Contacts view
                     drawerBinding.emptyStateContainer.isVisible = isEmpty
                     drawerBinding.contactsRecyclerView.isVisible = !isEmpty
                }
                contactsAdapter.setContactList(it.toMutableList())
            }
        }
    }

    private fun setupFilterChips(drawerBinding: FragmentAppDrawerBottomSheetBinding, list: List<AppListItem>, onFilterChanged: () -> Unit) {
        val drawerType = prefs.drawerType
        val tags = list.flatMap { it.customTag.split(",").map { t -> t.trim() } }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()

        val shouldShowTags = tags.isNotEmpty()
        val shouldShowProfiles = false // Placeholder for future work profile logic

        if (!shouldShowTags && !shouldShowProfiles) {
            drawerBinding.filterBarContainer.isVisible = false
            if (selectedTag != null || currentProfileFilter != null) {
                selectedTag = null
                currentProfileFilter = null
                view?.post { onFilterChanged() }
            }
            return
        }

        // Reset selectedTag if it no longer exists
        if (selectedTag != null && !tags.contains(selectedTag)) {
            selectedTag = null
            view?.post { onFilterChanged() }
        }

        drawerBinding.filterBarContainer.isVisible = true
        val chipGroup = drawerBinding.filterChipGroup

        // Clear existing chips
        chipGroup.removeAllViews()

        // Helper to create chips
        fun createFilterChip(id: Int, label: String, isSelected: Boolean): com.google.android.material.chip.Chip {
            return com.google.android.material.chip.Chip(requireContext(), null, R.attr.chipStyle).apply {
                setChipBackgroundColorResource(R.color.chip_background_color)
                setTextColor(ContextCompat.getColorStateList(requireContext(), R.color.chip_text_color))
                chipStrokeColor = ContextCompat.getColorStateList(requireContext(), R.color.chip_stroke_color)
                chipStrokeWidth = (1 * resources.displayMetrics.density)
                checkedIconTint = ContextCompat.getColorStateList(requireContext(), android.R.color.white)
                this.id = id
                this.text = label
                this.isCheckable = true
                this.isChecked = isSelected
                this.isCheckedIconVisible = true
            }
        }

        if (shouldShowProfiles) {
            val profiles = mutableListOf<String>()
            if (prefs.getProfileCounter("SYSTEM") > 0) profiles.add("SYSTEM")
            if (prefs.getProfileCounter("WORK") > 0) profiles.add("WORK")
            if (prefs.getProfileCounter("PRIVATE") > 0 && !PrivateSpaceManager(requireContext()).isPrivateSpaceLocked()) profiles.add("PRIVATE")

            // "All" Chip
            val allChip = createFilterChip(View.generateViewId(), "All", currentProfileFilter == null)
            chipGroup.addView(allChip)

            profiles.forEach { profile ->
                val label = when(profile) {
                    "SYSTEM" -> "Personal"
                    "WORK" -> "Work"
                    "PRIVATE" -> "Private"
                    else -> profile
                }
                val chip = createFilterChip(View.generateViewId(), label, profile == currentProfileFilter)
                chip.tag = profile
                chipGroup.addView(chip)
            }
        } else if (shouldShowTags) {
            // "All" Chip
            val allChip = createFilterChip(View.generateViewId(), "All", selectedTag == null)
            chipGroup.addView(allChip)

            tags.forEach { tag ->
                val chip = createFilterChip(View.generateViewId(), tag, tag == selectedTag)
                chipGroup.addView(chip)
            }
        }
        
        // Listener for changes
        chipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            // Single selection, so checkedIds should have 0 or 1 item
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            
            val checkedId = checkedIds[0]
            val chip = group.findViewById<com.google.android.material.chip.Chip>(checkedId)
            val newTag = if (chip.text == "All") null else chip.text.toString()

            if (selectedTag != newTag) {
                selectedTag = newTag
                view?.post { onFilterChanged() }
            }
        }
        
        drawerBinding.drawerRoot.requestLayout()
    }



    private fun setupAppDrawerSearch(drawerBinding: FragmentAppDrawerBottomSheetBinding) {
        val hideSearch = prefs.hideSearchView
       drawerBinding.search.isVisible = !hideSearch
        if (hideSearch) {
            drawerBinding.search.clearFocus()
            return
        }
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
        dialogBuilder.singleChoiceBottomSheetPill?.dismiss()
        dialogBuilder.singleChoiceBottomSheet?.dismiss()
        dialogBuilder.colorPickerBottomSheet?.dismiss()
        dialogBuilder.sliderBottomSheet?.dismiss()
        dialogBuilder.flagSettingsBottomSheet?.dismiss()
        dialogBuilder.showDeviceBottomSheet?.dismiss()
    }

    // --- Widget Logic ---

    private fun initWidgetHost() {
        val appContext = requireContext().applicationContext
        widgetDao = WidgetDatabase.getDatabase(appContext).widgetDao()
        appWidgetManager = AppWidgetManager.getInstance(appContext)
        appWidgetHost = LauncherAppWidgetHost(appContext, APP_WIDGET_HOST_ID)
        // Don't start listening here - will be done in onResume()

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

        addOption(getString(R.string.widgets_add_widget)) { showCustomWidgetPicker() }

        val editTitle = if (isEditingWidgets) getString(R.string.widgets_stop_editing_widget) else getString(R.string.widgets_edit_widget)
        addOption(editTitle) {
            isEditingWidgets = !isEditingWidgets
            updateWidgetEditMode()
        }

        if (isEditingWidgets) {
            addOption(getString(R.string.widgets_remove_widget)) { removeAllWidgets() }
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
                val previewDrawable = widgetInfo.loadPreviewImage(requireContext(), 0)
                    ?: widgetInfo.loadIcon(requireContext(), 0)
                
                val widgetRow = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(48, 16, 16, 16)
                    gravity = Gravity.CENTER_VERTICAL
                    
                    // Preview image
                    val previewView = ImageView(requireContext()).apply {
                        setImageDrawable(previewDrawable)
                        layoutParams = LinearLayout.LayoutParams(260, 260).apply {
                            marginEnd = 16
                        }
                        scaleType = ImageView.ScaleType.FIT_CENTER
                    }
                    addView(previewView)
                    
                    // Label and size info
                    val infoContainer = LinearLayout(requireContext()).apply {
                        orientation = LinearLayout.VERTICAL
                        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                        
                        val labelText = TextView(requireContext()).apply {
                            text = widgetLabel
                            textSize = 15f
                        }
                        addView(labelText)
                        
                        val sizeText = TextView(requireContext()).apply {
                            text = "${widgetInfo.minWidth}x${widgetInfo.minHeight}"
                            textSize = 12f
                            alpha = 0.6f
                        }
                        addView(sizeText)
                    }
                    addView(infoContainer)
                    
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
            (requireActivity() as MainActivity).launchWidgetConfiguration(appWidgetHost, widgetId) { resultCode, returnedId ->
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
        val appContext = requireContext().applicationContext
        val hostView = try {
            appWidgetHost.createView(appContext, appWidgetId, widgetInfo)
        } catch (e: Exception) {
            AppLogger.e("CVE", "❌ Failed to create widgetId=$appWidgetId: ${e.message}")
            appWidgetHost.deleteAppWidgetId(appWidgetId)
            return
        }

        val cellWidth = (binding.homeWidgetGrid.width - (GRID_COLUMNS - 1) * CELL_MARGIN) / GRID_COLUMNS
        val cellHeight = cellWidth

        val cellsW = ceil(widgetInfo.minWidth.toDouble() / (cellWidth + CELL_MARGIN)).toInt().coerceAtLeast(MIN_CELL_W)
        val cellsH = ceil(widgetInfo.minHeight.toDouble() / (cellHeight + CELL_MARGIN)).toInt().coerceAtLeast(MIN_CELL_H)

        val wrapper = ResizableWidgetWrapper(
            requireContext(), hostView, widgetInfo, appWidgetHost,
            appWidgetId,
            onUpdate = { saveWidgets() },
            onDelete = { deleteWidget(appWidgetId) },
            onLongPress = { showGridMenu() },
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

    private fun clearAllWidgets() {
        widgetWrappers.forEach { binding.homeWidgetGrid.removeView(it) }
        widgetWrappers.clear()
    }

    private fun saveWidgets() {
        val parentWidth = binding.homeWidgetGrid.width.coerceAtLeast(1)
        val cellWidth = (parentWidth - CELL_MARGIN * (GRID_COLUMNS - 1)) / GRID_COLUMNS
        val cellHeight = cellWidth

        val savedList = widgetWrappers.mapNotNull { wrapper ->
            val widgetId = wrapper.appWidgetId
            if (widgetId < 0) return@mapNotNull null
            val col = ((wrapper.translationX + cellWidth / 2) / (cellWidth + CELL_MARGIN)).toInt().coerceIn(0, GRID_COLUMNS - 1)
            val row = ((wrapper.translationY + cellHeight / 2) / (cellHeight + CELL_MARGIN)).toInt().coerceAtLeast(0)
            val cellsW = ((wrapper.width + CELL_MARGIN) / (cellWidth + CELL_MARGIN))
            val cellsH = ((wrapper.height + CELL_MARGIN) / (cellHeight + CELL_MARGIN))
            SavedWidgetEntity(widgetId, col, row, wrapper.width, wrapper.height, cellsW, cellsH)
        }

        if (savedList.isEmpty()) return

        lifecycleScope.launch(Dispatchers.IO) {
            widgetDao.insertAll(savedList)
        }
    }

    private fun restoreWidgets() {
        clearAllWidgets()
        
        lifecycleScope.launch {
            val savedWidgets = withContext(Dispatchers.IO) { widgetDao.getAll() }
            binding.homeWidgetGrid.post {
                val parentWidth = binding.homeWidgetGrid.width.coerceAtLeast(1)
                val cellWidth = (parentWidth - CELL_MARGIN * (GRID_COLUMNS - 1)) / GRID_COLUMNS
                val cellHeight = cellWidth

                savedWidgets.forEach { saved ->
                    // Skip invalid widget IDs and clean up the database
                    if (saved.appWidgetId < 0) {
                        lifecycleScope.launch(Dispatchers.IO) { widgetDao.deleteById(saved.appWidgetId) }
                        return@forEach
                    }
                    
                    val info = appWidgetManager.getAppWidgetInfo(saved.appWidgetId)
                    if (info == null) {
                        // Widget no longer exists, clean up
                        lifecycleScope.launch(Dispatchers.IO) { widgetDao.deleteById(saved.appWidgetId) }
                        return@forEach
                    }
                    
                    val hostView = try {
                        val appContext = requireContext().applicationContext
                        appWidgetHost.createView(appContext, saved.appWidgetId, info)
                    } catch (e: Exception) {
                        AppLogger.e("CVE", "❌ Failed to restore widgetId=${saved.appWidgetId}: ${e.message}")
                        return@forEach
                    }

                    val wrapper = ResizableWidgetWrapper(
                        requireContext(), hostView, info, appWidgetHost,
                        saved.appWidgetId,
                        onUpdate = { saveWidgets() },
                        onDelete = { deleteWidget(saved.appWidgetId) },
                        onLongPress = { showGridMenu() },
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



    private fun removeHomeApp(position: Int) {
        // Clear the app at this position
        val emptyApp = AppListItem(
            activityLabel = "",
            activityPackage = "",
            activityClass = "",
            user = android.os.Process.myUserHandle(),
            customLabel = "",
            customTag = ""
        )
        
        // Check if this is the last position
        val currentTotal = prefs.homeAppsNum
        if (position == currentTotal - 1) {
            // If it's the last slot, decrease the total count
            prefs.homeAppsNum = currentTotal - 1
            homeAppsAdapter.notifyDataSetChanged()
        } else {
            // Otherwise just clear it
            prefs.setHomeAppModel(position, emptyApp)
            homeAppsAdapter.notifyItemChanged(position)
        }
    }

    @SuppressLint("SetTextI18n", "RestrictedApi")
    private fun showHomeAppMenu(position: Int) {
        if (prefs.homeLocked) {
            trySettings()
            return
        }
        val appModel = prefs.getHomeAppModel(position)
        val isEmpty = appModel.activityPackage.isEmpty()

        val viewHolder = binding.homeAppsRecyclerView.findViewHolderForAdapterPosition(position) ?: return
        val anchorView = viewHolder.itemView.findViewById<View>(R.id.appIcon) ?: viewHolder.itemView

        val popup = androidx.appcompat.widget.PopupMenu(requireContext(), anchorView)
        val menu = popup.menu

        // Enable dividers
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            menu.setGroupDividerEnabled(true)
        }

        // Force icons to show
        try {
            val field = popup.javaClass.getDeclaredField("mPopup")
            field.isAccessible = true
            val menuPopupHelper = field.get(popup)
            val classPopupHelper = Class.forName(menuPopupHelper.javaClass.name)
            val setForceIcons = classPopupHelper.getMethod("setForceShowIcon", Boolean::class.javaPrimitiveType)
            setForceIcons.invoke(menuPopupHelper, true)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Helper to get icon with colored circle background
        fun getIconWithBackground(iconRes: Int, bgColorRes: Int): Drawable {
            val icon = ContextCompat.getDrawable(requireContext(), iconRes)?.mutate()?.apply {
                setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
            }
            
            val bgColor = ContextCompat.getColor(requireContext(), bgColorRes)
            val circle = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(bgColor)
            }
            
            val size = (24 * resources.displayMetrics.density).toInt()
            val padding = (6 * resources.displayMetrics.density).toInt()
            
            return LayerDrawable(arrayOf(circle, icon ?: ColorDrawable(Color.TRANSPARENT))).apply {
                setLayerSize(0, size, size)
                setLayerSize(1, size - padding * 2, size - padding * 2)
                setLayerGravity(1, Gravity.CENTER)
            }
        }

        // Group 0: Main actions
        menu.add(0, 2, 0, "Change").icon = getIconWithBackground(R.drawable.ic_restart, R.color.bg_icon_circle_blue)
        
        if (!isEmpty) {
            menu.add(0, 3, 1, "App Info").icon = getIconWithBackground(R.drawable.ic_info, R.color.bg_icon_circle_teal)
        }
        
        // Group 1: Settings & Destructive actions (Separated)
        menu.add(1, 4, 2, "Cascade Settings").icon = getIconWithBackground(R.drawable.ic_settings, R.color.bg_icon_circle_orange)
        menu.add(1, 1, 3, "Remove").icon = getIconWithBackground(R.drawable.ic_close, R.color.bg_icon_circle_red)

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> removeHomeApp(position)
                2 -> showAppList(AppDrawerFlag.SetHomeApp, includeRecentApps = false, position)
                3 -> openAppInfo(requireContext(), appModel.user, appModel.activityPackage)
                4 -> trySettings()
            }
            true
        }
        
        popup.show()
    }
    
    private fun getSelectableBackground(): Drawable {
        val attrs = intArrayOf(android.R.attr.selectableItemBackground)
        val typedArray = requireContext().obtainStyledAttributes(attrs)
        val drawable = typedArray.getDrawable(0)
        typedArray.recycle()
        return drawable ?: GradientDrawable()
    }
}

