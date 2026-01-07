/**
 * The view for the list of all the installed applications.
 */

package app.wazabe.mlauncher.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.UserHandle
import android.os.UserManager
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.content.SharedPreferences
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.util.TypedValue
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import android.content.res.ColorStateList
import android.graphics.Color
import androidx.core.content.ContextCompat
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.SearchView
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.droidworksstudio.common.AppLogger

import com.github.droidworksstudio.common.hasSoftKeyboard
import com.github.droidworksstudio.common.isGestureNavigationEnabled
import com.github.droidworksstudio.common.isSystemApp
import com.github.droidworksstudio.common.searchCustomSearchEngine
import com.github.droidworksstudio.common.searchOnPlayStore
import com.github.droidworksstudio.common.showShortToast
import app.wazabe.mlauncher.MainViewModel
import app.wazabe.mlauncher.Mlauncher
import app.wazabe.mlauncher.R
import app.wazabe.mlauncher.data.AppCategory
import app.wazabe.mlauncher.data.AppListItem
import app.wazabe.mlauncher.data.Constants
import app.wazabe.mlauncher.data.Constants.AppDrawerFlag
import app.wazabe.mlauncher.data.ContactListItem
import app.wazabe.mlauncher.data.Prefs
import app.wazabe.mlauncher.databinding.FragmentAppDrawerBinding
import app.wazabe.mlauncher.helper.emptyString
import app.wazabe.mlauncher.helper.getHexForOpacity
import app.wazabe.mlauncher.helper.hasContactsPermission
import app.wazabe.mlauncher.helper.ismlauncherDefault
import app.wazabe.mlauncher.helper.openAppInfo
import app.wazabe.mlauncher.helper.utils.PrivateSpaceManager
import app.wazabe.mlauncher.ui.adapter.AppDrawerAdapter
import app.wazabe.mlauncher.ui.adapter.ContactDrawerAdapter

class AppDrawerFragment : BaseFragment(), SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var prefs: Prefs
    private lateinit var viewModel: MainViewModel
    private lateinit var appsAdapter: AppDrawerAdapter
    private lateinit var contactsAdapter: ContactDrawerAdapter
    private var selectedTag: String? = null
    private var currentProfileFilter: String? = null
    private var flag: AppDrawerFlag = AppDrawerFlag.LaunchApp  // Current drawer mode

    private var _binding: FragmentAppDrawerBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppDrawerBinding.inflate(inflater, container, false)
        prefs = Prefs(requireContext())
        return binding.root
    }

    private fun setupFilterChips(list: List<AppListItem>, onFilterChanged: () -> Unit) {
        val drawerType = prefs.drawerType
        val tags = list.flatMap { it.customTag.split(",").map { t -> t.trim() } }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()

        val shouldShowTags = tags.isNotEmpty()
        val shouldShowProfiles = false // Placeholder for future work profile logic

        if (!shouldShowTags && !shouldShowProfiles) {
            binding.filterChipGroup.isVisible = false
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

        binding.filterChipGroup.isVisible = true
        val chipGroup = binding.filterChipGroup

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

        binding.root.requestLayout()
    }





    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    @SuppressLint("RtlHardcoded")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (prefs.firstSettingsOpen) {
            prefs.firstSettingsOpen = false
        }

        // Check if device is using gesture navigation or 3-button navigation
        val isGestureNav = isGestureNavigationEnabled(requireContext())

        binding.apply {
            val params = menuView.layoutParams as ViewGroup.MarginLayoutParams
            if (isGestureNav) {
                params.bottomMargin = resources.getDimensionPixelSize(R.dimen.bottom_margin_gesture_nav) // or just in px
            } else {
                params.bottomMargin = resources.getDimensionPixelSize(R.dimen.bottom_margin_3_button_nav) // or just in px
            }
            menuView.layoutParams = params

            val layoutParams = sidebarContainer.layoutParams as RelativeLayout.LayoutParams

            // Clear old alignment rules
            layoutParams.removeRule(RelativeLayout.ALIGN_PARENT_START)
            layoutParams.removeRule(RelativeLayout.ALIGN_PARENT_END)

            // Apply new alignment based on prefs
            when (prefs.drawerAlignment) {
                Constants.Gravity.Left -> layoutParams.addRule(RelativeLayout.ALIGN_PARENT_END)
                Constants.Gravity.Center,
                Constants.Gravity.IconLeft,
                Constants.Gravity.IconCenter,
                Constants.Gravity.IconRight,
                Constants.Gravity.Right -> layoutParams.addRule(RelativeLayout.ALIGN_PARENT_START)
            }

            sidebarContainer.layoutParams = layoutParams

            searchSwitcher.setOnClickListener {
                switchMenus()
            }
            menuView.displayedChild = 0
        }

        // Retrieve the letter key code from arguments
        val letterKeyCode = arguments?.getInt("letterKeyCode", -1)
        if (letterKeyCode != null && letterKeyCode != -1) {
            val letterToChar = convertKeyCodeToLetter(letterKeyCode)
            val searchTextView = binding.search.findViewById<TextView>(R.id.search_src_text)
            searchTextView.text = letterToChar.toString()
        }


        val flagString = arguments?.getString("flag", AppDrawerFlag.LaunchApp.toString())
            ?: AppDrawerFlag.LaunchApp.toString()
        flag = AppDrawerFlag.valueOf(flagString)  // Set class property
        val n = arguments?.getInt("n", 0) ?: 0

        val profileType: String = arguments?.getString("profileType", "SYSTEM") ?: "SYSTEM"

        when (flag) {
            AppDrawerFlag.SetDoubleTap,
            AppDrawerFlag.SetShortSwipeRight,
            AppDrawerFlag.SetShortSwipeLeft,
            AppDrawerFlag.SetShortSwipeUp,
            AppDrawerFlag.SetShortSwipeDown,
            AppDrawerFlag.SetLongSwipeRight,
            AppDrawerFlag.SetLongSwipeLeft,
            AppDrawerFlag.SetLongSwipeUp,
            AppDrawerFlag.SetLongSwipeDown,
            AppDrawerFlag.SetClickClock,
            AppDrawerFlag.SetAppUsage,
            AppDrawerFlag.SetClickDate,
            AppDrawerFlag.SetFloating -> {
                binding.drawerButton.setOnClickListener {
                    findNavController().popBackStack()
                }
            }

            AppDrawerFlag.SetHomeApp -> {
                // Get UserManager
                val userManager = requireContext().getSystemService(Context.USER_SERVICE) as UserManager

                val clearApp = AppListItem(
                    activityLabel = "Clear",
                    activityPackage = emptyString(),
                    activityClass = emptyString(),
                    user = userManager.userProfiles[0], // or use Process.myUserHandle() if it makes more sense
                    profileType = "SYSTEM",
                    customLabel = "Clear",
                    customTag = emptyString(),
                    category = AppCategory.REGULAR
                )

                binding.drawerButton.setOnClickListener {
                    findNavController().popBackStack()
                }

                binding.clearHomeButton.apply {
                    val currentApp = prefs.getHomeAppModel(n)
                    if (currentApp.activityPackage.isNotEmpty() && currentApp.activityClass.isNotEmpty()) {
                        isVisible = true
                        text = getString(R.string.clear_home_app)
                        setTextColor(prefs.appColor)
                        textSize = prefs.appSize.toFloat()
                        setOnClickListener {
                            prefs.setHomeAppModel(n, clearApp)
                            findNavController().popBackStack()
                        }
                    }
                }

                if (app.wazabe.mlauncher.Mlauncher.prefs.launcherFont != "system") {
                    binding.root.post {
                        binding.root.applyCustomFont()
                    }
                }
            }



            else -> {}


        }

        viewModel = activity?.run {
            ViewModelProvider(this)[MainViewModel::class.java]
        } ?: throw Exception("Invalid Activity")

        val combinedScrollMaps = MediatorLiveData<Pair<Map<String, Int>, Map<String, Int>>>()

        combinedScrollMaps.addSource(viewModel.appScrollMap) { appMap ->
            combinedScrollMaps.value = Pair(appMap, viewModel.contactScrollMap.value ?: emptyMap())
        }
        combinedScrollMaps.addSource(viewModel.contactScrollMap) { contactMap ->
            combinedScrollMaps.value = Pair(viewModel.appScrollMap.value ?: emptyMap(), contactMap)
        }

       /* combinedScrollMaps.observe(viewLifecycleOwner) { (appMap, contactMap) ->
            binding.azSidebar.onLetterSelected = { section ->
                when (binding.menuView.displayedChild) {
                    0 -> appMap[section]?.let { index ->
                        binding.appsRecyclerView.smoothScrollToPosition(index)
                    }

                    1 -> contactMap[section]?.let { index ->
                        binding.contactsRecyclerView.smoothScrollToPosition(index)
                    }
                }
            }
        }*/


        val gravity = when (Prefs(requireContext()).drawerAlignment) {
            Constants.Gravity.Left -> Gravity.LEFT
            Constants.Gravity.Center -> Gravity.CENTER
            Constants.Gravity.Right -> Gravity.RIGHT
            Constants.Gravity.IconLeft -> Gravity.LEFT
            Constants.Gravity.IconCenter -> Gravity.CENTER
            Constants.Gravity.IconRight -> Gravity.RIGHT
        }

        val appAdapter = context?.let {
            parentFragment?.let { fragment ->
                AppDrawerAdapter(
                    it,
                    fragment,
                    flag,
                    gravity,
                    appClickListener(viewModel, flag, n),
                    { appModel ->
                        showCenteredAppOptionsDialog(requireContext(), viewModel, appModel)
                    }
                )
            }
        }

        val contactAdapter = context?.let {
            parentFragment?.let { fragment ->
                ContactDrawerAdapter(
                    it,
                    gravity,
                    contactClickListener(viewModel, n)
                )
            }
        }


        when (binding.menuView.displayedChild) {
            0 -> appAdapter?.let { appsAdapter = it }
            1 -> contactAdapter?.let { contactsAdapter = it }
        }

        val searchTextView = binding.search.findViewById<TextView>(R.id.search_src_text)

        val textSize = prefs.appSize.toFloat()
        searchTextView.textSize = textSize

        if (appAdapter != null && contactAdapter != null) {
            initViewModel(flag, viewModel, appAdapter, contactAdapter, profileType)
        }

        binding.appsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.contactsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.appsRecyclerView.adapter = appAdapter
        binding.contactsRecyclerView.adapter = contactAdapter

        var lastSectionLetter: String? = null

        binding.appsRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            var onTop = false

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
                val itemCount = layoutManager.itemCount
                if (itemCount == 0) return

                val firstVisible = layoutManager.findFirstVisibleItemPosition()
                val lastVisible = layoutManager.findLastVisibleItemPosition()
                if (firstVisible == RecyclerView.NO_POSITION || lastVisible == RecyclerView.NO_POSITION) return

                val position = when {
                    firstVisible <= 1 -> firstVisible
                    lastVisible >= itemCount - 2 -> lastVisible
                    else -> (firstVisible + lastVisible) / 2
                }.coerceIn(0, itemCount - 1)

                val item = appAdapter?.getItemAt(position) ?: return

                val sectionLetter = when (item.category) {
                    AppCategory.PINNED -> "★"
                    else -> item.label.firstOrNull()?.uppercaseChar()?.toString() ?: return
                }

                // Skip redundant updates
                if (sectionLetter == lastSectionLetter) return
                lastSectionLetter = sectionLetter

                //binding.azSidebar.setSelectedLetter(sectionLetter)
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                when (newState) {

                    RecyclerView.SCROLL_STATE_DRAGGING -> {
                        onTop = !recyclerView.canScrollVertically(-1)
                        if (onTop) {
                            if (requireContext().hasSoftKeyboard()) {
                                binding.search.hideKeyboard()
                            }
                        }
                        if (onTop && !recyclerView.canScrollVertically(1)) {
                            findNavController().popBackStack()
                        }
                    }

                    RecyclerView.SCROLL_STATE_IDLE -> {
                        if (!recyclerView.canScrollVertically(1)) {
                            binding.search.hideKeyboard()
                        } else if (!recyclerView.canScrollVertically(-1)) {
                            if (onTop) {
                                findNavController().popBackStack()
                            } else {
                                if (requireContext().hasSoftKeyboard()) {
                                    binding.search.showKeyboard()
                                }
                            }
                        }
                    }
                }
            }
        })

        binding.contactsRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            var onTop = false

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
                val itemCount = layoutManager.itemCount
                if (itemCount == 0) return

                val firstVisible = layoutManager.findFirstVisibleItemPosition()
                val lastVisible = layoutManager.findLastVisibleItemPosition()
                if (firstVisible == RecyclerView.NO_POSITION || lastVisible == RecyclerView.NO_POSITION) return

                val position = when {
                    firstVisible <= 1 -> firstVisible
                    lastVisible >= itemCount - 2 -> lastVisible
                    else -> (firstVisible + lastVisible) / 2
                }.coerceIn(0, itemCount - 1)

                val item = contactAdapter?.getItemAt(position) ?: return

                val sectionLetter = item.displayName.firstOrNull()?.uppercaseChar()?.toString() ?: return

                // Skip redundant updates
                if (sectionLetter == lastSectionLetter) return
                lastSectionLetter = sectionLetter

                //binding.azSidebar.setSelectedLetter(sectionLetter)
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                when (newState) {

                    RecyclerView.SCROLL_STATE_DRAGGING -> {
                        onTop = !recyclerView.canScrollVertically(-1)
                        if (onTop) {
                            if (requireContext().hasSoftKeyboard()) {
                                binding.search.hideKeyboard()
                            }
                        }
                        if (onTop && !recyclerView.canScrollVertically(1)) {
                            findNavController().popBackStack()
                        }
                    }

                    RecyclerView.SCROLL_STATE_IDLE -> {
                        if (!recyclerView.canScrollVertically(1)) {
                            binding.search.hideKeyboard()
                        } else if (!recyclerView.canScrollVertically(-1)) {
                            if (onTop) {
                                findNavController().popBackStack()
                            } else {
                                if (requireContext().hasSoftKeyboard()) {
                                    binding.search.showKeyboard()
                                }
                            }
                        }
                    }
                }
            }
        })

        // Always set visibility based on preference
        updateSearchVisibility()
        
        if (!prefs.hideSearchView) {
            val appListButtonFlags = prefs.getMenuFlags("APPLIST_BUTTON_FLAGS", "00")
            when (flag) {
                AppDrawerFlag.LaunchApp -> {
                    setupProfileButtons(flag, viewModel, appAdapter, contactAdapter, profileType)


                    binding.internetSearch.apply {
                        isVisible = appListButtonFlags[0]
                        setOnClickListener {
                            val query = binding.search.query.toString().trim()
                            if (query.isEmpty()) return@setOnClickListener
                            requireContext().searchCustomSearchEngine(query, prefs)
                        }
                    }
                    binding.searchSwitcher.apply {
                        if (hasContactsPermission(context)) {
                            when (profileType) {
                                "WORK", "PRIVATE" -> isVisible = false
                                else -> {
                                    isVisible = appListButtonFlags[1]
                                    setOnClickListener { switchMenus() }
                                }

                            }
                        } else {
                            binding.menuView.displayedChild = 0
                        }
                    }
                }

                AppDrawerFlag.HiddenApps -> {
                    binding.search.queryHint = getString(R.string.hidden_apps)
                }

                AppDrawerFlag.SetHomeApp -> {
                    binding.search.queryHint = getString(R.string.please_select_app)
                }

                else -> {}
            }
        }

        // Set appropriate empty hint based on flag
        val emptyHintText = when (flag) {
            AppDrawerFlag.HiddenApps -> getString(R.string.hidden_apps_empty_hint)
            else -> getString(R.string.drawer_list_empty_hint)
        }
        binding.listEmptyHint.text = applyTextColor(emptyHintText, prefs.appColor)
        
        binding.clearFiltersButton.setOnClickListener {
            binding.search.setQuery("", false)
            selectedTag = null
            currentProfileFilter = null
            viewModel.clearAppCache()
            viewModel.getAppList()
            updateSearchVisibility()
        }

        binding.search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                val searchQuery = query?.trim()

                if (!searchQuery.isNullOrEmpty()) {

                    // Hashtag shortcut
                    if (searchQuery.startsWith("#")) return true

                    when (binding.menuView.displayedChild) {
                        0 -> { // appsAdapter
                            val firstItem = appAdapter?.getFirstInList()
                            if (firstItem.equals(searchQuery, ignoreCase = true) || prefs.openAppOnEnter) {
                                appAdapter?.launchFirstInList()
                            } else {
                                requireContext().searchOnPlayStore(searchQuery)
                            }
                        }

                        1 -> { // contactsAdapter
                            val firstItem = contactAdapter?.getFirstInList()
                            if (firstItem.equals(searchQuery, ignoreCase = true) || prefs.openAppOnEnter) {
                                contactAdapter?.launchFirstInList()
                            } else {
                                requireContext().searchOnPlayStore(searchQuery)
                            }
                        }
                    }

                    return true
                }

                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (flag == AppDrawerFlag.SetHomeApp) {
                    binding.drawerButton.apply {
                        isVisible = !newText.isNullOrEmpty()
                        text = if (isVisible) getString(R.string.rename) else null
                        setOnClickListener { if (isVisible) renameListener(flag, n) }
                    }
                    binding.clearHomeButton.apply {
                        isVisible = newText.isNullOrEmpty()
                    }
                }

                newText?.let {
                    when (binding.menuView.displayedChild) {
                        0 -> appAdapter?.filter?.filter(it.trim())
                        1 -> contactAdapter?.filter?.filter(it.trim())
                    }
                }
                return false
            }
        })

        // Initial data load
        if (flag == AppDrawerFlag.HiddenApps) {
            viewModel.getHiddenApps()
        } else {
            viewModel.getAppList()
        }
    }
    
    private fun updateSearchVisibility() {
        if (_binding == null) return
        val hide = prefs.hideSearchView
        binding.searchContainer.isVisible = !hide
        binding.search.isVisible = !hide
        if (hide) {
             binding.search.clearFocus()
        }
        binding.root.requestLayout()
    }

    private fun setupProfileButtons(
        flag: AppDrawerFlag,
        viewModel: MainViewModel,
        appAdapter: AppDrawerAdapter?,
        contactAdapter: ContactDrawerAdapter?,
        profileType: String
    ) {
        var currentProfileType = profileType

        fun updateProfileUI(profileType: String) {
            currentProfileType = profileType

            val isWorkProfileAvailable = prefs.getProfileCounter("WORK") > 0 && profileType != "WORK"
            val isPrivateProfileAvailable = prefs.getProfileCounter("PRIVATE") > 0 &&
                    profileType != "PRIVATE" &&
                    !PrivateSpaceManager(requireContext()).isPrivateSpaceLocked() &&
                    ismlauncherDefault(requireContext())
            val isSystemProfileAvailable = prefs.getProfileCounter("SYSTEM") > 0 && profileType != "SYSTEM"

            binding.workApps.isVisible = isWorkProfileAvailable
            binding.privateApps.isVisible = isPrivateProfileAvailable
            binding.systemApps.isVisible = isSystemProfileAvailable

            binding.search.queryHint = when (profileType) {
                "WORK" -> getString(R.string.show_work_apps)
                "PRIVATE" -> getString(R.string.show_private_apps)
                else -> getString(R.string.show_apps)
            }
        }

        fun onProfileClicked(newType: String) {
            binding.menuView.displayedChild = 0
            currentProfileFilter = newType
            viewModel.appList.value = viewModel.appList.value // Trigger refresh
            setAppViewDetails()
            updateProfileUI(newType)
        }

        // Initial setup
        updateProfileUI(currentProfileType)

        // Button listeners
        binding.workApps.setOnClickListener {
            onProfileClicked("WORK")
        }
        binding.privateApps.setOnClickListener {
            onProfileClicked("PRIVATE")
        }
        binding.systemApps.setOnClickListener {
            onProfileClicked("SYSTEM")
        }
    }


    fun switchMenus() {
        binding.apply {
            menuView.showNext()
            when (menuView.displayedChild) {
                0 -> {
                    setAppViewDetails()
                }

                1 -> {
                    setContactViewDetails()
                }
            }
        }
    }

    private fun setAppViewDetails() {
        binding.apply {
            searchSwitcher.setImageResource(R.drawable.ic_contacts)
            search.queryHint = getString(R.string.show_apps)
            search.setQuery("", false)
        }
    }

    private fun setContactViewDetails() {
        binding.apply {
            searchSwitcher.setImageResource(R.drawable.ic_apps)
            search.queryHint = getString(R.string.show_contacts)
            search.setQuery("", false)
        }
    }

    private fun applyTextColor(text: String, color: Int): SpannableString {
        val spannableString = SpannableString(text)
        spannableString.setSpan(
            ForegroundColorSpan(color),
            0,
            text.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return spannableString
    }

    private fun convertKeyCodeToLetter(keyCode: Int): Char {
        return when (keyCode) {
            KeyEvent.KEYCODE_A -> 'A'
            KeyEvent.KEYCODE_B -> 'B'
            KeyEvent.KEYCODE_C -> 'C'
            KeyEvent.KEYCODE_D -> 'D'
            KeyEvent.KEYCODE_E -> 'E'
            KeyEvent.KEYCODE_F -> 'F'
            KeyEvent.KEYCODE_G -> 'G'
            KeyEvent.KEYCODE_H -> 'H'
            KeyEvent.KEYCODE_I -> 'I'
            KeyEvent.KEYCODE_J -> 'J'
            KeyEvent.KEYCODE_K -> 'K'
            KeyEvent.KEYCODE_L -> 'L'
            KeyEvent.KEYCODE_M -> 'M'
            KeyEvent.KEYCODE_N -> 'N'
            KeyEvent.KEYCODE_O -> 'O'
            KeyEvent.KEYCODE_P -> 'P'
            KeyEvent.KEYCODE_Q -> 'Q'
            KeyEvent.KEYCODE_R -> 'R'
            KeyEvent.KEYCODE_S -> 'S'
            KeyEvent.KEYCODE_T -> 'T'
            KeyEvent.KEYCODE_U -> 'U'
            KeyEvent.KEYCODE_V -> 'V'
            KeyEvent.KEYCODE_W -> 'W'
            KeyEvent.KEYCODE_X -> 'X'
            KeyEvent.KEYCODE_Y -> 'Y'
            KeyEvent.KEYCODE_Z -> 'Z'
            else -> throw IllegalArgumentException("Invalid key code: $keyCode")
        }
    }

    private fun initViewModel(
        flag: AppDrawerFlag,
        viewModel: MainViewModel,
        appAdapter: AppDrawerAdapter,
        contactAdapter: ContactDrawerAdapter,
        profileFilter: String? = null // Initial filter
    ) {
        currentProfileFilter = profileFilter
        
        fun <T> observeList(
            liveData: LiveData<List<T>?>,
            currentList: List<T>,
            onPopulate: (List<T>) -> Unit,
            skipCondition: () -> Boolean = { false }
        ) {
            liveData.observe(viewLifecycleOwner) { newList ->
                val shouldSkip = skipCondition()
                val sameList = newList == currentList
                
                if (shouldSkip || sameList) {
                    return@observe
                }
                
                newList?.let {
                    val isEmpty = it.isEmpty()
                    binding.emptyStateContainer.isVisible = isEmpty

                    // Force visible just in case
                    binding.menuView.visibility = View.VISIBLE
                    
                    binding.sidebarContainer.isVisible = prefs.showAZSidebar
                    onPopulate(it)
                }
            }
        }

        // 🔹 Observe hidden apps
        observeList(
            viewModel.hiddenApps, appAdapter.appsList,
            onPopulate = { 
                populateAppList(it, appAdapter) 
            },
            skipCondition = { 
                val skip = flag != AppDrawerFlag.HiddenApps
                skip
            }
        )

        // 🔹 Observe contacts
        observeList(
            viewModel.contactList, contactAdapter.contactsList,
            onPopulate = { populateContactList(it, contactAdapter) },
            skipCondition = { binding.menuView.displayedChild != 0 }
        )

        // 🔹 Observe apps
        viewModel.appList.observe(viewLifecycleOwner) { rawAppList ->
            if (flag == AppDrawerFlag.HiddenApps) return@observe
            if (binding.menuView.displayedChild != 0) return@observe

            AppLogger.d("Apps", "Loaded ${rawAppList?.size ?: 0} raw apps")
            rawAppList?.let { list ->
                
                setupFilterChips(list) {
                    // Refresh when filter changed
                    viewModel.appList.value = viewModel.appList.value
                }

                val appsByProfile = list.groupBy { it.profileType }
                val allProfiles = listOf("SYSTEM", "PRIVATE", "WORK", "USER")

                // Update prefs counters
                allProfiles.forEach { profile ->
                    prefs.setProfileCounter(profile, appsByProfile[profile]?.size ?: 0)
                }

                // Update profile icons visibility based on new counts
                val isWorkProfileAvailable = prefs.getProfileCounter("WORK") > 0 && currentProfileFilter != "WORK"
                val isPrivateProfileAvailable = prefs.getProfileCounter("PRIVATE") > 0 && 
                        currentProfileFilter != "PRIVATE" && 
                        !PrivateSpaceManager(requireContext()).isPrivateSpaceLocked()
                val isSystemProfileAvailable = prefs.getProfileCounter("SYSTEM") > 0 && currentProfileFilter != "SYSTEM"

                binding.workApps.isVisible = isWorkProfileAvailable
                binding.privateApps.isVisible = isPrivateProfileAvailable
                binding.systemApps.isVisible = isSystemProfileAvailable

                // Merge apps based on profile filter
                val mergedList = allProfiles.flatMap { profile ->
                    val apps = appsByProfile[profile].orEmpty()
                    if (apps.isNotEmpty() && (currentProfileFilter == null || currentProfileFilter.equals(profile, true))) {
                        AppLogger.d("AppMerge", "Adding ${apps.size} $profile apps")
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

                AppLogger.d("AppMerge", "Final list (${finalFilteredList.size} apps)")

                binding.emptyStateContainer.isVisible = finalFilteredList.isEmpty()
                binding.sidebarContainer.isVisible = prefs.showAZSidebar && prefs.drawerType == Constants.DrawerType.Alphabetical
                populateAppList(finalFilteredList, appAdapter)
            }
        }

        // 🔹 Observe first open
        viewModel.firstOpen.observe(viewLifecycleOwner) {
            binding.appDrawerTip.isVisible = it
        }
    }

    override fun onResume() {
        super.onResume()
        updateSearchVisibility()
        if (requireContext().hasSoftKeyboard()) {
            binding.search.showKeyboard()
        }
    }

    override fun onStart() {
        super.onStart()
        prefs.prefsNormal.registerOnSharedPreferenceChangeListener(this)
        updateSearchVisibility()
    }

    override fun onStop() {
        super.onStop()
        prefs.prefsNormal.unregisterOnSharedPreferenceChangeListener(this)
        if (requireContext().hasSoftKeyboard()) {
            binding.search.hideKeyboard()
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == "DRAWER_TYPE" || key == "HIDE_SEARCH_VIEW") {
            if (key == "HIDE_SEARCH_VIEW") {
                updateSearchVisibility()
                return
            }
            selectedTag = null
            currentProfileFilter = null
            viewModel.clearAppCache() // Ensure we re-sort the list
            viewModel.getAppList()
        }
    }


    private fun View.showKeyboard() {
        if (!Prefs(requireContext()).autoShowKeyboard) return
        if (Prefs(requireContext()).hideSearchView) return

        val searchTextView = binding.search.findViewById<TextView>(R.id.search_src_text)
        searchTextView.requestFocus()
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        searchTextView.postDelayed({
            searchTextView.requestFocus()
            @Suppress("DEPRECATION")
            imm.showSoftInput(searchTextView, InputMethodManager.SHOW_FORCED)
        }, 100)
    }

    private fun View.hideKeyboard() {
        val imm: InputMethodManager? =
            context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        imm?.hideSoftInputFromWindow(windowToken, 0)
        this.clearFocus()
    }


    private fun populateAppList(apps: List<AppListItem>, appAdapter: AppDrawerAdapter) {
        val animation =
            AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.layout_anim_from_bottom)
        binding.appsRecyclerView.layoutAnimation = animation
        appAdapter.setAppList(apps.toMutableList())
    }

    private fun populateContactList(contacts: List<ContactListItem>, contactAdapter: ContactDrawerAdapter) {
        val animation =
            AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.layout_anim_from_bottom)
        binding.contactsRecyclerView.layoutAnimation = animation
        contactAdapter.setContactList(contacts.toMutableList())
    }

    private fun appClickListener(
        viewModel: MainViewModel,
        flag: AppDrawerFlag,
        n: Int = 0
    ): (appListItem: AppListItem) -> Unit = { appModel ->
        viewModel.selectedApp(this, appModel, flag, n)
        if (flag == AppDrawerFlag.LaunchApp || flag == AppDrawerFlag.HiddenApps || flag == AppDrawerFlag.SetHomeApp)
            findNavController().popBackStack(R.id.mainFragment, false)
        else
            findNavController().popBackStack()
    }

    private fun appDeleteListener(): (appListItem: AppListItem) -> Unit = { appModel ->
        if (requireContext().isSystemApp(appModel.activityPackage))
            showShortToast(getString(R.string.can_not_delete_system_apps))
        else {
            val appPackage = appModel.activityPackage
            val intent = Intent(Intent.ACTION_DELETE)
            intent.data = "package:$appPackage".toUri()
            requireContext().startActivity(intent)
        }

    }

    // Removed createAppRenameListener to use inline lambda

    // Removed createAppTagListener to use inline lambda
    

    private fun renameListener(flag: AppDrawerFlag, i: Int) {
        val name = binding.search.query.toString().trim()
        if (name.isEmpty()) return
        if (flag == AppDrawerFlag.SetHomeApp) {
            Prefs(requireContext()).setHomeAppName(i, name)
        }

        findNavController().popBackStack()
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
        // Async load icon would be better but for dialog this is fast enough usually
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
            try {
                appShowHideListener(viewModel).invoke(AppDrawerFlag.None, appModel)
            } catch (e: Exception) {
            }
        }
        
        addSeparator()
        
        addButton("App Info", R.drawable.ic_info) {
            appInfoListener().invoke(appModel)
        }

        dialog.show()
    }

    private fun showRenameDialog(context: Context, viewModel: MainViewModel, pkg: String, alias: String) {
        System.out.println("AppDrawerDebug: showRenameDialog for $pkg")
        
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
        System.out.println("AppDrawerDebug: showTagDialog for $pkg")

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

    private fun appShowHideListener(viewModel: MainViewModel): (flag: AppDrawerFlag, appListItem: AppListItem) -> Unit = { _, appModel ->
        // 1. Sauvegarde et refresh global
        viewModel.toggleAppVisibility(appModel, refreshMainList = true)
        
        // 2. DISPARITION IMMEDIATE ROBUSTE (Optimistic UI)
        if (this::appsAdapter.isInitialized) {
            val currentList = appsAdapter.appsList.toMutableList()
            
            // On supprime en cherchant par clé unique, pour éviter les problèmes d'instances différentes
            val wasRemoved = currentList.removeAll { item ->
                item.activityPackage == appModel.activityPackage && 
                item.activityClass == appModel.activityClass &&
                item.user == appModel.user
            }

            if (wasRemoved) {
                appsAdapter.setAppList(currentList)
            }
        }
        
        // Si on est dans le mode HiddenApps (ne devrait pas arriver ici avec la nouvelle nav), on rafraîchit quand même ou on pop
        if (this.flag == AppDrawerFlag.HiddenApps) {
             val prefs = Prefs(requireContext())
             if (prefs.hiddenApps.isEmpty()) {
                 findNavController().popBackStack()
             }
        }
    }

    private fun appInfoListener(): (appListItem: AppListItem) -> Unit = { appModel ->
        openAppInfo(
            requireContext(),
            appModel.user,
            appModel.activityPackage
        )
        findNavController().popBackStack(R.id.mainFragment, false)
    }

    // Handles click on a contact item
    private fun contactClickListener(
        viewModel: MainViewModel,
        n: Int = 0
    ): (contactItem: ContactListItem) -> Unit = { contactModel ->
        viewModel.selectedContact(this, contactModel, n)
        // Close the drawer or fragment after selection
        findNavController().popBackStack()
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